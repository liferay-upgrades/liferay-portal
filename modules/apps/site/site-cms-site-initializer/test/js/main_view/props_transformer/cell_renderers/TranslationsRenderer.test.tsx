/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import React from 'react';

import {
	OBJECT_ENTRY_CLASS_NAME,
	OBJECT_ENTRY_FOLDER_CLASS_NAME,
} from '../../../../../src/main/resources/META-INF/resources/js/common/utils/constants';
import TranslationsRenderer from '../../../../../src/main/resources/META-INF/resources/js/main_view/props_transformer/cell_renderers/TranslationsRenderer';

jest.mock('frontend-js-web', () => ({
	sub: jest.fn((key, ...args) => {
		let result = key;
		args.forEach((arg, i) => {
			result = result.replace(`{${i}}`, arg);
		});

		return result;
	}),
}));

(global as any).Liferay = {
	Language: {
		get: jest.fn((key) => (key === 'x-of-x' ? '{0} of {1}' : key)),
	},
};

const SITE_LANGUAGE_IDS = [
	'ar_SA',
	'ca_ES',
	'de_DE',
	'en_US',
	'es_ES',
	'fi_FI',
	'fr_FR',
	'hu_HU',
	'ja_JP',
	'nl_NL',
	'pt_BR',
	'sv_SE',
	'zh_CN',
];

const AVAILABLE_LOCALES = SITE_LANGUAGE_IDS.map((languageId) => ({
	label: languageId.replace('_', '-'),
	languageId,
}));

const PRIVACY_POLICY_LANGUAGE_IDS = ['en_US', 'es_ES', 'fr_FR', 'pt_BR', 'de_DE'];

function getItemData(
	languageIds: string[],
	entryClassName = OBJECT_ENTRY_CLASS_NAME
) {
	return {
		embedded: {
			title_i18n: Object.fromEntries(
				languageIds.map((languageId) => [languageId, languageId])
			),
		},
		entryClassName,
	};
}

function renderTranslations(itemData: ReturnType<typeof getItemData>) {
	render(
		<TranslationsRenderer
			availableLocales={AVAILABLE_LOCALES}
			itemData={itemData}
		/>
	);
}

describe('TranslationsRenderer', () => {
	it.each([
		['Summer Sale', '13 of 13', SITE_LANGUAGE_IDS],
		['Privacy Policy', '5 of 13', PRIVACY_POLICY_LANGUAGE_IDS],
		['Annual Report.pdf', '1 of 13', ['en_US']],
		['an asset without translations', '0 of 13', []],
	])('shows %s as %s', (_title, expected, languageIds) => {
		renderTranslations(getItemData(languageIds));

		expect(screen.getByText(expected)).toHaveClass('text-dark');
	});

	it('lists the translated languages in the site order', () => {
		renderTranslations(getItemData(PRIVACY_POLICY_LANGUAGE_IDS));

		expect(screen.getByText('5 of 13')).toHaveAttribute(
			'title',
			'de-DE, en-US, es-ES, fr-FR, pt-BR'
		);
	});

	it('renders no value for folders', () => {
		renderTranslations(
			getItemData(['en_US'], OBJECT_ENTRY_FOLDER_CLASS_NAME)
		);

		expect(screen.getByText('--')).toHaveClass('text-secondary');
	});

	it('ignores translations into languages that are no longer available', () => {
		renderTranslations(
			getItemData([...PRIVACY_POLICY_LANGUAGE_IDS, 'ko_KR'])
		);

		expect(screen.getByText('5 of 13')).toBeInTheDocument();
	});
});