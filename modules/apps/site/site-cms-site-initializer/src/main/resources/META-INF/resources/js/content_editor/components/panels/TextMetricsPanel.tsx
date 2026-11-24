/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import React from 'react';

import useTextMetrics from '../useTextMetrics';

type Props = {
	objectDefinitionExternalReferenceCode?: string;
};

export default function TextMetricsPanel({
	objectDefinitionExternalReferenceCode,
}: Props) {
	const formatOptions: Intl.NumberFormatOptions = {
		style: 'unit',
		unit: 'minute',
		unitDisplay: 'short',
	};

	const languageTag = Liferay.ThemeDisplay.getBCP47LanguageId();

	const textMetrics = useTextMetrics(objectDefinitionExternalReferenceCode);

	if (!textMetrics) {
		return null;
	}

	if (!textMetrics.words) {
		return (
			<p className="p-3 text-3 text-center text-secondary">
				{Liferay.Language.get('start-writing-to-see-the-text-metrics')}
			</p>
		);
	}

	return (
		<div className="p-3">
			<dl className="mb-0">
				<Row
					label={Liferay.Language.get('words')}
					value={textMetrics.words}
				/>

				<Row
					label={Liferay.Language.get('characters')}
					value={textMetrics.characters}
				/>

				<Row
					label={Liferay.Language.get('characters-without-spaces')}
					value={textMetrics.charactersWithoutSpaces}
				/>

				<Row
					label={Liferay.Language.get('paragraphs')}
					value={textMetrics.paragraphs}
				/>

				<Row
					label={Liferay.Language.get('reading-time')}
					value={new Intl.NumberFormat(
						languageTag,
						formatOptions
					).format(textMetrics.readingTime)}
				/>
			</dl>

			<p className="mt-3 text-2 text-secondary">
				{Liferay.Language.get(
					'counts-text-fields-including-rich-text-as-you-type'
				)}
			</p>
		</div>
	);
}

function Row({label, value}: {label: string; value: React.ReactNode}) {
	return (
		<div className="border-bottom d-flex justify-content-between mb-2 pb-2 text-3">
			<dt className="font-weight-normal text-secondary">{label}</dt>

			<dd className="font-weight-semi-bold mb-0 text-dark">{value}</dd>
		</div>
	);
}
