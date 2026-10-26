/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import StructureService from '../../common/services/StructureService';
import {ObjectField} from '../../common/types/ObjectDefinition';

type EditedField = {
	businessType: ObjectField['businessType'];
	label?: string;
	value: string;
};

export type EditedTextField = {
	multiline: boolean;
	value: string;
};

type FieldControl = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

const BLOCK_SELECTOR =
	'blockquote, div, h1, h2, h3, h4, h5, h6, li, p, pre, td, th';

const EDITED_CONTENT_BUSINESS_TYPES = new Set<ObjectField['businessType']>([
	'LongText',
	'MultiselectPicklist',
	'Picklist',
	'RichText',
	'String',
	'Text',
]);

const MAX_EDITED_CONTENT_LENGTH = 20000;

const MULTILINE_BUSINESS_TYPES = new Set<ObjectField['businessType']>([
	'LongText',
	'RichText',
]);

const TEXT_BUSINESS_TYPES = new Set<ObjectField['businessType']>([
	'LongText',
	'RichText',
	'String',
	'Text',
]);

const objectFieldsPromises = new Map<string, Promise<ObjectField[] | null>>();

function convertHTMLToText(html: string): string {
	const element = document.createElement('div');

	element.innerHTML = html;

	element
		.querySelectorAll(BLOCK_SELECTOR)
		.forEach((block) => block.append('\n'));

	return (element.textContent ?? '').replace(/\n\s*\n/g, '\n').trim();
}

export function getContentForm(): Element | null {
	return (
		document.querySelector('.lfr-main-form-container') ||
		document.querySelector('.lfr-layout-structure-item-form')
	);
}

export default async function getEditedContent(
	objectDefinitionExternalReferenceCode: string | undefined
): Promise<string> {
	const fields = await getEditedFields(objectDefinitionExternalReferenceCode);

	return fields
		.map(({label, value}) => (label ? `${label}: ${value}` : value))
		.join('\n\n')
		.slice(0, MAX_EDITED_CONTENT_LENGTH);
}

async function getEditedFields(
	objectDefinitionExternalReferenceCode: string | undefined
): Promise<EditedField[]> {
	const form = getContentForm();

	if (!form) {
		return [];
	}

	const fields: EditedField[] = [];

	const objectFields = objectDefinitionExternalReferenceCode
		? await getObjectFields(objectDefinitionExternalReferenceCode)
		: null;

	const richTextValues = getRichTextValues(form);

	if (objectFields) {
		objectFields.forEach((field) => {
			if (!EDITED_CONTENT_BUSINESS_TYPES.has(field.businessType)) {
				return;
			}

			const value = getFieldValue(form, field, richTextValues);

			if (value) {
				fields.push({
					businessType: field.businessType,
					label: getFieldLabel(field),
					value,
				});
			}
		});
	}
	else {
		const title = getFieldControls(form, 'title')
			.map((control) => control.value.trim())
			.find(Boolean);

		if (title) {
			fields.push({businessType: 'String', value: title});
		}
	}

	richTextValues.filter(Boolean).forEach((value) => {
		fields.push({businessType: 'RichText', value});
	});

	return fields;
}

export async function getEditedTextFields(
	objectDefinitionExternalReferenceCode: string | undefined
): Promise<EditedTextField[]> {
	const fields = await getEditedFields(objectDefinitionExternalReferenceCode);

	return fields
		.filter(({businessType}) => TEXT_BUSINESS_TYPES.has(businessType))
		.map(({businessType, value}) => ({
			multiline: MULTILINE_BUSINESS_TYPES.has(businessType),
			value,
		}));
}

function getFieldControls(form: Element, fieldName: string): FieldControl[] {
	const prefix = `ObjectField_${fieldName}`;

	const controls: FieldControl[] = [];

	form.querySelectorAll<FieldControl>(`[name*="${prefix}"]`).forEach(
		(control) => {
			const index = control.name.indexOf(prefix);

			if (index > 0 && control.name[index - 1] !== '-') {
				return;
			}

			const suffix = control.name.slice(index + prefix.length);

			if (suffix && !/^(_[a-z]{2}_[A-Z]{2})?(-label)?$/.test(suffix)) {
				return;
			}

			controls.push(control);
		}
	);

	return controls;
}

function getFieldLabel(field: ObjectField): string {
	return (
		field.label[
			Liferay.ThemeDisplay.getLanguageId() as Liferay.Language.Locale
		] ||
		Object.values(field.label)[0] ||
		field.name
	);
}

function getFieldValue(
	form: Element,
	field: ObjectField,
	richTextValues: string[]
): string {
	if (field.businessType === 'RichText') {
		return richTextValues.shift() ?? '';
	}

	const controls = getFieldControls(form, field.name);

	if (field.businessType === 'MultiselectPicklist') {
		const labels: string[] = [];

		controls.forEach((control) => {
			if (
				control instanceof HTMLInputElement &&
				control.type === 'checkbox'
			) {
				if (!control.checked || !control.value) {
					return;
				}

				const label = form
					.querySelector(`label[for="${control.id}"]`)
					?.textContent?.trim();

				labels.push(label || control.value);
			}
			else if (control instanceof HTMLSelectElement) {
				Array.from(control.selectedOptions).forEach((option) => {
					labels.push(option.text.trim() || option.value);
				});
			}
		});

		return labels.join(', ');
	}

	if (field.businessType === 'Picklist') {
		const labelControl = controls.find((control) =>
			control.name.endsWith('-label')
		);

		if (labelControl?.value.trim()) {
			return labelControl.value.trim();
		}

		for (const control of controls) {
			if (control instanceof HTMLSelectElement) {
				const option = control.selectedOptions[0];

				if (option?.text.trim()) {
					return option.text.trim();
				}
			}
			else if (control.value.trim()) {
				return control.value.trim();
			}
		}

		return '';
	}

	for (const control of controls) {
		const value = control.value.trim();

		if (value) {
			return value;
		}
	}

	return '';
}

function getObjectFields(
	objectDefinitionExternalReferenceCode: string
): Promise<ObjectField[] | null> {
	let promise = objectFieldsPromises.get(
		objectDefinitionExternalReferenceCode
	);

	if (!promise) {
		promise = StructureService.getStructure(
			objectDefinitionExternalReferenceCode
		)
			.then(({data, error}) => {
				if (error || !data) {
					objectFieldsPromises.delete(
						objectDefinitionExternalReferenceCode
					);

					return null;
				}

				return data.objectFields ?? null;
			})
			.catch(() => {
				objectFieldsPromises.delete(
					objectDefinitionExternalReferenceCode
				);

				return null;
			});

		objectFieldsPromises.set(
			objectDefinitionExternalReferenceCode,
			promise
		);
	}

	return promise;
}

function getRichTextValues(form: Element): string[] {
	return Array.from(
		form.querySelectorAll<HTMLElement>('.ck-editor__editable')
	).map((editable) => {
		const editor = (
			editable as HTMLElement & {
				ckeditorInstance?: {getData: () => string};
			}
		).ckeditorInstance;

		if (editor) {
			return convertHTMLToText(editor.getData());
		}

		return (editable.innerText ?? editable.textContent ?? '').trim();
	});
}
