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

import {RecentlyViewed} from '../../../../src/main/resources/META-INF/resources/js';
import {addRecentlyViewedEntry} from '../../../../src/main/resources/META-INF/resources/js/common/utils/recentlyViewed';
import {mockFetch} from '../../__mocks__/frontend-js-web';

const DEFAULT_PROPS = {
	objectDefinitionCssClasses: {default: 'content-icon-custom-structure'},
	objectDefinitionIcons: {default: 'web-content'},
};

function getAsset(id: number, title: string) {
	return {
		embedded: {
			id,
			scopeKey: '20125',
			status: {code: 0, label: 'approved', label_i18n: 'Approved'},
			systemProperties: {
				objectDefinitionBrief: {
					externalReferenceCode: 'L_CMS_BASIC_WEB_CONTENT',
				},
				scope: {externalReferenceCode: 'component-space'},
			},
			title,
		},
	};
}

function mockAPI(assets: ReturnType<typeof getAsset>[]) {
	mockFetch.mockImplementation(((url: string) =>
		Promise.resolve({
			json: async () =>
				url.includes('/o/search/v1.0/search')
					? {items: assets}
					: {id: 1, name: 'Marketing'},
			ok: true,
			status: 200,
		} as Response)) as any);
}

describe('RecentlyViewed', () => {
	beforeEach(() => {
		localStorage.clear();

		mockFetch.mockReset();
	});

	it('shows the empty state when nothing was viewed', async () => {
		render(<RecentlyViewed {...DEFAULT_PROPS} />);

		expect(
			await screen.findByText('nothing-viewed-yet')
		).toBeInTheDocument();
		expect(
			screen.queryByRole('button', {name: 'clear-history'})
		).not.toBeInTheDocument();
	});

	it('shows the viewed assets with their status and space', async () => {
		addRecentlyViewedEntry(1, '/asset/1');

		mockAPI([getAsset(1, 'Black Friday Landing Copy')]);

		render(<RecentlyViewed {...DEFAULT_PROPS} />);

		expect(
			await screen.findByRole('link', {name: 'Black Friday Landing Copy'})
		).toHaveAttribute('href', '/asset/1');
		expect(screen.getByText('approved')).toBeInTheDocument();
		expect(await screen.findByText('Marketing')).toBeInTheDocument();
	});

	it('removes an asset from the history', async () => {
		addRecentlyViewedEntry(1, '/asset/1');
		addRecentlyViewedEntry(2, '/asset/2');

		mockAPI([
			getAsset(1, 'Black Friday Landing Copy'),
			getAsset(2, 'Product Hero Banner'),
		]);

		render(<RecentlyViewed {...DEFAULT_PROPS} />);

		await screen.findByRole('link', {name: 'Product Hero Banner'});

		await userEvent.click(
			screen.getAllByRole('button', {name: 'remove-from-history'})[0]
		);

		expect(
			screen.queryByRole('link', {name: 'Product Hero Banner'})
		).not.toBeInTheDocument();
		expect(
			screen.getByRole('link', {name: 'Black Friday Landing Copy'})
		).toBeInTheDocument();
	});

	it('clears the history', async () => {
		addRecentlyViewedEntry(1, '/asset/1');

		mockAPI([getAsset(1, 'Black Friday Landing Copy')]);

		render(<RecentlyViewed {...DEFAULT_PROPS} />);

		await userEvent.click(
			await screen.findByRole('button', {name: 'clear-history'})
		);

		expect(screen.getByText('nothing-viewed-yet')).toBeInTheDocument();
	});

	it('marks an asset that is no longer available', async () => {
		addRecentlyViewedEntry(1, '/asset/1');

		mockAPI([]);

		render(<RecentlyViewed {...DEFAULT_PROPS} />);

		expect(
			await screen.findByText('no-longer-available')
		).toBeInTheDocument();
		expect(screen.queryByRole('link')).not.toBeInTheDocument();
	});

	it('has no accessibility violations', async () => {
		addRecentlyViewedEntry(1, '/asset/1');

		mockAPI([getAsset(1, 'Black Friday Landing Copy')]);

		const {container} = render(<RecentlyViewed {...DEFAULT_PROPS} />);

		await screen.findByText('Marketing');

		await checkAccessibility({bestPractices: true, context: container});
	});
});
