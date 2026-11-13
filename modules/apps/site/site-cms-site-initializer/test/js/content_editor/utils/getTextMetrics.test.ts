/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import getTextMetrics from '../../../../src/main/resources/META-INF/resources/js/content_editor/utils/getTextMetrics';

describe('getTextMetrics', () => {
	it('counts words and characters across all fields', () => {
		expect(
			getTextMetrics([
				{multiline: false, value: 'Autumn Campaign'},
				{multiline: true, value: 'New  discounts\nFree shipping'},
			])
		).toEqual({
			characters: 41,
			charactersWithoutSpaces: 38,
			paragraphs: 2,
			readingTime: 1,
			words: 6,
		});
	});

	it('counts paragraphs only in multiline fields', () => {
		expect(
			getTextMetrics([
				{multiline: false, value: 'Title'},
				{multiline: true, value: 'One\n\n\nTwo\nThree'},
			]).paragraphs
		).toBe(3);
	});

	it('counts an emoji as a single character', () => {
		expect(
			getTextMetrics([{multiline: false, value: 'Hi 👋'}]).characters
		).toBe(4);
	});

	it('rounds the reading time up to the next minute', () => {
		expect(
			getTextMetrics([
				{multiline: true, value: Array(201).fill('word').join(' ')},
			]).readingTime
		).toBe(2);
	});

	it('returns zero for content without text', () => {
		expect(getTextMetrics([{multiline: true, value: ' \n '}])).toEqual({
			characters: 0,
			charactersWithoutSpaces: 0,
			paragraphs: 0,
			readingTime: 0,
			words: 0,
		});
	});
});
