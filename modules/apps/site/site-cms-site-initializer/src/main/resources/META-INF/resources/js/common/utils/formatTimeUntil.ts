/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
const SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;
const SECONDS_PER_WEEK = 7 * SECONDS_PER_DAY;
const SECONDS_PER_MONTH = 30 * SECONDS_PER_DAY;
const SECONDS_PER_YEAR = 365 * SECONDS_PER_DAY;

const UNITS: Array<{seconds: number; unit: Intl.RelativeTimeFormatUnit}> = [
	{seconds: SECONDS_PER_YEAR, unit: 'year'},
	{seconds: SECONDS_PER_MONTH, unit: 'month'},
	{seconds: SECONDS_PER_WEEK, unit: 'week'},
	{seconds: SECONDS_PER_DAY, unit: 'day'},
	{seconds: SECONDS_PER_HOUR, unit: 'hour'},
	{seconds: SECONDS_PER_MINUTE, unit: 'minute'},
];

/**
 * Formats the time left until a future date in the largest unit that fits,
 * such as "in 3 days" or "in 2 hours". A date in the past or less than a
 * minute away is reported as "now".
 */
export default function formatTimeUntil(
	date: Date,
	now = new Date(),
	locale = Liferay.ThemeDisplay.getBCP47LanguageId()
) {
	const diffInSeconds = Math.floor((date.getTime() - now.getTime()) / 1000);

	const relative = new Intl.RelativeTimeFormat(locale, {numeric: 'auto'});

	for (const {seconds, unit} of UNITS) {
		if (diffInSeconds >= seconds) {
			return relative.format(Math.floor(diffInSeconds / seconds), unit);
		}
	}

	return relative.format(0, 'second');
}
