/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {Text} from '@clayui/core';
import ClayEmptyState from '@clayui/empty-state';
import ClayIcon from '@clayui/icon';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {sub} from 'frontend-js-web';
import React, {useContext, useEffect, useMemo, useState} from 'react';

import SpaceSticker from '../../../../common/components/SpaceSticker';
import SpaceService from '../../../../common/services/SpaceService';
import {BaseCard} from '../../common/BaseCard';
import {SpaceOption} from '../../common/SpacePicker';
import GovernanceService, {
	DEFAULT_CMS_FILTER,
	FacetBucket,
} from '../../governance/GovernanceService';
import {InventoryContext} from '../InventoryContext';

export type SpaceLibrary = {
	externalReferenceCode?: string;
	groupId?: number;
	id?: number;
	name: string;
	siteId?: number;
};

export type SpaceRow = {
	count: number;
	groupId: string;
	name: string;
	relativeWidth: number;
	share: number;
};

export function getRows(
	buckets: FacetBucket[],
	spaces: SpaceLibrary[] = []
): SpaceRow[] {
	const spaceNames = new Map(
		spaces.map(({groupId, name, siteId}) => [
			String(siteId ?? groupId),
			name,
		])
	);

	const items: {count: number; groupId: string; name: string}[] = [];

	for (const {frequency, term} of buckets) {
		if (frequency <= 0) {
			continue;
		}

		const name = spaceNames.get(String(term));

		if (!name) {
			continue;
		}

		items.push({
			count: frequency,
			groupId: String(term),
			name,
		});
	}

	items.sort((a, b) => b.count - a.count);

	const totalCount = items.reduce((sum, {count}) => sum + count, 0);

	return items.map(({count, groupId, name}) => ({
		count,
		groupId,
		name,
		relativeWidth: totalCount ? (count / totalCount) * 100 : 0,
		share: totalCount ? count / totalCount : 0,
	}));
}

function getSelectedGroupId(
	space: SpaceOption,
	spaces: SpaceLibrary[] = []
): number | undefined {
	if (!space || space.value === 'all') {
		return undefined;
	}

	if (space.siteId) {
		return space.siteId;
	}

	const matchedSpace = spaces.find(
		({externalReferenceCode, groupId, id, name, siteId}) =>
			(space.externalReferenceCode &&
				externalReferenceCode === space.externalReferenceCode) ||
			String(id) === space.value ||
			String(siteId ?? groupId) === space.value ||
			name === space.label
	);

	return (
		matchedSpace?.siteId ??
		matchedSpace?.groupId ??
		(Number(space.value) || undefined)
	);
}

function EmptyState() {
	const {
		changeLanguage,
		changeSpace,
		filters: {language, space},
	} = useContext(InventoryContext);

	const hasFilters: boolean =
		(language && language.value !== 'all') ||
		(space && space.value !== 'all');

	if (hasFilters) {
		return (
			<ClayEmptyState
				description={Liferay.Language.get(
					'sorry,-no-results-were-found'
				)}
				image={
					<ClayIcon
						className="c-empty-state-icon c-empty-state-icon-search-state"
						illustration
						symbol="search-state"
					/>
				}
				title={Liferay.Language.get('no-results-found')}
			>
				<ClayButton
					displayType="secondary"
					onClick={() => {
						changeLanguage({
							label: Liferay.Language.get('all-languages'),
							value: 'all',
						});
						changeSpace({
							label: Liferay.Language.get('all-spaces'),
							value: 'all',
						});
					}}
				>
					{Liferay.Language.get('clear-filters')}
				</ClayButton>
			</ClayEmptyState>
		);
	}

	return (
		<ClayEmptyState
			className="cms-dashboard__empty-state"
			description={Liferay.Language.get(
				'the-spaces-you-can-access-do-not-have-any-assets'
			)}
			imgSrc={`${Liferay.ThemeDisplay.getPathThemeImages()}/states/cms_empty_state.svg`}
			imgSrcReducedMotion={`${Liferay.ThemeDisplay.getPathThemeImages()}/states/cms_empty_state.svg`}
			title={Liferay.Language.get('no-assets-yet')}
		/>
	);
}

