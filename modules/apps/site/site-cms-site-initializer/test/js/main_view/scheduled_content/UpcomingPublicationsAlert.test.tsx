/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';
import {act, render, screen, waitFor} from '@testing-library/react';
import React from 'react';

import ScheduledContentService from '../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/ScheduledContentService';
import UpcomingPublicationsAlert from '../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/UpcomingPublicationsAlert';

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/ScheduledContentService',
	() => ({__esModule: true, default: {getScheduledContentCount: jest.fn()}})
);

const mockGetScheduledContentCount =
	ScheduledContentService.getScheduledContentCount as jest.Mock;

const liferayEventHandlers: Record<string, Function[]> = {};

(global as any).Liferay = {
	Language: {
		get: jest.fn((key: string) => key),
	},
	detach: jest.fn((event: string, handler: Function) => {
		liferayEventHandlers[event] = (
			liferayEventHandlers[event] ?? []
		).filter((registered) => registered !== handler);
	}),
	fire: jest.fn((event: string, payload?: unknown) => {
		(liferayEventHandlers[event] ?? []).forEach((handler) =>
			handler(payload)
		);
	}),
	on: jest.fn((event: string, handler: Function) => {
		liferayEventHandlers[event] = [
			...(liferayEventHandlers[event] ?? []),
			handler,
		];
	}),
};

const API_URL = '/o/search/v1.0/search?filter=status eq 7';

describe('[CMS Scheduled Content] UpcomingPublicationsAlert', () => {
	beforeEach(() => {
		jest.clearAllMocks();
		Object.keys(liferayEventHandlers).forEach(
			(key) => delete liferayEventHandlers[key]
		);
	});

	it('renders nothing when no content publishes in the next hour', async () => {
		mockGetScheduledContentCount.mockResolvedValueOnce({
			count: 0,
			error: null,
		});

		const {container} = render(
			<UpcomingPublicationsAlert apiURL={API_URL} />
		);

		await waitFor(() =>
			expect(mockGetScheduledContentCount).toHaveBeenCalled()
		);

		expect(container).toBeEmptyDOMElement();
	});

	it('asks for the content due in the next hour', async () => {
		mockGetScheduledContentCount.mockResolvedValueOnce({
			count: 0,
			error: null,
		});

		render(<UpcomingPublicationsAlert apiURL={API_URL} />);

		await waitFor(() =>
			expect(mockGetScheduledContentCount).toHaveBeenCalled()
		);

		const [apiURL, {from, to}] = mockGetScheduledContentCount.mock.calls[0];

		expect(apiURL).toBe(API_URL);
		expect(from).toBeUndefined();
		expect(to.getTime() - Date.now()).toBeGreaterThan(59 * 60 * 1000);
		expect(to.getTime() - Date.now()).toBeLessThanOrEqual(60 * 60 * 1000);
	});

	it('announces a single upcoming publication', async () => {
		mockGetScheduledContentCount.mockResolvedValueOnce({
			count: 1,
			error: null,
		});

		render(<UpcomingPublicationsAlert apiURL={API_URL} />);

		expect(
			await screen.findByText(
				'one-asset-will-be-published-in-the-next-hour'
			)
		).toBeInTheDocument();
		expect(screen.getByRole('status')).toBeInTheDocument();
	});

	it('announces several upcoming publications', async () => {
		mockGetScheduledContentCount.mockResolvedValueOnce({
			count: 3,
			error: null,
		});

		render(<UpcomingPublicationsAlert apiURL={API_URL} />);

		expect(
			await screen.findByText(
				'x-assets-will-be-published-in-the-next-hour'
			)
		).toBeInTheDocument();
	});

	it('refreshes the count when the scheduled content data set updates', async () => {
		mockGetScheduledContentCount
			.mockResolvedValueOnce({count: 0, error: null})
			.mockResolvedValueOnce({count: 2, error: null});

		render(<UpcomingPublicationsAlert apiURL={API_URL} />);

		await waitFor(() =>
			expect(mockGetScheduledContentCount).toHaveBeenCalledTimes(1)
		);

		act(() => {
			Liferay.fire('fds-display-updated', {
				id: 'com.liferay.site.cms.site.initializer-scheduledContentSection',
			});
		});

		expect(
			await screen.findByText(
				'x-assets-will-be-published-in-the-next-hour'
			)
		).toBeInTheDocument();
		expect(mockGetScheduledContentCount).toHaveBeenCalledTimes(2);
	});
});
