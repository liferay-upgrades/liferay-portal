/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import React from 'react';

import {
	EXPIRING_SOON_THRESHOLD_DAYS,
	MS_PER_DAY,
} from '../../../../../src/main/resources/META-INF/resources/js/common/utils/constants';
import ExpirationDateRenderer from '../../../../../src/main/resources/META-INF/resources/js/main_view/props_transformer/cell_renderers/ExpirationDateRenderer';

const NOW = new Date('2026-04-21T12:00:00Z');
const getTimeZone = Liferay.ThemeDisplay.getTimeZone;

beforeAll(() => {
	jest.useFakeTimers();
	jest.setSystemTime(NOW);
	Liferay.ThemeDisplay.getTimeZone = () => 'UTC';
});

afterAll(() => {
	jest.useRealTimers();
	Liferay.ThemeDisplay.getTimeZone = getTimeZone;
});

describe('ExpirationDateRenderer. No date provided.', () => {
	it('renders a placeholder', () => {
		for (const props of [{}, {itemData: {}}, {value: ''}]) {
			const {unmount} = render(<ExpirationDateRenderer {...props} />);

			expect(screen.getByText('--')).toHaveClass('text-secondary');

			unmount();
		}
	});
});

describe('ExpirationDateRenderer. Date resolution priority.', () => {
	it('prefers value over itemData.dateExpiration', () => {
		render(
			<ExpirationDateRenderer
				itemData={{dateExpiration: '2026-06-01T00:00:00Z'}}
				value="2026-07-15T00:00:00Z"
			/>
		);

		expect(screen.getByText(/Jul/)).toBeInTheDocument();
	});

	it('falls back to itemData.dateExpiration when value is absent', () => {
		render(
			<ExpirationDateRenderer
				itemData={{dateExpiration: '2026-07-15T00:00:00Z'}}
			/>
		);

		expect(screen.getByText(/Jul/)).toBeInTheDocument();
	});
});

describe('ExpirationDateRenderer. Date formatting.', () => {
	it('formats the date using the short month style', () => {
		render(<ExpirationDateRenderer value="2026-04-23T00:00:00Z" />);

		const span = screen.getByText(/Apr/);

		expect(span.textContent).toMatch(/2026/);
		expect(span.textContent).toMatch(/23/);
	});

	it('formats the date with the portal locale', () => {
		jest.spyOn(
			Liferay.ThemeDisplay,
			'getBCP47LanguageId'
		).mockReturnValueOnce('pt-BR');

		render(<ExpirationDateRenderer value="2026-12-25T00:00:00Z" />);

		expect(screen.getByText(/dez/)).toBeInTheDocument();
	});
});

describe('ExpirationDateRenderer. Expiring-soon styling.', () => {
	it('applies text-warning when the date is within the expiring-soon threshold', () => {
		const soonDate = new Date(NOW.getTime() + 3 * MS_PER_DAY).toISOString();

		render(<ExpirationDateRenderer value={soonDate} />);

		expect(screen.getByText(/.+/)).toHaveClass('text-warning');
	});

	it('applies text-dark when the date is beyond the expiring-soon threshold', () => {
		const farDate = new Date(
			NOW.getTime() + (EXPIRING_SOON_THRESHOLD_DAYS + 1) * MS_PER_DAY
		).toISOString();

		render(<ExpirationDateRenderer value={farDate} />);

		expect(screen.getByText(/.+/)).toHaveClass('text-dark');
	});

	it('applies text-warning when the expiration date is in the past', () => {
		const pastDate = new Date(NOW.getTime() - MS_PER_DAY).toISOString();

		render(<ExpirationDateRenderer value={pastDate} />);

		expect(screen.getByText(/.+/)).toHaveClass('text-warning');
	});

	it('applies text-warning at the exact edge of the threshold', () => {
		const edgeDate = new Date(
			NOW.getTime() + EXPIRING_SOON_THRESHOLD_DAYS * MS_PER_DAY
		).toISOString();

		render(<ExpirationDateRenderer value={edgeDate} />);

		expect(screen.getByText(/.+/)).toHaveClass('text-warning');
	});
});
