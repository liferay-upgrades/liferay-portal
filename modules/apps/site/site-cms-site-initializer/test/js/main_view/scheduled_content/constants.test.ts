/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	SCHEDULED_CONTENT_QUICK_FILTER_TYPES,
	getQuickFilterDateRange,
} from '../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/constants';

describe('[CMS Scheduled Content] getQuickFilterDateRange', () => {
	const now = new Date(2026, 9, 7, 15, 30);

	it('covers the whole current day', () => {
		const {from, to} = getQuickFilterDateRange(
			SCHEDULED_CONTENT_QUICK_FILTER_TYPES.TODAY,
			now
		);

		expect(from).toEqual(new Date(2026, 9, 7, 0, 0, 0, 0));
		expect(to).toEqual(new Date(2026, 9, 7, 23, 59, 59, 999));
	});

	it('covers the current week from Monday to Sunday', () => {
		const {from, to} = getQuickFilterDateRange(
			SCHEDULED_CONTENT_QUICK_FILTER_TYPES.THIS_WEEK,
			now
		);

		expect(from).toEqual(new Date(2026, 9, 5, 0, 0, 0, 0));
		expect(to).toEqual(new Date(2026, 9, 11, 23, 59, 59, 999));
	});

	it('covers the current month from the first to the last day', () => {
		const {from, to} = getQuickFilterDateRange(
			SCHEDULED_CONTENT_QUICK_FILTER_TYPES.THIS_MONTH,
			now
		);

		expect(from).toEqual(new Date(2026, 9, 1, 0, 0, 0, 0));
		expect(to).toEqual(new Date(2026, 9, 31, 23, 59, 59, 999));
	});
});
