/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ApiHelper from '../../common/services/ApiHelper';
import {DateRange} from './constants';

type SearchPage = {
	totalCount: number;
};

function toODataDate(date: Date) {
	return date.toISOString().replace(/\.\d{3}Z$/, 'Z');
}

export function getScheduledContentCountURL(
	apiURL: string,
	{from, to}: DateRange
) {
	const url = new URL(apiURL, window.location.origin);

	const conditions = [];

	if (from) {
		conditions.push(`dateDisplay ge ${toODataDate(from)}`);
	}

	if (to) {
		conditions.push(`dateDisplay le ${toODataDate(to)}`);
	}

	const filter = url.searchParams.get('filter');

	if (filter && conditions.length) {
		url.searchParams.set(
			'filter',
			`(${filter}) and ${conditions.join(' and ')}`
		);
	}
	else if (conditions.length) {
		url.searchParams.set('filter', conditions.join(' and '));
	}

	url.searchParams.set('page', '1');
	url.searchParams.set('pageSize', '1');

	return `${url.pathname}${url.search}`;
}

async function getScheduledContentCount(
	apiURL: string,
	dateRange: DateRange,
	signal?: AbortSignal
) {
	const {data, error} = await ApiHelper.get<SearchPage>(
		getScheduledContentCountURL(apiURL, dateRange),
		signal
	);

	return {count: data?.totalCount ?? 0, error};
}

export default {
	getScheduledContentCount,
};
