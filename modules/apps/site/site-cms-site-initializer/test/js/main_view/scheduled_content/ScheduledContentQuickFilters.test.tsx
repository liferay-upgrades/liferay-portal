/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import toDatePart from '../../../../src/main/resources/META-INF/resources/js/common/utils/toDatePart';
import ScheduledContentQuickFilters from '../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/ScheduledContentQuickFilters';
import ScheduledContentService from '../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/ScheduledContentService';
import {
	SCHEDULED_CONTENT_QUICK_FILTER_TYPES,
	getQuickFilterDateRange,
} from '../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/constants';

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/ScheduledContentService',
	() => ({__esModule: true, default: {getScheduledContentCount: jest.fn()}})
);

const mockSetFDSState = jest.fn();

let mockFDSState: any;

jest.mock('@liferay/frontend-js-state-web/react', () => ({
	useLiferayState: () => [mockFDSState, mockSetFDSState],
}));

(global as any).Liferay = {
	Language: {
		get: jest.fn((key: string) => key),
	},
	ThemeDisplay: {
		getTimeZone: jest.fn(() => 'UTC'),
	},
	detach: jest.fn(),
	on: jest.fn(),
};

const mockGetScheduledContentCount =
	ScheduledContentService.getScheduledContentCount as jest.Mock;

const API_URL = '/o/search/v1.0/search?filter=status eq 7';

function findQuickFilterButton(label: string) {
	return screen.getByText(label).closest('button') as HTMLButtonElement;
}

describe('[CMS Scheduled Content] ScheduledContentQuickFilters', () => {
	beforeEach(() => {
		jest.clearAllMocks();

		mockFDSState = {
			filters: [
				{active: false, id: 'scopeGroupId', selectedData: {}},
				{active: false, id: 'dateDisplay', selectedData: {}},
			],
			search: {query: ''},
		};

		mockGetScheduledContentCount
			.mockResolvedValueOnce({count: 1, error: null})
			.mockResolvedValueOnce({count: 3, error: null})
			.mockResolvedValueOnce({count: 5, error: null});
	});

	it('shows one button per period with its count', async () => {
		render(<ScheduledContentQuickFilters apiURL={API_URL} />);

		expect(
			screen.getByRole('group', {name: 'publish-date'})
		).toBeInTheDocument();

		await waitFor(() => expect(screen.getByText('5')).toBeInTheDocument());

		expect(findQuickFilterButton('today')).toHaveTextContent('1');
		expect(findQuickFilterButton('this-week')).toHaveTextContent('3');
		expect(findQuickFilterButton('this-month')).toHaveTextContent('5');
	});

	it('applies the publish date range of the clicked period', async () => {
		render(<ScheduledContentQuickFilters apiURL={API_URL} />);

		await userEvent.click(findQuickFilterButton('today'));

		const {from, to} = getQuickFilterDateRange(
			SCHEDULED_CONTENT_QUICK_FILTER_TYPES.TODAY
		);

		expect(mockSetFDSState).toHaveBeenCalledWith({
			...mockFDSState,
			filters: [
				mockFDSState.filters[0],
				{
					active: true,
					id: 'dateDisplay',
					selectedData: {
						exclude: false,
						from: toDatePart(from as Date),
						to: toDatePart(to as Date),
					},
				},
			],
		});
	});

	it('marks the button of the active period and clears it on a second click', async () => {
		const {from, to} = getQuickFilterDateRange(
			SCHEDULED_CONTENT_QUICK_FILTER_TYPES.THIS_WEEK
		);

		mockFDSState.filters[1] = {
			active: true,
			id: 'dateDisplay',
			selectedData: {
				exclude: false,
				from: toDatePart(from as Date),
				to: toDatePart(to as Date),
			},
		};

		render(<ScheduledContentQuickFilters apiURL={API_URL} />);

		expect(findQuickFilterButton('this-week')).toHaveAttribute(
			'aria-pressed',
			'true'
		);
		expect(findQuickFilterButton('today')).toHaveAttribute(
			'aria-pressed',
			'false'
		);

		await userEvent.click(findQuickFilterButton('this-week'));

		expect(mockSetFDSState).toHaveBeenCalledWith({
			...mockFDSState,
			filters: [
				mockFDSState.filters[0],
				{
					active: false,
					id: 'dateDisplay',
					odataFilterString: undefined,
					selectedData: undefined,
				},
			],
		});
	});
});
