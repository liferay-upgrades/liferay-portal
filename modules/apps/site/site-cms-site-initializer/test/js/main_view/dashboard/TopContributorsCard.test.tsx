/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';

// eslint-disable-next-line @liferay/portal/no-cross-module-deep-import
import {checkAccessibility} from '@liferay/layout-js-components-web/test/__lib__/index';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import {InventoryContextProvider} from '../../../../src/main/resources/META-INF/resources/js/main_view/dashboard/inventory/InventoryContext';
import {TopContributorsCard} from '../../../../src/main/resources/META-INF/resources/js/main_view/dashboard/inventory/components/TopContributorsCard';
import {mockFetch} from '../../__mocks__/frontend-js-web';

const NOW = new Date('2026-10-14T12:00:00.000Z').getTime();

const USER_NAMES: {[userId: string]: string} = {
	1: 'Ana Silva',
	2: 'John Doe',
};

function mockResponse(body: object, status = 200) {
	return {
		json: async () => body,
		ok: status < 400,
		status,
	} as Response;
}

function mockContributorsRequests({
	buckets = [],
	searchError,
}: {
	buckets?: {frequency: number; term: string}[];
	searchError?: string;
}) {
	(mockFetch as jest.Mock).mockImplementation(async (url: string) => {
		if (url.startsWith('/o/search/v1.0/search')) {
			if (searchError) {
				return mockResponse({title: searchError}, 500);
			}

			return mockResponse({
				searchFacets: {contributors: buckets},
				totalCount: buckets.length,
			});
		}

		const name = USER_NAMES[url.split('/').pop() as string];

		if (!name) {
			return mockResponse({status: 'NOT_FOUND'}, 404);
		}

		return mockResponse({name});
	});
}

function getSearchURLs() {
	return (mockFetch as jest.Mock).mock.calls
		.map(([url]) => decodeURIComponent(url).replace(/\+/g, ' '))
		.filter((url) => url.startsWith('/o/search/v1.0/search'));
}

function renderTopContributorsCard(space: object = {value: 'all'}) {
	return render(
		<InventoryContextProvider
			value={{
				contentProgressFilter: 'someFilter',
				filters: {space} as any,
			}}
		>
			<TopContributorsCard />
		</InventoryContextProvider>
	);
}

describe('TopContributorsCard', () => {
	beforeEach(() => {
		mockFetch.mockReset();

		jest.spyOn(Date, 'now').mockReturnValue(NOW);
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	it('lists the contributors ordered by the number of assets', async () => {
		mockContributorsRequests({
			buckets: [
				{frequency: 12, term: '1'},
				{frequency: 9, term: '2'},
				{frequency: 1, term: '3'},
			],
		});

		renderTopContributorsCard();

		const items = await screen.findAllByRole('listitem');

		expect(items).toHaveLength(3);

		expect(within(items[0]).getByText('Ana Silva')).toBeInTheDocument();
		expect(within(items[0]).getByText('x-assets')).toBeInTheDocument();
		expect(within(items[1]).getByText('John Doe')).toBeInTheDocument();
		expect(within(items[2]).getByText('deleted-user')).toBeInTheDocument();
		expect(within(items[2]).getByText('x-asset')).toBeInTheDocument();
	});

	it('requests the top five contributors of the last 30 days in the selected space', async () => {
		mockContributorsRequests({});

		renderTopContributorsCard({siteId: 123, value: '456'});

		await screen.findByText(
			'no-assets-were-created-in-the-selected-spaces-for-this-period'
		);

		const [url] = getSearchURLs();

		expect(url).toContain(
			'filter=someFilter and dateCreated ge 2026-09-14T12:00:00.000Z and groupIds/any(g:g eq 123)'
		);
	});

	it('requests the contributors again when the period changes', async () => {
		mockContributorsRequests({});

		renderTopContributorsCard();

		await screen.findByText(
			'no-assets-were-created-in-the-selected-spaces-for-this-period'
		);

		await userEvent.click(
			screen.getByRole('button', {name: /last-30-days/})
		);
		await userEvent.click(screen.getByText('last-7-days'));

		await screen.findByText(
			'no-assets-were-created-in-the-selected-spaces-for-this-period'
		);

		expect(getSearchURLs()[1]).toContain(
			'dateCreated ge 2026-10-07T12:00:00.000Z'
		);
	});

	it('shows a loading indicator while the contributors are requested', async () => {
		mockContributorsRequests({});

		renderTopContributorsCard();

		expect(screen.getByTestId('loading')).toBeInTheDocument();

		await screen.findByText(
			'no-assets-were-created-in-the-selected-spaces-for-this-period'
		);

		expect(screen.queryByTestId('loading')).not.toBeInTheDocument();
	});

	it('shows an empty state when there are no contributors', async () => {
		mockContributorsRequests({});

		renderTopContributorsCard();

		expect(
			await screen.findByText(
				'no-assets-were-created-in-the-selected-spaces-for-this-period'
			)
		).toBeInTheDocument();
		expect(screen.queryByRole('list')).not.toBeInTheDocument();
	});

	it('shows the error instead of the empty state when the request fails', async () => {
		mockContributorsRequests({searchError: 'Unable to search'});

		renderTopContributorsCard();

		expect(await screen.findByText('Unable to search')).toBeInTheDocument();
		expect(
			screen.queryByText(
				'no-assets-were-created-in-the-selected-spaces-for-this-period'
			)
		).not.toBeInTheDocument();
	});

	it('has no accessibility violations', async () => {
		mockContributorsRequests({
			buckets: [
				{frequency: 12, term: '1'},
				{frequency: 9, term: '2'},
			],
		});

		const {container} = renderTopContributorsCard();

		await screen.findAllByRole('listitem');

		await checkAccessibility({bestPractices: true, context: container});
	});
});
