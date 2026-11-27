/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayButton, {ClayButtonWithIcon} from '@clayui/button';
import ClayForm, {ClayCheckbox, ClayInput, ClaySelect} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import ClayLayout from '@clayui/layout';
import {ClayTooltipProvider} from '@clayui/tooltip';
import {useSessionState} from 'frontend-js-components-web';
import {COOKIE_TYPES, sessionStorage} from 'frontend-js-web';
import React, {useState} from 'react';

import {
	CREATE_NEW_SPACE,
	FORM_SESSION_KEY,
	MIGRATABLE_CONTENT_TYPES,
} from '../../utils/constants';
import getForm from '../../utils/getForm';

import type {IForm, IOption} from '../../types/Form';

interface IProps {
	groups: IOption[];
	migrationIncomplete: boolean;
	portletNamespace: string;
	spaces: IOption[];
	startCMSMigrationURL: string;
	title: string;
}

export default function Configuration({
	groups,
	migrationIncomplete,
	portletNamespace,
	spaces,
	startCMSMigrationURL,
	title,
}: IProps) {
	const [storedForm, setStoredForm] =
		useSessionState<Partial<IForm>>(FORM_SESSION_KEY);
	const [submitting, setSubmitting] = useState(false);

	const form = getForm(groups, spaces, storedForm);

	const handleSubmit = () => {
		const keptForm = {...storedForm};

		delete keptForm.depotEntryId;
		delete keptForm.spaceName;

		sessionStorage.setItem(
			FORM_SESSION_KEY,
			JSON.stringify(keptForm),
			COOKIE_TYPES.PERSONALIZATION
		);

		setSubmitting(true);
	};

	const updateForm = <K extends keyof IForm>(name: K, value: IForm[K]) =>
		setStoredForm((previousStoredForm) => ({
			...previousStoredForm,
			[name]: value,
		}));

	return (
		<ClayLayout.Sheet>
			<form
				action={startCMSMigrationURL}
				method="post"
				onSubmit={handleSubmit}
			>
				<ClayLayout.SheetHeader>
					<h2 className="sheet-title">{title}</h2>

					<div className="sheet-text">
						{Liferay.Language.get('cms-migration-description')}
					</div>
				</ClayLayout.SheetHeader>

				<ClayLayout.SheetSection>
					<ClayForm.Group>
						<label htmlFor={`${portletNamespace}sourceGroupId`}>
							{Liferay.Language.get('cms-migration-source-site')}

							<ClayIcon
								className="ml-1 reference-mark"
								symbol="asterisk"
							/>

							<span className="hide-accessible sr-only">
								{Liferay.Language.get('required')}
							</span>
						</label>

						<ClaySelect
							id={`${portletNamespace}sourceGroupId`}
							name={`${portletNamespace}sourceGroupId`}
							onChange={(event) =>
								updateForm('sourceGroupId', event.target.value)
							}
							required
							value={form.sourceGroupId}
						>
							{groups.map(({label, value}) => (
								<ClaySelect.Option
									key={value}
									label={label}
									value={value}
								/>
							))}
						</ClaySelect>
					</ClayForm.Group>

					<ClayForm.Group>
						<label htmlFor={`${portletNamespace}depotEntryId`}>
							{Liferay.Language.get('cms-migration-target-space')}
						</label>

						<ClaySelect
							id={`${portletNamespace}depotEntryId`}
							name={`${portletNamespace}depotEntryId`}
							onChange={(event) =>
								updateForm('depotEntryId', event.target.value)
							}
							value={form.depotEntryId}
						>
							<ClaySelect.Option
								label={Liferay.Language.get(
									'cms-migration-create-new-space'
								)}
								value={CREATE_NEW_SPACE}
							/>

							{spaces.map(({label, value}) => (
								<ClaySelect.Option
									key={value}
									label={label}
									value={value}
								/>
							))}
						</ClaySelect>
					</ClayForm.Group>

					{form.depotEntryId === CREATE_NEW_SPACE && (
						<ClayForm.Group>
							<label htmlFor={`${portletNamespace}spaceName`}>
								{Liferay.Language.get(
									'cms-migration-new-space-name'
								)}
							</label>

							<ClayInput
								id={`${portletNamespace}spaceName`}
								name={`${portletNamespace}spaceName`}
								onChange={(event) =>
									updateForm('spaceName', event.target.value)
								}
								type="text"
								value={form.spaceName}
							/>
						</ClayForm.Group>
					)}
				</ClayLayout.SheetSection>

				<ClayTooltipProvider>
					<ClayLayout.SheetSection>
						<h3 className="sheet-subtitle">
							{Liferay.Language.get(
								'cms-migration-content-types'
							)}
						</h3>

						{MIGRATABLE_CONTENT_TYPES.map(({help, label, name}) => (
							<ClayForm.Group
								className="align-items-start d-flex"
								key={name}
							>
								<ClayCheckbox
									checked={form[name]}
									containerProps={{
										className:
											'custom-control-primary mb-0',
									}}
									label={label}
									name={`${portletNamespace}${name}`}
									onChange={(event) =>
										updateForm(name, event.target.checked)
									}
									value="true"
								/>

								{help && (
									<ClayButtonWithIcon
										aria-label={help}
										className="component-action mt-n2"
										data-tooltip-align="top"
										displayType="unstyled"
										size="sm"
										style={
											{
												'--component-action-hover-background-color':
													'transparent',
											} as React.CSSProperties
										}
										symbol="question-circle-full"
										title={help}
									/>
								)}
							</ClayForm.Group>
						))}

						<ClayForm.Group>
							<label
								htmlFor={`${portletNamespace}titleObjectFieldNames`}
							>
								{Liferay.Language.get(
									'cms-migration-title-object-field-names'
								)}
							</label>

							<ClayInput
								id={`${portletNamespace}titleObjectFieldNames`}
								name={`${portletNamespace}titleObjectFieldNames`}
								onChange={(event) =>
									updateForm(
										'titleObjectFieldNames',
										event.target.value
									)
								}
								type="text"
								value={form.titleObjectFieldNames}
							/>

							<ClayForm.FeedbackGroup>
								<ClayForm.Text>
									{Liferay.Language.get(
										'cms-migration-title-object-field-names-help'
									)}
								</ClayForm.Text>
							</ClayForm.FeedbackGroup>
						</ClayForm.Group>
					</ClayLayout.SheetSection>
				</ClayTooltipProvider>

				{migrationIncomplete && (
					<ClayAlert displayType="warning">
						{Liferay.Language.get(
							'cms-migration-only-one-migration-can-run-at-a-time'
						)}
					</ClayAlert>
				)}

				<ClayLayout.SheetFooter>
					<ClayButton
						disabled={migrationIncomplete || submitting}
						type="submit"
					>
						{Liferay.Language.get('cms-migration-start')}
					</ClayButton>
				</ClayLayout.SheetFooter>
			</form>
		</ClayLayout.Sheet>
	);
}
