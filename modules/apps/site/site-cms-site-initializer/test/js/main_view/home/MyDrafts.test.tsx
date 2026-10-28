/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {serializeFDSConfig} from '@liferay/frontend-data-set-web';
import {render, screen} from '@testing-library/react';
import React from 'react';

import '@testing-library/jest-dom';

import MyDrafts from '../../../../src/main/resources/META-INF/resources/js/main_view/home/MyDrafts';
import {mockFetch} from '../../__mocks__/frontend-js-web';

jest.mock('@liferay/frontend-data-set-web', () => ({
	getConfigParamName: (fdsName: string) => `${fdsName}_fdsConfig`,
	serializeFDSConfig: jest.fn(() => 'SERIALIZED_CONFIG'),
}));

const DEFAULT_PROPS = {
	allSectionFDSName: 'allSection',
	editContentItemURL: 'http://localhost/c/cms/edit_content_item',
	maxItems: 5,
	redirect: 'http://localhost/web/cms/home',
	userId: 20132,
	userName: 'Test Test',
};

function mockAPI(items: unknown[]) {
	mockFetch.mockImplementation(((url: string) =>
		Promise.resolve({
			json: async () =>
				url.includes('/o/search/v1.0/search')
					? {items}
					: {id: 1, name: 'Marketing'},
			ok: true,
			status: 200,
		} as Response)) as any);
}

describe('MyDrafts', () => {
	afterEach(() => {
		mockFetch.mockReset();
	});

	it('links View All to the drafts of the current user', async () => {
		mockAPI([]);

		render(<MyDrafts {...DEFAULT_PROPS} />);

		await screen.findByText('no-drafts');

		expect(screen.getByRole('link', {name: 'view-all-x'})).toHaveAttribute(
			'href',
			'/web/cms/all?allSection_fdsConfig=SERIALIZED_CONFIG'
		);

		const {filters} = (serializeFDSConfig as jest.Mock).mock.calls[0][0];

		expect(filters).toEqual([
			{
				id: 'creatorId',
				selectedData: {
					exclude: false,
					selectedItems: [{label: 'Test Test', value: 20132}],
				},
			},
			{
				id: 'status',
				selectedData: {exclude: false, selectedItems: [{value: 2}]},
			},
		]);
	});
});
