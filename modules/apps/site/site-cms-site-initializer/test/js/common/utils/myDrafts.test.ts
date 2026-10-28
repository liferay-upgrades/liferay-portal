/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getDrafts} from '../../../../src/main/resources/META-INF/resources/js/common/utils/myDrafts';
import {mockFetch} from '../../__mocks__/frontend-js-web';

function getRequestedSearchParams() {
	const [url] = mockFetch.mock.calls[0] as unknown as [string];

	return new URL(url, 'http://localhost').searchParams;
}

function mockSearchResponse(items: unknown[]) {
	return {
		json: async () => ({items}),
		ok: true,
		status: 200,
	} as Response;
}

describe('getDrafts', () => {
	afterEach(() => {
		mockFetch.mockReset();
	});

	it('returns the drafts with their title, space and modified date', async () => {
		mockFetch.mockImplementation(((url: string) =>
			Promise.resolve(
				url.includes('/o/search/v1.0/search')
					? mockSearchResponse([
							{
								dateModified: '2026-10-27T13:47:58Z',
								embedded: {
									id: 38677,
									scopeKey: 'marketing-space',
									systemProperties: {
										scope: {
											externalReferenceCode:
												'utils-space',
										},
									},
								},
								entryClassName:
									'com.liferay.object.model.ObjectDefinition#L_CMS_BASIC_WEB_CONTENT',
								title: 'Draft Article',
							},
						])
					: ({
							json: async () => ({id: 1, name: 'Marketing'}),
							ok: true,
							status: 200,
						} as Response)
			)) as any);

		const drafts = await getDrafts(20132, 5);

		expect(drafts).toEqual([
			{
				dateModified: '2026-10-27T13:47:58Z',
				id: 38677,
				space: expect.objectContaining({name: 'Marketing'}),
				title: 'Draft Article',
			},
		]);
	});

	it('requests the latest drafts created by the given user', async () => {
		mockFetch.mockResolvedValueOnce(mockSearchResponse([]));

		await getDrafts(20132, 5);

		const searchParams = getRequestedSearchParams();

		expect(searchParams.get('emptySearch')).toBe('true');
		expect(searchParams.get('filter')).toContain('status eq 2');
		expect(searchParams.get('filter')).toContain('creatorId eq 20132');
		expect(searchParams.get('filter')).toContain(
			'rootDescendantNode eq false'
		);
		expect(searchParams.get('pageSize')).toBe('5');
		expect(searchParams.get('sort')).toBe('dateModified:desc');
	});

	it('returns no drafts when the search fails', async () => {
		mockFetch.mockResolvedValueOnce({
			json: async () => ({title: 'Internal Server Error'}),
			ok: false,
			status: 500,
		} as Response);

		expect(await getDrafts(20132, 5)).toEqual([]);
	});
});
