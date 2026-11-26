/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

// eslint-disable-next-line @liferay/portal/no-cross-module-deep-import
import {checkAccessibility} from '@liferay/layout-js-components-web/test/__lib__/index';
import {render, screen} from '@testing-library/react';
import React from 'react';

import '@testing-library/jest-dom';

import FieldWrapper from '../../../src/main/resources/META-INF/resources/js/components/forms/FieldWrapper';

const FIELD_ID = 'fieldId';

const LABEL = 'Field Label';

function renderFieldWrapper({required = false} = {}) {
	return render(
		<FieldWrapper fieldId={FIELD_ID} label={LABEL} required={required}>
			<input id={FIELD_ID} type="text" />
		</FieldWrapper>
	);
}

describe('FieldWrapper', () => {
	it('associates the label with the field', () => {
		renderFieldWrapper();

		expect(screen.getByLabelText(LABEL)).toHaveAttribute('id', FIELD_ID);
	});

	it('marks the field as required', () => {
		renderFieldWrapper({required: true});

		expect(screen.getByText('required')).toBeInTheDocument();
	});

	it('does not mark an optional field as required', () => {
		renderFieldWrapper();

		expect(screen.queryByText('required')).not.toBeInTheDocument();
	});

	it('has no accessibility violations', async () => {
		const {container} = renderFieldWrapper({required: true});

		await checkAccessibility({bestPractices: true, context: container});
	});
});
