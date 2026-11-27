/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

// eslint-disable-next-line @liferay/portal/no-cross-module-deep-import
import {checkAccessibility} from '@liferay/layout-js-components-web/test/__lib__/index';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import '@testing-library/jest-dom';

import HelpTooltipIcon from '../../../src/main/resources/META-INF/resources/js/components/forms/HelpTooltipIcon';

const MESSAGE = 'Help message';

describe('HelpTooltipIcon', () => {
	it('exposes the message as the accessible name', () => {
		render(<HelpTooltipIcon message={MESSAGE} />);

		expect(screen.getByRole('img', {name: MESSAGE})).toBeInTheDocument();
	});

	it('shows the message in the portal tooltip', () => {
		render(<HelpTooltipIcon message={MESSAGE} />);

		const icon = screen.getByRole('img', {name: MESSAGE});

		expect(icon).toHaveAttribute('data-title', MESSAGE);
		expect(icon).toHaveClass('lfr-portal-tooltip');
	});

	it('is reachable with the keyboard', async () => {
		render(<HelpTooltipIcon message={MESSAGE} />);

		await userEvent.tab();

		expect(screen.getByRole('img', {name: MESSAGE})).toHaveFocus();
	});

	it('keeps the focus where it was when it is clicked', async () => {
		render(
			<>
				<button>Previous</button>

				<HelpTooltipIcon message={MESSAGE} />
			</>
		);

		const button = screen.getByRole('button', {name: 'Previous'});

		button.focus();

		await userEvent.click(screen.getByRole('img', {name: MESSAGE}));

		expect(button).toHaveFocus();
	});

	it('has no accessibility violations', async () => {
		const {container} = render(<HelpTooltipIcon message={MESSAGE} />);

		await checkAccessibility({bestPractices: true, context: container});
	});
});
