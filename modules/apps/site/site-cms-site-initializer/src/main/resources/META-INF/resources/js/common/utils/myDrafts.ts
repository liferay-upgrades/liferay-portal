/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ApiHelper from '../services/ApiHelper';
import SpaceService from '../services/SpaceService';
import {Space} from '../types/Space';
import {getScopeExternalReferenceCode} from './getScopeExternalReferenceCode';

export type DraftAssets = {
	dateModified: string;
	id: number;
	space: Space | null;
	title: string;
};

type SearchItem = {
	dateModified: string;
	embedded: {
		id: number;
		scopeKey: string;
		systemProperties?: {
			scope?: {
				externalReferenceCode: string;
			};
		};
	};
	entryClassName: string;
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

	return Promise.all(
		data.items.map(async (item) => ({
			dateModified: item.dateModified,
			id: item.embedded.id,
			space: await getSpace(item),
			title: item.title,
		}))
	);
}

function getSpace(item: SearchItem): Promise<Space | null> {
	return SpaceService.getSpaceWithCache(
		getScopeExternalReferenceCode(item),
		item.embedded.scopeKey
	).catch(() => null);
}
