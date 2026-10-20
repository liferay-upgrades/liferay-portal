/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {sub} from 'frontend-js-web';
import React from 'react';

import {
	NO_VALUE,
	OBJECT_ENTRY_FOLDER_CLASS_NAME,
} from '../../../common/utils/constants';

import '../../../../css/props_transformer/TranslationsRenderer.scss';

export interface TranslationLocale {
	label: string;
	languageId: string;
}

interface TranslationsItemData {
	embedded?: {
		title_i18n?: Record<string, string>;
	};
	entryClassName?: string;
}

export function getTranslatedLocales(
	availableLocales: TranslationLocale[] = [],
	titleI18n: Record<string, string> = {}
): TranslationLocale[] {
	return Object.keys(titleI18n)
		.map((languageId) =>
			availableLocales.find((locale) => locale.languageId === languageId)
		)
		.filter(Boolean) as TranslationLocale[];
}

const TranslationsRenderer = ({
	availableLocales = [],
	itemData,
}: {
	availableLocales?: TranslationLocale[];
	itemData?: TranslationsItemData;
}) => {
	const titleI18n = itemData?.embedded?.title_i18n;

	if (
		itemData?.entryClassName === OBJECT_ENTRY_FOLDER_CLASS_NAME ||
		!availableLocales.length ||
		!titleI18n ||
		!Object.keys(titleI18n).length
	) {
		return <span className="text-secondary">{NO_VALUE}</span>;
	}

	const translatedLocales = getTranslatedLocales(availableLocales, titleI18n);

	const tooltip = translatedLocales.map(({label}) => label).join(', ');

	return (
		<span
			className="cms-translations-cell text-dark"
			data-tooltip-align="top"
			title={tooltip || undefined}
		>
			{sub(
				Liferay.Language.get('x-of-x'),
				translatedLocales.length,
				availableLocales.length
			)}
		</span>
	);
};

export default TranslationsRenderer;