/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import {Text} from '@clayui/core';
import ClayEmptyState from '@clayui/empty-state';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import ClaySticker from '@clayui/sticker';
import {
	RangeSelector,
	RangeSelectors,
	RangeSelectorsDropdown,
} from '@liferay/analytics-reports-js-components-web';
import classNames from 'classnames';
import {sub} from 'frontend-js-web';
import React, {useContext, useEffect, useState} from 'react';

import {BaseCard} from '../../common/BaseCard';
import GovernanceService, {
	Contributor,
	MILLISECONDS_PER_DAY,
} from '../../governance/GovernanceService';
import {InventoryContext} from '../InventoryContext';

const MAX_CONTRIBUTORS = 5;

function getDateCreatedFilter({rangeEnd, rangeKey, rangeStart}: RangeSelector) {
	if (rangeKey === RangeSelectors.CustomRange) {
		const endDate = new Date(`${rangeEnd}T23:59:59.999`);
		const startDate = new Date(`${rangeStart}T00:00:00.000`);

		return `dateCreated ge ${startDate.toISOString()} and dateCreated le ${endDate.toISOString()}`;
	}

	const days = rangeKey === RangeSelectors.Last24Hours ? 1 : Number(rangeKey);

	const createdSince = new Date(Date.now() - days * MILLISECONDS_PER_DAY);

	return `dateCreated ge ${createdSince.toISOString()}`;
}

function getInitials(name: string) {
	const words = name.trim().split(/\s+/);

	const firstInitial = words[0]?.[0] ?? '';
	const lastInitial = words.length > 1 ? words[words.length - 1][0] : '';

	return (firstInitial + lastInitial).toUpperCase();
}

function ContributorRow({
	contributor,
	isLast,
	maxFrequency,
}: {
	contributor: Contributor;
	isLast: boolean;
	maxFrequency: number;
}) {
	const name = contributor.name ?? Liferay.Language.get('deleted-user');

	return (
		<li
			className={classNames(
				'align-items-center cms-dashboard__top-contributors__item d-flex py-2',
				{'border-bottom': !isLast}
			)}
		>
			<ClaySticker
				className={`mr-3 sticker-user-icon user-icon-color-${contributor.userId % 10}`}
				shape="user-icon"
			>
				{getInitials(name)}
			</ClaySticker>

			<Text truncate weight="semi-bold">
				{name}
			</Text>

			<div aria-hidden="true" className="flex-grow-1 mx-3 progress">
				<div
					className="progress-bar"
					style={{
						width: `${(contributor.frequency / maxFrequency) * 100}%`,
					}}
				/>
			</div>

			<span className="cms-dashboard__top-contributors__count text-right text-secondary">
				{sub(
					contributor.frequency === 1
						? Liferay.Language.get('x-asset')
						: Liferay.Language.get('x-assets'),
					[contributor.frequency]
				)}
			</span>
		</li>
	);
}

function ContributorList({
	contributors,
	error,
}: {
	contributors: Contributor[] | null;
	error: string | null;
}) {
	if (error) {
		return (
			<ClayAlert
				displayType="danger"
				title={Liferay.Language.get('error')}
			>
				{error}
			</ClayAlert>
		);
	}

	if (contributors === null) {
		return (
			<div className="d-flex justify-content-center py-5">
				<ClayLoadingIndicator
					data-testid="loading"
					displayType="primary"
					shape="circle"
					size="md"
				/>
			</div>
		);
	}

	if (!contributors.length) {
		return (
			<ClayEmptyState
				className="cms-dashboard__empty-state"
				description={Liferay.Language.get(
					'no-assets-were-created-in-the-selected-spaces-for-this-period'
				)}
				imgSrc={`${Liferay.ThemeDisplay.getPathThemeImages()}/states/cms_empty_state.svg`}
				imgSrcReducedMotion={`${Liferay.ThemeDisplay.getPathThemeImages()}/states/cms_empty_state.svg`}
				title={Liferay.Language.get('no-contributors-yet')}
			/>
		);
	}

	const maxFrequency = contributors[0].frequency;

	return (
		<ul className="cms-dashboard__top-contributors list-unstyled mb-0">
			{contributors.map((contributor, index) => (
				<ContributorRow
					contributor={contributor}
					isLast={index === contributors.length - 1}
					key={contributor.userId}
					maxFrequency={maxFrequency}
				/>
			))}
		</ul>
	);
}

export function TopContributorsCard() {
	const {
		contentProgressFilter,
		filters: {space},
	} = useContext(InventoryContext);

	const [contributors, setContributors] = useState<Contributor[] | null>(
		null
	);

	const [error, setError] = useState<string | null>(null);

	const [rangeSelector, setRangeSelector] = useState<RangeSelector>({
		rangeEnd: '',
		rangeKey: RangeSelectors.Last30Days,
		rangeStart: '',
	});

	useEffect(() => {
		let stale = false;

		async function fetchContributors() {
			setContributors(null);
			setError(null);

			const {data, error} = await GovernanceService.getContributors(
				`${contentProgressFilter} and ${getDateCreatedFilter(rangeSelector)}`,
				space.siteId,
				MAX_CONTRIBUTORS
			);

			if (stale) {
				return;
			}

			setContributors(data?.contributors ?? null);
			setError(error);
		}

		fetchContributors();

		return () => {
			stale = true;
		};
	}, [contentProgressFilter, rangeSelector, space.siteId]);

	return (
		<BaseCard
			Preferences={
				<RangeSelectorsDropdown
					activeRangeSelector={rangeSelector}
					availableRangeKeys={[
						RangeSelectors.Last24Hours,
						RangeSelectors.Last7Days,
						RangeSelectors.Last28Days,
						RangeSelectors.Last30Days,
						RangeSelectors.Last90Days,
						RangeSelectors.CustomRange,
					]}
					onChange={setRangeSelector}
				/>
			}
			ariaLevel={3}
			description={Liferay.Language.get(
				'these-are-the-people-who-created-the-most-assets-in-the-selected-spaces'
			)}
			role="heading"
			title={Liferay.Language.get('top-contributors')}
			uppercaseTitle={false}
		>
			<ContributorList contributors={contributors} error={error} />
		</BaseCard>
	);
}
