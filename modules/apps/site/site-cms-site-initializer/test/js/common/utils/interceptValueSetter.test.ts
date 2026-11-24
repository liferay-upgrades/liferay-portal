/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import interceptValueSetter from '../../../../src/main/resources/META-INF/resources/js/common/utils/interceptValueSetter';

describe('interceptValueSetter', () => {
	it('notifies every listener of a programmatic change', () => {
		const input = document.createElement('input');

		const firstListener = jest.fn();
		const secondListener = jest.fn();

		interceptValueSetter(input, firstListener);
		interceptValueSetter(input, secondListener);

		input.value = 'Hello';

		expect(input.value).toBe('Hello');
		expect(firstListener).toHaveBeenCalledTimes(1);
		expect(secondListener).toHaveBeenCalledTimes(1);
	});

	it('stops notifying a listener once it is unregistered', () => {
		const input = document.createElement('input');

		const listener = jest.fn();

		const unregister = interceptValueSetter(input, listener);

		unregister();

		input.value = 'Hello';

		expect(listener).not.toHaveBeenCalled();
	});

	it('does not notify when the value does not change', () => {
		const input = document.createElement('input');

		input.value = 'Hello';

		const listener = jest.fn();

		interceptValueSetter(input, listener);

		input.value = 'Hello';

		expect(listener).not.toHaveBeenCalled();
	});
});
