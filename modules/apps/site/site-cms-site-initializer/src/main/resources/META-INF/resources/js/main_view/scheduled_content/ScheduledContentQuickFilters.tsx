/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLayout from '@clayui/layout';
import {IBaseFilterState, IFDSState} from '@liferay/frontend-data-set-web';
import {useLiferayState} from '@liferay/frontend-js-state-web/react';
import React, {useCallback, useEffect, useState} from 'react';

import {
	CMSSiteInitializerFDSNames,
	FDS_EVENT_DISPLAY_UPDATED,
	FDS_FILTER_ID,
} from '../../common/utils/constants';
import toDatePart from '../../common/utils/toDatePart';
import ScheduledContentQuickFilterButton from './ScheduledContentQuickFilterButton';
import ScheduledContentService from './ScheduledContentService';
import {scheduledContentFDSAtom} from './atoms';
import {
	SCHEDULED_CONTENT_QUICK_FILTER_TYPES,
	ScheduledContentQuickFilterType,
	getQuickFilterDateRange,
} from './constants';

type DatePart = ReturnType<typeof toDatePart>;

type DateRangeSelectedData = {
	exclude?: boolean;
	from?: DatePart | null;
	to?: DatePart | null;
};

type QuickFilterCounts = Record<ScheduledContentQuickFilterType, number>;

const QUICK_FILTERS: Array<{
	icon: string;
	label: string;
	type: ScheduledContentQuickFilterType;
}> = [
	{
		icon: 'calendar',
		label: Liferay.Language.get('today'),
		type: SCHEDULED_CONTENT_QUICK_FILTER_TYPES.TODAY,
	},
	{
		icon: 'date-time',
		label: Liferay.Language.get('this-week'),
		type: SCHEDULED_CONTENT_QUICK_FILTER_TYPES.THIS_WEEK,
	},
	{
		icon: 'time',
		label: Liferay.Language.get('this-month'),
		type: SCHEDULED_CONTENT_QUICK_FILTER_TYPES.THIS_MONTH,
	},
];

function isSameDatePart(actual?: DatePart | null, expected?: DatePart | null) {
	if (!actual || !expected) {
		return !actual && !expected;
	}

	return (
		actual.day === expected.day &&
		actual.month === expected.month &&
		actual.year === expected.year
	);
}

function getSelectedData(
	quickFilterType: ScheduledContentQuickFilterType
): DateRangeSelectedData {
	const {from, to} = getQuickFilterDateRange(quickFilterType);

	return {
		exclude: false,
		from: from ? toDatePart(from) : null,
		to: to ? toDatePart(to) : null,
	};
}

function getActiveQuickFilter(
	filters: readonly IBaseFilterState[] = []
): ScheduledContentQuickFilterType | null {
	const displayDateFilter = filters.find(
		({active, id}) => active && id === FDS_FILTER_ID.DATE_DISPLAY
	);

	if (!displayDateFilter) {
		return null;
	}

	const actual = displayDateFilter.selectedData as DateRangeSelectedData;

	for (const {type} of QUICK_FILTERS) {
		const expected = getSelectedData(type);

		if (
			isSameDatePart(actual?.from, expected.from) &&
			isSameDatePart(actual?.to, expected.to)
		) {
			return type;
		}
	}

	return null;
}

export default function ScheduledContentQuickFilters({
	apiURL,
}: {
	apiURL: string;
}) {
	const [fdsState, setFDSState] = useLiferayState<IFDSState>(
		scheduledContentFDSAtom
	);

	const activeQuickFilter = getActiveQuickFilter(fdsState.filters);

	const [counts, setCounts] = useState<QuickFilterCounts>({
		thisMonth: 0,
		thisWeek: 0,
		today: 0,
	});

	const fetchCounts = useCallback(
		async (signal?: AbortSignal) => {
			const results = await Promise.all(
				QUICK_FILTERS.map(({type}) =>
					ScheduledContentService.getScheduledContentCount(
						apiURL,
						getQuickFilterDateRange(type),
						signal
					)
				)
			);

			if (signal?.aborted) {
				return;
			}

			setCounts(
				QUICK_FILTERS.reduce(
					(previousCounts, {type}, index) => ({
						...previousCounts,
						[type]: results[index].count,
					}),
					{} as QuickFilterCounts
				)
			);
		},
		[apiURL]
	);

	useEffect(() => {
		const controller = new AbortController();

		fetchCounts(controller.signal);

		const handleDisplayUpdated = (event?: {id?: string}) => {
			if (
				event?.id?.endsWith(
					CMSSiteInitializerFDSNames.SCHEDULED_CONTENT_SECTION
				)
			) {
				fetchCounts(controller.signal);
			}
		};

		Liferay.on(FDS_EVENT_DISPLAY_UPDATED, handleDisplayUpdated);

		return () => {
			controller.abort();

			Liferay.detach(FDS_EVENT_DISPLAY_UPDATED, handleDisplayUpdated);
		};
	}, [fetchCounts]);

	const toggleQuickFilter = useCallback(
		(quickFilterType: ScheduledContentQuickFilterType) => {
			const active = activeQuickFilter === quickFilterType;

			setFDSState({
				...fdsState,
				filters: fdsState.filters.map((filter: IBaseFilterState) => {
					if (filter.id !== FDS_FILTER_ID.DATE_DISPLAY) {
						return filter;
					}

					if (active) {
						return {
							...filter,
							active: false,
							odataFilterString: undefined,
							selectedData: undefined,
						};
					}

					return {
						...filter,
						active: true,
						selectedData: getSelectedData(quickFilterType),
					};
				}),
			});
		},
		[activeQuickFilter, fdsState, setFDSState]
	);

	return (
		<div
			aria-label={Liferay.Language.get('publish-date')}
			className="cms-scheduled-content-quick-filters"
			role="group"
		>
			<ClayLayout.ContainerFluid
				className="c-pb-4 c-pt-2 c-px-4"
				size={false}
			>
				<ClayLayout.Row>
					{QUICK_FILTERS.map(({icon, label, type}) => (
						<ClayLayout.Col className="c-px-2" key={type} size={4}>
							<ScheduledContentQuickFilterButton
								active={activeQuickFilter === type}
								count={counts[type]}
								icon={icon}
								label={label}
								onClick={() => toggleQuickFilter(type)}
							/>
						</ClayLayout.Col>
					))}
				</ClayLayout.Row>
			</ClayLayout.ContainerFluid>
		</div>
	);
}
