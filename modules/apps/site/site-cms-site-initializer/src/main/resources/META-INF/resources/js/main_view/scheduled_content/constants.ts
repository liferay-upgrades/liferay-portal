/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export const MILLISECONDS_PER_HOUR = 60 * 60 * 1000;

export const SCHEDULED_CONTENT_QUICK_FILTER_TYPES = {
	THIS_MONTH: 'thisMonth',
	THIS_WEEK: 'thisWeek',
	TODAY: 'today',
} as const;

export type ScheduledContentQuickFilterType =
	(typeof SCHEDULED_CONTENT_QUICK_FILTER_TYPES)[keyof typeof SCHEDULED_CONTENT_QUICK_FILTER_TYPES];

export type DateRange = {
	from?: Date;
	to?: Date;
};

function toEndOfDay(date: Date) {
	const endOfDay = new Date(date);

	endOfDay.setHours(23, 59, 59, 999);

	return endOfDay;
}

function toStartOfDay(date: Date) {
	const startOfDay = new Date(date);

	startOfDay.setHours(0, 0, 0, 0);

	return startOfDay;
}

export function getQuickFilterDateRange(
	quickFilterType: ScheduledContentQuickFilterType,
	now = new Date()
): DateRange {
	if (quickFilterType === SCHEDULED_CONTENT_QUICK_FILTER_TYPES.TODAY) {
		return {from: toStartOfDay(now), to: toEndOfDay(now)};
	}

	if (quickFilterType === SCHEDULED_CONTENT_QUICK_FILTER_TYPES.THIS_WEEK) {
		const from = toStartOfDay(now);

		from.setDate(from.getDate() - ((from.getDay() + 6) % 7));

		const to = toEndOfDay(from);

		to.setDate(to.getDate() + 6);

		return {from, to};
	}

	const from = toStartOfDay(now);

	from.setDate(1);

	const to = toEndOfDay(from);

	to.setMonth(to.getMonth() + 1, 0);

	return {from, to};
}
