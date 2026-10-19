/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import React from 'react';

import ScheduledContentFDSPropsTransformer from '../../../../src/main/resources/META-INF/resources/js/main_view/props_transformer/ScheduledContentFDSPropsTransformer';
import {scheduledContentFDSAtom} from '../../../../src/main/resources/META-INF/resources/js/main_view/scheduled_content/atoms';

jest.mock('@liferay/frontend-data-set-web', () => ({
	findAction: jest.fn(() => ({data: {id: 'edit'}, href: '/edit/{id}'})),
	replaceTokens: jest.fn(() => '/edit/1'),
	useFDSRecordVisit: () => jest.fn(),
}));

jest.mock('frontend-js-web', () => ({
	sub: jest.fn((key, ...args) => {
		let result = key;

		args.forEach((arg, i) => {
			result = result.replace(`{${i}}`, arg);
		});

		return result;
	}),
}));

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/common/utils/expirationStatus',
	() => ({formatExpirationDate: jest.fn(() => '10/07/2026 04:00 PM')})
);

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/common/utils/formatTimeUntil',
	() => ({__esModule: true, default: jest.fn(() => 'in 2 hours')})
);

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/main_view/props_transformer/cell_renderers/SpaceRendererWithCache',
	() => ({__esModule: true, default: () => null})
);

const DATA_SET_ID =
	'com.liferay.site.cms.site.initializer-scheduledContentSection';
const TITLE = 'Test schedule content';

describe('[CMS Scheduled Content] ScheduledContentFDSPropsTransformer', () => {
	const transform = () =>
		ScheduledContentFDSPropsTransformer({
			additionalProps: {
				additionalAPIURLParameters: 'filter=status eq 7',
				fileMimeTypeCssClasses: {},
				fileMimeTypeIcons: {},
				objectDefinitionCssClasses: {},
				objectDefinitionIcons: {},
			},
			id: DATA_SET_ID,
		}) as any;

	const getRenderer = (name: string) =>
		transform().customRenderers.tableCell.find(
			(renderer: {name: string}) => renderer.name === name
		).component;

	beforeEach(() => {
		jest.clearAllMocks();
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	it('sorts the scheduled content by publish date, soonest first', () => {
		expect(transform().sorts).toEqual([
			{
				active: true,
				direction: 'asc',
				key: 'dateDisplay',
				label: 'publish-date',
			},
			{direction: 'asc', key: 'title', label: 'title'},
		]);
	});

	it('shares its state through the scheduled content atom', () => {
		expect(transform().atom).toBe(scheduledContentFDSAtom);
	});

	it('keeps the additional API URL parameters out of the additional props', () => {
		const props = transform();

		expect(props.additionalAPIURLParameters).toBe('filter=status eq 7');
		expect(props.additionalProps).not.toHaveProperty(
			'additionalAPIURLParameters'
		);
	});

	it('registers a renderer for each column of the table view', () => {
		const names = transform().customRenderers.tableCell.map(
			(renderer: {name: string}) => renderer.name
		);

		expect(names).toEqual([
			'assetTypeTableCellRenderer',
			'authorTableCellRenderer',
			'publishDateTableCellRenderer',
			'simpleActionLinkTableCellRenderer',
			'spaceTableCellRenderer',
		]);
	});

	it('renders the scheduled publish date with the relative time', () => {
		jest.spyOn(Liferay.Language, 'get').mockImplementation((key) =>
			key === 'publishes-x' ? 'Publishes {0}' : key
		);

		const PublishDateRenderer = getRenderer('publishDateTableCellRenderer');

		render(<PublishDateRenderer value="2026-10-07T19:00:00Z" />);

		expect(screen.getByText('10/07/2026 04:00 PM')).toBeInTheDocument();
		expect(screen.getByText('Publishes in 2 hours')).toBeInTheDocument();
	});

	it('renders a placeholder when the content has no publish date', () => {
		const PublishDateRenderer = getRenderer('publishDateTableCellRenderer');

		render(<PublishDateRenderer />);

		expect(screen.getByText('--')).toBeInTheDocument();
	});

	it('links the title with the update permission', () => {
		const TitleRenderer = getRenderer('simpleActionLinkTableCellRenderer');

		render(
			<TitleRenderer
				actions={[{data: {id: 'edit'}, href: '/edit/{id}'}]}
				itemData={{actions: {update: {}}, embedded: {title: TITLE}}}
				options={{actionId: 'edit'}}
				value={TITLE}
			/>
		);

		expect(screen.getByRole('link', {name: TITLE})).toBeInTheDocument();
	});
});
