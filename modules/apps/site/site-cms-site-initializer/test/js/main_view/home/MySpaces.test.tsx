/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';

// eslint-disable-next-line @liferay/portal/no-cross-module-deep-import
import {checkAccessibility} from '@liferay/layout-js-components-web/test/__lib__/index';
import {render, screen} from '@testing-library/react';
import React from 'react';

import {MySpaces} from '../../../../src/main/resources/META-INF/resources/js';
import SpaceService from '../../../../src/main/resources/META-INF/resources/js/common/services/SpaceService';

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/common/services/SpaceService'
);

jest.mock('frontend-js-web', () => ({
	...(jest.requireActual('frontend-js-web') as object),
	sub: (template: string, ...values: string[]) =>
		values.reduce(
			(result, value, index) => result.replace(`{${index}}`, value),
			template
		),
}));

const mockedSpaceService = SpaceService as jest.Mocked<typeof SpaceService>;

const props = {
	allSpacesURL: '/web/cms/all-spaces',
	baseSpaceURL: '/web/cms/e/space/1234/',
};

const languageValues: Record<string, string> = {
	'modified-x': 'Modified {0}',
	'x-asset': '{0} Asset',
	'x-assets': '{0} Assets',
};

describe('MySpaces', () => {
	beforeEach(() => {
		jest.clearAllMocks();

		jest.spyOn(Liferay.Language, 'get').mockImplementation(
			(key: string) => languageValues[key] ?? key
		);
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	it('lists the most recently modified spaces with their assets count', async () => {
		mockedSpaceService.getRecentSpaces.mockResolvedValue({
			data: {
				items: [
					{
						dateModified: '2026-09-30T10:00:00Z',
						id: 1,
						name: 'Marketing',
						settings: {logoColor: 'outline-1'},
					},
					{
						dateModified: '2026-09-29T10:00:00Z',
						id: 2,
						name: 'Legal',
					},
				],
				totalCount: 2,
			},
			error: null,
		} as any);

		mockedSpaceService.getSpaceAssetsCount.mockImplementation(
			async (assetLibraryId) =>
				({
					data: {totalCount: assetLibraryId === 1 ? 128 : 1},
					error: null,
				}) as any
		);

		const {container} = render(<MySpaces {...props} />);

		const marketingLink = await screen.findByRole('link', {
			name: /Marketing/,
		});

		expect(marketingLink).toHaveAttribute(
			'href',
			'/web/cms/e/space/1234/1'
		);
		expect(marketingLink).toHaveTextContent('128 Assets');
		expect(marketingLink).toHaveTextContent('Modified Sep 30, 2026');

		const legalLink = screen.getByRole('link', {name: /Legal/});

		expect(legalLink).toHaveAttribute('href', '/web/cms/e/space/1234/2');
		expect(legalLink).toHaveTextContent('1 Asset');
		expect(legalLink).not.toHaveTextContent('1 Assets');

		await checkAccessibility({bestPractices: true, context: container});
	});

	it('shows the empty state and links View All when the user has no spaces', async () => {
		mockedSpaceService.getRecentSpaces.mockResolvedValue({
			data: {items: [], totalCount: 0},
			error: null,
		} as any);

		render(<MySpaces {...props} />);

		expect(screen.getByRole('heading', {name: 'my-spaces'})).toBeVisible();
		expect(screen.getByRole('link', {name: 'view-all'})).toHaveAttribute(
			'href',
			'/web/cms/all-spaces'
		);

		expect(await screen.findByText('no-spaces-yet')).toBeInTheDocument();
		expect(
			screen.getByText(
				'you-are-not-a-member-of-any-space.-ask-an-administrator-to-add-you-to-one'
			)
		).toBeInTheDocument();
		expect(mockedSpaceService.getSpaceAssetsCount).not.toHaveBeenCalled();
	});

	it('shows the empty state when the spaces cannot be loaded', async () => {
		mockedSpaceService.getRecentSpaces.mockResolvedValue({
			data: null,
			error: 'error',
		} as any);

		render(<MySpaces {...props} />);

		expect(await screen.findByText('no-spaces-yet')).toBeInTheDocument();
	});
});
