/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import formatTimeUntil from '../../../../src/main/resources/META-INF/resources/js/common/utils/formatTimeUntil';

describe('[CMS Scheduled Content] formatTimeUntil', () => {
	const now = new Date('2026-10-07T12:00:00Z');

	const until = (date: string) =>
		formatTimeUntil(new Date(date), now, 'en-US');

	it('uses minutes under an hour', () => {
		expect(until('2026-10-07T12:26:51Z')).toBe('in 26 minutes');
	});

	it('uses hours under a day', () => {
		expect(until('2026-10-07T14:30:00Z')).toBe('in 2 hours');
	});

	it('uses days under a week', () => {
		expect(until('2026-10-10T18:00:00Z')).toBe('in 3 days');
	});

	it('uses weeks under a month', () => {
		expect(until('2026-10-22T12:00:00Z')).toBe('in 2 weeks');
	});

	it('uses months under a year', () => {
		expect(until('2027-01-20T12:00:00Z')).toBe('in 3 months');
	});

	it('uses years beyond that', () => {
		expect(until('2028-11-01T12:00:00Z')).toBe('in 2 years');
	});

	it('reports now for dates less than a minute away or already past', () => {
		expect(until('2026-10-07T12:00:30Z')).toBe('now');
		expect(until('2026-10-07T11:00:00Z')).toBe('now');
	});
});