export function AssetsBySpaceCard() {
	const {
		filters: {language, space},
	} = useContext(InventoryContext);

	const [buckets, setBuckets] = useState<FacetBucket[]>([]);
	const [loading, setLoading] = useState(true);
	const [spaces, setSpaces] = useState<SpaceLibrary[]>([]);

	useEffect(() => {
		const controller = new AbortController();

		async function fetchData() {
			setLoading(true);

			const fetchedSpaces = (await SpaceService.getSpaces()) ?? [];

			if (controller.signal.aborted) {
				return;
			}

			const selectedGroupId = getSelectedGroupId(space, fetchedSpaces);

			const {data, error} = await GovernanceService.getAssetsBySpace(
				DEFAULT_CMS_FILTER,
				selectedGroupId,
				controller.signal
			);

			if (controller.signal.aborted) {
				return;
			}

			if (error) {
				console.error(error);
			}

			setSpaces(fetchedSpaces);
			setBuckets(data ?? []);
			setLoading(false);
		}

		fetchData();

		return () => {
			controller.abort();
		};
	}, [language, space]);

	const rows = useMemo(() => getRows(buckets, spaces), [buckets, spaces]);

	const totalCount = useMemo(
		() => rows.reduce((sum, {count}) => sum + count, 0),
		[rows]
	);

	const languageId = Liferay.ThemeDisplay.getBCP47LanguageId();

	const numberFormat = new Intl.NumberFormat(languageId);
	const percentFormat = new Intl.NumberFormat(languageId, {
		maximumFractionDigits: 0,
		style: 'percent',
	});

	return (
		<BaseCard
			ariaLevel={3}
			className="cms-dashboard__assets-by-space"
			description={Liferay.Language.get(
				'this-is-how-the-assets-are-distributed-across-the-spaces'
			)}
			role="heading"
			title={Liferay.Language.get('assets-by-space')}
			uppercaseTitle={false}
		>
			{loading ? (
				<div className="align-items-center cms-dashboard__assets-by-space__loading d-flex justify-content-center">
					<ClayLoadingIndicator
						data-testid="loading"
						displayType="primary"
						shape="squares"
						size="md"
					/>
				</div>
			) : !rows.length ? (
				<EmptyState />
			) : (
				<>
					<div className="align-items-baseline d-flex mb-2">
						<span className="font-weight-semi-bold mr-2 text-8 text-dark">
							{numberFormat.format(totalCount)}
						</span>

						<Text color="secondary" size={3}>
							{sub(
								rows.length === 1
									? Liferay.Language.get(
											'assets-in-x-space'
										).toLowerCase()
									: Liferay.Language.get(
											'assets-in-x-spaces'
										).toLowerCase(),
								rows.length
							)}
						</Text>
					</div>

					<ul className="cms-dashboard__assets-by-space__list list-unstyled mb-0">
						{rows.map(
							({count, groupId, name, relativeWidth, share}) => (
								<li
									className="align-items-center cms-dashboard__assets-by-space__row d-flex py-2"
									key={groupId}
								>
									<div className="cms-dashboard__assets-by-space__name font-weight-semi-bold overflow-hidden text-3 text-dark">
										<SpaceSticker
											displayType="outline-9"
											name={name}
											size="sm"
										/>
									</div>

									<div
										aria-hidden="true"
										className="cms-dashboard__assets-by-space__bar flex-grow-1 mx-3"
									>
										<div
											className="bg-primary cms-dashboard__assets-by-space__bar-fill h-100"
											style={{
												width: `${relativeWidth}%`,
											}}
										/>
									</div>

									<span className="cms-dashboard__assets-by-space__count cms-dashboard__assets-by-space__value text-3 text-dark text-right">
										{numberFormat.format(count)}
									</span>

									<span className="cms-dashboard__assets-by-space__share cms-dashboard__assets-by-space__value ml-2 text-3 text-right text-secondary">
										{percentFormat.format(share)}
									</span>
								</li>
							)
						)}
					</ul>
				</>
			)}
		</BaseCard>
	);
}
