/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {render, screen} from '@testing-library/react';
import React from 'react';

import '@testing-library/jest-dom';

import MigrationBar from '../../../src/main/resources/META-INF/resources/js/components/migrations/MigrationBar';
import {IMigrationBar} from '../../../src/main/resources/META-INF/resources/js/types/Migration';

function renderMigrationBars(bars: IMigrationBar[]) {
	const numberFormat = new Intl.NumberFormat('en-US');

	return render(
		<>
			{bars.map((bar) => (
				<MigrationBar
					bar={bar}
					key={bar.key}
					numberFormat={numberFormat}
				/>
			))}
		</>
	);
}

describe('MigrationBar', () => {
	it('shows the progress of the bar as computed by the server', () => {
		renderMigrationBars([
			{
				completed: 10,
				current: true,
				key: 'web-content',
				label: 'Web Content',
				pending: false,
				progress: 99,
				total: 10,
			},
		]);

		expect(screen.getByRole('progressbar')).toHaveAttribute(
			'aria-valuenow',
			'99'
		);
		expect(screen.getByText('Web Content')).not.toHaveClass('text-success');
	});

	it('highlights the current bar, marks the complete bars, and dims the bars that have not started', () => {
		renderMigrationBars([
			{
				completed: 2,
				current: false,
				key: 'documents-and-media',
				label: 'Documents and Media',
				pending: false,
				progress: 100,
				total: 2,
			},
			{
				completed: 4,
				current: true,
				key: 'web-content',
				label: 'Web Content',
				pending: false,
				progress: 40,
				total: 10,
			},
			{
				completed: 0,
				current: false,
				key: 'update-pages',
				label: 'Update Pages',
				pending: false,
				progress: 0,
				total: 5,
			},
		]);

		expect(screen.getByText('Documents and Media')).toHaveClass(
			'text-success'
		);
		expect(screen.getByText('100%')).toBeInTheDocument();
		expect(screen.getByText('Web Content')).toHaveClass('text-info');
		expect(
			screen.getByRole('progressbar', {name: 'Web Content'})
		).toHaveAttribute('aria-valuenow', '40');
		expect(screen.getByText('Update Pages')).toHaveClass('text-secondary');
	});

	it('marks the total still being counted', () => {
		renderMigrationBars([
			{
				completed: 4,
				current: true,
				key: 'web-content',
				label: 'Web Content',
				pending: true,
				progress: 40,
				total: 10,
			},
		]);

		const count = screen.getByText('4 / 10+');

		expect(count).toHaveAttribute('title', 'cms-migration-total-pending');
		expect(count).not.toHaveClass('text-right');
	});
});
