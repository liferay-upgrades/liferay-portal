/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {EditedTextField} from './getEditedContent';

export type TextMetrics = {
	characters: number;
	charactersWithoutSpaces: number;
	paragraphs: number;
	readingTime: number;
	words: number;
};

const WORDS_PER_MINUTE = 200;

export default function getTextMetrics(fields: EditedTextField[]): TextMetrics {
	const metrics: TextMetrics = {
		characters: 0,
		charactersWithoutSpaces: 0,
		paragraphs: 0,
		readingTime: 0,
		words: 0,
	};

	fields.forEach(({multiline, value}) => {
		const blocks = value
			.split('\n')
			.map((block) => block.replace(/\s+/g, ' ').trim())
			.filter(Boolean);

		blocks.forEach((block) => {
			const characters = [...block];

			metrics.characters += characters.length;
			metrics.charactersWithoutSpaces += characters.filter(
				(character) => character !== ' '
			).length;
			metrics.words += block.split(' ').length;
		});

		if (multiline) {
			metrics.paragraphs += blocks.length;
		}
	});

	metrics.readingTime = Math.ceil(metrics.words / WORDS_PER_MINUTE);

	return metrics;
}
