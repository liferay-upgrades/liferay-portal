/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

// eslint-disable-next-line @liferay/portal/no-cross-module-deep-import
import {checkAccessibility} from '@liferay/layout-js-components-web/test/__lib__/index';
import {render, screen} from '@testing-library/react';
import React from 'react';

import '@testing-library/jest-dom';

import RequiredMark from '../../../src/main/resources/META-INF/resources/js/components/forms/RequiredMark';

describe('RequiredMark', () => {
	it('tells screen readers that the field is required', () => {
		render(<RequiredMark />);

		expect(screen.getByText('required')).toHaveClass('sr-only');
	});

	it('has no accessibility violations', async () => {
		const {container} = render(<RequiredMark />);

		await checkAccessibility({bestPractices: true, context: container});
	});
});
