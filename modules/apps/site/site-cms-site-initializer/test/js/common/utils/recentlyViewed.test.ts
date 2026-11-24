/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	addRecentlyViewedEntry,
	clearRecentlyViewedEntries,
	getRecentlyViewedAssets,
	removeRecentlyViewedEntry,
} from '../../../../src/main/resources/META-INF/resources/js/common/utils/recentlyViewed';
import {mockFetch} from '../../__mocks__/frontend-js-web';

function getAsset(id: number) {
	return {
		embedded: {
			id,
			scopeKey: '20125',
			status: {code: 0, label: 'approved', label_i18n: 'Approved'},
			systemProperties: {
				scope: {externalReferenceCode: 'utils-space'},
			},
			title: `Asset ${id}`,
		},
	};
}

function mockAPI(availableIds: number[]) {
	mockFetch.mockImplementation(((url: string) =>
		Promise.resolve({
			json: async () =>
				url.includes('/o/search/v1.0/search')
					? {items: availableIds.map(getAsset)}
					: {id: 1, name: 'Marketing'},
			ok: true,
			status: 200,
		} as Response)) as any);
}

describe('recentlyViewed', () => {
	beforeEach(() => {
		localStorage.clear();

		mockFetch.mockReset();
	});

	it('returns nothing without querying the server when there is no history', async () => {
		expect(await getRecentlyViewedAssets()).toEqual([]);
		expect(mockFetch).not.toHaveBeenCalled();
	});

	it('returns the most recently viewed asset first, without duplicates', async () => {
		addRecentlyViewedEntry(1, '/asset/1');
		addRecentlyViewedEntry(2, '/asset/2');
		addRecentlyViewedEntry(1, '/asset/1');

		mockAPI([1, 2]);

		const assets = await getRecentlyViewedAssets();

		expect(assets.map(({id}) => id)).toEqual([1, 2]);
		expect(assets[0]).toEqual(
			expect.objectContaining({
				href: '/asset/1',
				space: expect.objectContaining({name: 'Marketing'}),
			})
		);
	});

	it('returns at most 8 assets', async () => {
		for (let id = 1; id <= 12; id++) {
			addRecentlyViewedEntry(id, `/asset/${id}`);
		}

		mockAPI([12, 11, 10, 9, 8, 7, 6, 5]);

		const assets = await getRecentlyViewedAssets();

		expect(assets.map(({id}) => id)).toEqual([12, 11, 10, 9, 8, 7, 6, 5]);
	});

	it('ignores invalid entries', async () => {
		addRecentlyViewedEntry('abc', '/asset/abc');
		addRecentlyViewedEntry(0, '/asset/0');
		addRecentlyViewedEntry(3, '');

		expect(await getRecentlyViewedAssets()).toEqual([]);
	});

	it('removes an asset from the history', async () => {
		addRecentlyViewedEntry(1, '/asset/1');
		addRecentlyViewedEntry(2, '/asset/2');

		removeRecentlyViewedEntry(2);

		mockAPI([1]);

		const assets = await getRecentlyViewedAssets();

		expect(assets.map(({id}) => id)).toEqual([1]);
	});

	it('clears the history', async () => {
		addRecentlyViewedEntry(1, '/asset/1');

		clearRecentlyViewedEntries();

		expect(await getRecentlyViewedAssets()).toEqual([]);
	});

	it('returns an unavailable asset once and drops it from the history', async () => {
		addRecentlyViewedEntry(1, '/asset/1');
		addRecentlyViewedEntry(2, '/asset/2');

		mockAPI([1]);

		const assets = await getRecentlyViewedAssets();

		expect(assets).toEqual([
			expect.objectContaining({asset: null, id: 2}),
			expect.objectContaining({id: 1}),
		]);

		const nextAssets = await getRecentlyViewedAssets();

		expect(nextAssets.map(({id}) => id)).toEqual([1]);
	});
});
