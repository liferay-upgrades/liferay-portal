/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';

// eslint-disable-next-line
import {checkAccessibility} from '@liferay/layout-js-components-web/test/__lib__/index';
import {render, screen} from '@testing-library/react';
import React from 'react';

import {MS_PER_DAY} from '../../../../../src/main/resources/META-INF/resources/js/common/utils/constants';
import ExpirationDateRenderer from '../../../../../src/main/resources/META-INF/resources/js/main_view/props_transformer/cell_renderers/ExpirationDateRenderer';

const NOW = new Date('2026-04-21T10:00:00Z');

const dateFormatter = new Intl.DateTimeFormat('en-US', {
	day: 'numeric',
	month: 'short',
	timeZone: 'UTC',
	year: 'numeric',
});

function getDateFromNow(days: number) {
	return new Date(NOW.getTime() + days * MS_PER_DAY).toISOString();
}

describe('ExpirationDateRenderer', () => {
	beforeAll(() => {
		jest.useFakeTimers();
		jest.setSystemTime(NOW);
	});

	afterAll(() => {
		jest.useRealTimers();
	});

	it('renders a placeholder when there is no expiration date', () => {
		render(<ExpirationDateRenderer />);

		expect(screen.getByText('--')).toHaveClass('text-secondary');
	});

	it('highlights an expiration date within the expiring soon threshold', () => {
		const expirationDate = getDateFromNow(3);

		render(<ExpirationDateRenderer value={expirationDate} />);

		expect(
			screen.getByText(dateFormatter.format(new Date(expirationDate)))
		).toHaveClass('text-warning');
	});

	it('does not highlight an expiration date beyond the expiring soon threshold', () => {
		const expirationDate = getDateFromNow(30);

		render(<ExpirationDateRenderer value={expirationDate} />);

		expect(
			screen.getByText(dateFormatter.format(new Date(expirationDate)))
		).toHaveClass('text-dark');
	});

	it('falls back to the embedded expiration date when there is no value', () => {
		const expirationDate = getDateFromNow(3);

		render(
			<ExpirationDateRenderer itemData={{embedded: {expirationDate}}} />
		);

		expect(
			screen.getByText(dateFormatter.format(new Date(expirationDate)))
		).toBeInTheDocument();
	});

	it('has no accessibility violations', async () => {
		jest.useRealTimers();

		const {container} = render(
			<ExpirationDateRenderer value={getDateFromNow(3)} />
		);

		await checkAccessibility({bestPractices: true, context: container});
	});
});
