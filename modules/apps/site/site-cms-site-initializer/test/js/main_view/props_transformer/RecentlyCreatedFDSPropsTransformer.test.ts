/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import RecentlyCreatedFDSPropsTransformer from '../../../../src/main/resources/META-INF/resources/js/main_view/props_transformer/RecentlyCreatedFDSPropsTransformer';

function transform(props = {}) {
	return RecentlyCreatedFDSPropsTransformer({
		additionalProps: {} as any,
		id: 'com.liferay.site.cms.site.initializer-recentlyCreatedSection',
		views: [],
		...props,
	});
}

describe('RecentlyCreatedFDSPropsTransformer', () => {
	beforeEach(() => {
		jest.clearAllMocks();
	});

	it('sorts the assets by creation date, newest first', () => {
		expect(transform().sorts).toEqual([
			{
				active: true,
				direction: 'desc',
				key: 'dateCreated',
				label: 'created',
			},
		]);
	});

	it('renders the table cells of the all section', () => {
		const tableCellRendererNames =
			transform().customRenderers.tableCell.map(({name}) => name);

		expect(tableCellRendererNames).toEqual(
			expect.arrayContaining([
				'authorTableCellRenderer',
				'simpleActionLinkTableCellRenderer',
				'spaceTableCellRenderer',
			])
		);
	});

	it('shows no creation menu items when there is no creation menu', () => {
		expect(transform().creationMenu.primaryItems).toEqual([]);
	});
});
