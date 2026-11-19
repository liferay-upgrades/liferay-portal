/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const FORMULA_PREFIXES = ['=', '+', '-', '@'];

export function downloadCSV({
	errors,
	name,
	warnings,
}: {
	errors: string[];
	name: string;
	warnings: string[];
}) {
	const messagesCSV = getCSV([
		[Liferay.Language.get('type'), Liferay.Language.get('message')],
		...errors.map((error) => [Liferay.Language.get('error'), error]),
		...warnings.map((warning) => [
			Liferay.Language.get('warning'),
			warning,
		]),
	]);

	const blobURL = URL.createObjectURL(
		new Blob([messagesCSV], {type: 'text/csv;charset=utf-8'})
	);

	const link = document.createElement('a');

	link.download = getCSVFileName(name);
	link.href = blobURL;

	link.click();

	URL.revokeObjectURL(blobURL);
}

function escapeValue(value: string) {
	if (FORMULA_PREFIXES.includes(value.charAt(0))) {
		value = `'${value}`;
	}

	if (/[",\r\n]/.test(value)) {
		return `"${value.replace(/"/g, '""')}"`;
	}

	return value;
}

function getCSV(rows: string[][]) {
	return (
		'﻿' +
		rows.map((row) => row.map(escapeValue).join(',')).join('\r\n') +
		'\r\n'
	);
}

function getCSVFileName(name: string) {
	const slug = name
		.normalize('NFD')
		.replace(/[̀-ͯ]/g, '')
		.toLowerCase()
		.replace(/[^a-z0-9]+/g, '-')
		.replace(/^-|-$/g, '');

	const date = new Date();

	return `cms-migration-${slug}-${date.getFullYear()}-${padNumber(
		date.getMonth() + 1
	)}-${padNumber(date.getDate())}-${padNumber(date.getHours())}${padNumber(
		date.getMinutes()
	)}.csv`;
}

function padNumber(number: number) {
	return String(number).padStart(2, '0');
}
