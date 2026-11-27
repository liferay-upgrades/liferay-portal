/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import getForm from '../../src/main/resources/META-INF/resources/js/utils/getForm';

const GROUPS = [
	{label: 'Site 1', value: '1'},
	{label: 'Site 2', value: '2'},
];

const SPACES = [
	{label: 'Space 1', value: '10'},
	{label: 'Space 2', value: '20'},
];

describe('getForm', () => {
	it('falls back to the defaults when the stored site and space are gone', () => {
		expect(
			getForm(GROUPS, SPACES, {depotEntryId: '30', sourceGroupId: '3'})
		).toMatchObject({depotEntryId: '10', sourceGroupId: '1'});
	});
});
