/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ApiHelper from '../../../../src/main/resources/META-INF/resources/js/common/services/ApiHelper';
import ScheduledContentService, {
	getScheduledContentCountURL,
} from '../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/ScheduledContentService';

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/common/services/ApiHelper',
	() => ({__esModule: true, default: {get: jest.fn()}})
);

const API_URL =
	'/o/search/v1.0/search?emptySearch=true&filter=status eq 7&sort=dateDisplay:asc';

describe('[CMS Scheduled Content] ScheduledContentService', () => {
	beforeEach(() => {
		jest.clearAllMocks();
	});

	it('appends the publish date range to the existing filter', () => {
		const url = new URL(
			getScheduledContentCountURL(API_URL, {
				from: new Date('2026-10-07T00:00:00.000Z'),
				to: new Date('2026-10-07T23:59:59.999Z'),
			}),
			'http://localhost'
		);

		expect(url.searchParams.get('filter')).toBe(
			'(status eq 7) and dateDisplay ge 2026-10-07T00:00:00Z and dateDisplay le 2026-10-07T23:59:59Z'
		);
		expect(url.searchParams.get('pageSize')).toBe('1');
		expect(url.searchParams.get('sort')).toBe('dateDisplay:asc');
	});

	it('keeps the filter untouched when there is no range', () => {
		const url = new URL(
			getScheduledContentCountURL(API_URL, {}),
			'http://localhost'
		);

		expect(url.searchParams.get('filter')).toBe('status eq 7');
	});

	it('returns the total count of the search', async () => {
		(ApiHelper.get as jest.Mock).mockResolvedValueOnce({
			data: {totalCount: 4},
			error: null,
		});

		const {count, error} =
			await ScheduledContentService.getScheduledContentCount(API_URL, {
				to: new Date('2026-10-07T20:00:00Z'),
			});

		expect(count).toBe(4);
		expect(error).toBeNull();
	});

	it('returns zero with the error when the request fails', async () => {
		(ApiHelper.get as jest.Mock).mockResolvedValueOnce({
			data: null,
			error: 'boom',
		});

		const {count, error} =
			await ScheduledContentService.getScheduledContentCount(API_URL, {});

		expect(count).toBe(0);
		expect(error).toBe('boom');
	});
});
