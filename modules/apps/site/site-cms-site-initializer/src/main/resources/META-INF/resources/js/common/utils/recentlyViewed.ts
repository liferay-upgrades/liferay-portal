/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {localStorage} from 'frontend-js-web';

import ApiHelper from '../services/ApiHelper';
import SpaceService from '../services/SpaceService';
import {ISearchAssetObjectEntry} from '../types/AssetType';
import {Space} from '../types/Space';
import {WORKFLOW_STATUS} from './constants';
import {getScopeExternalReferenceCode} from './getScopeExternalReferenceCode';

const MAX_ENTRIES = 12;

const MAX_ITEMS = 8;

export type RecentlyViewedAsset = {
	asset: ISearchAssetObjectEntry | null;
	href: string;
	id: number;
	space: Space | null;
	viewedAt: number;
};

type RecentlyViewedEntry = {
	href: string;
	id: number;
	viewedAt: number;
};

export function addRecentlyViewedEntry(
	objectEntryId: number | string,
	href: string
) {
	const id = Number(objectEntryId);

	if (!Number.isInteger(id) || id <= 0 || !href) {
		return;
	}

	setRecentlyViewedEntries([
		{href, id, viewedAt: Date.now()},
		...getRecentlyViewedEntries().filter((entry) => entry.id !== id),
	]);
}

export function clearRecentlyViewedEntries() {
	setRecentlyViewedEntries([]);
}

export async function getRecentlyViewedAssets(): Promise<
	RecentlyViewedAsset[]
> {
	const entries = getRecentlyViewedEntries().slice(0, MAX_ITEMS);

	if (!entries.length) {
		return [];
	}

	const searchParams = new URLSearchParams({
		emptySearch: 'true',
		filter: [
			"(cmsSection eq 'contents' or cmsSection eq 'files')",
			`entryClassPK in (${entries.map(({id}) => id).join(', ')})`,
			`status in (${Object.values(WORKFLOW_STATUS).join(', ')})`,
		].join(' and '),
		nestedFields: 'embedded,systemProperties.objectDefinitionBrief',
		pageSize: String(entries.length),
	});

	const {data, error} = await ApiHelper.get<{
		items: ISearchAssetObjectEntry[];
	}>(`/o/search/v1.0/search?${searchParams}`);

	if (error) {
		return [];
	}

	const assets = data?.items ?? [];

	const recentlyViewedAssets = await Promise.all(
		entries.map(({href, id, viewedAt}) => {
			const asset =
				assets.find(({embedded}) => embedded?.id === id) ?? null;

			if (!asset) {
				return {asset, href, id, space: null, viewedAt};
			}

			return getSpace(asset).then((space) => ({
				asset,
				href,
				id,
				space,
				viewedAt,
			}));
		})
	);

	const unavailableIds = recentlyViewedAssets
		.filter(({asset}) => !asset)
		.map(({id}) => id);

	if (unavailableIds.length) {
		setRecentlyViewedEntries(
			getRecentlyViewedEntries().filter(
				({id}) => !unavailableIds.includes(id)
			)
		);
	}

	return recentlyViewedAssets;
}

export function removeRecentlyViewedEntry(id: number) {
	setRecentlyViewedEntries(
		getRecentlyViewedEntries().filter((entry) => entry.id !== id)
	);
}

function getRecentlyViewedEntries(): RecentlyViewedEntry[] {
	try {
		const entries = JSON.parse(
			localStorage.getItem(
				getStorageKey(),
				localStorage.TYPES.FUNCTIONAL
			) || '[]'
		);

		if (!Array.isArray(entries)) {
			return [];
		}

		return entries
			.map((entry) => ({...entry, id: Number(entry?.id)}))
			.filter(isRecentlyViewedEntry);
	}
	catch {
		return [];
	}
}

function getSpace(asset: ISearchAssetObjectEntry): Promise<Space | null> {
	return SpaceService.getSpaceWithCache(
		getScopeExternalReferenceCode(asset),
		asset.embedded.scopeKey
	).catch(() => null);
}

function getStorageKey() {
	return `cms-recently-viewed-${Liferay.ThemeDisplay.getCompanyId()}-${Liferay.ThemeDisplay.getUserId()}`;
}

function isRecentlyViewedEntry(value: unknown): value is RecentlyViewedEntry {
	const {href, id, viewedAt} = (value ?? {}) as RecentlyViewedEntry;

	return (
		typeof href === 'string' &&
		Number.isInteger(id) &&
		id > 0 &&
		typeof viewedAt === 'number'
	);
}

function setRecentlyViewedEntries(entries: RecentlyViewedEntry[]) {
	try {
		localStorage.setItem(
			getStorageKey(),
			JSON.stringify(entries.slice(0, MAX_ENTRIES)),
			localStorage.TYPES.FUNCTIONAL
		);
	}
	catch {}
}
