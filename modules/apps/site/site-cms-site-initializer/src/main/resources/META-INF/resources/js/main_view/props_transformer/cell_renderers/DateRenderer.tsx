/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import React, {useMemo} from 'react';

function getDateOptions(): Intl.DateTimeFormatOptions {
	return {
		day: 'numeric',
		month: 'short',
		timeZone: Liferay.ThemeDisplay.getTimeZone(),
		year: 'numeric',
	};
}

const DateRenderer = ({
	isWarning,
	value,
}: {
	isWarning: (date: Date) => boolean;
	value?: string;
}) => {
	const locale = Liferay.ThemeDisplay.getBCP47LanguageId();

	const formatter = useMemo(
		() => new Intl.DateTimeFormat(locale, getDateOptions()),
		[locale]
	);

	if (!value) {
		return <span className="text-secondary">--</span>;
	}

	const date = new Date(value);

	return (
		<span className={isWarning(date) ? 'text-warning' : 'text-dark'}>
			{formatter.format(date)}
		</span>
	);
};

export default DateRenderer;
