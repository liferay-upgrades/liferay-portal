/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ApiHelper from '../services/ApiHelper';

export type DraftAssets = {
	dateModified: string;
	id: number;
	scopeKey: string;
	title: string;
};

type SearchItem = {
	dateModified: string;
	embedded: {
		id: number;
		scopeKey: string;
	};
	title: string;
};

export async function getDrafts(
	userId: number,
	maxItems: number
): Promise<DraftAssets[]> {
	const params = new URLSearchParams({
		emptySearch: 'true',
		filter: `(cmsSection eq 'contents' or cmsSection eq 'files') and status eq 2 and creatorId eq ${userId} and rootDescendantNode eq false`,
		nestedFields: 'embedded',
		pageSize: String(maxItems),
		sort: 'dateModified:desc',
	});

	const {data, error} = await ApiHelper.get<{
		items: SearchItem[];
	}>(`/o/search/v1.0/search?${params}`);

	if (error !== null) {
		return [];
	}

	return data.items.map(({dateModified, embedded, title}) => ({
		dateModified,
		id: embedded.id,
		scopeKey: embedded.scopeKey,
		title,
	}));
}
