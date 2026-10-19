/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {IInternalRenderer, IItemsActions} from '@liferay/frontend-data-set-web';
import {sub} from 'frontend-js-web';
import React from 'react';

import {NO_VALUE} from '../../common/utils/constants';
import {formatExpirationDate} from '../../common/utils/expirationStatus';
import formatTimeUntil from '../../common/utils/formatTimeUntil';
import {getScopeExternalReferenceCode} from '../../common/utils/getScopeExternalReferenceCode';
import {scheduledContentFDSAtom} from '../scheduled_content/atoms';
import {AdditionalProps} from './AssetsFDSPropsTransformer';
import AssetTypeRenderer from './cell_renderers/AssetTypeRenderer';
import AuthorRenderer from './cell_renderers/AuthorRenderer';
import SimpleActionLinkRenderer from './cell_renderers/SimpleActionLinkRenderer';
import SpaceRendererWithCache from './cell_renderers/SpaceRendererWithCache';

type ScheduledContentAdditionalProps = Pick<
	AdditionalProps,
	| 'fileMimeTypeCssClasses'
	| 'fileMimeTypeIcons'
	| 'objectDefinitionCssClasses'
	| 'objectDefinitionIcons'
> &
	Partial<Pick<AdditionalProps, 'additionalAPIURLParameters'>>;

export function PublishDateRenderer({value}: {value?: string}) {
	if (!value) {
		return <>{NO_VALUE}</>;
	}

	const date = new Date(value);

	return (
		<div className="d-flex flex-column">
			<span>{formatExpirationDate(date)}</span>

			<span className="text-3 text-secondary">
				{sub(
					Liferay.Language.get('publishes-x'),
					formatTimeUntil(date)
				)}
			</span>
		</div>
	);
}

export default function ScheduledContentFDSPropsTransformer({
	additionalProps,
	itemsActions = [],
	...otherProps
}: {
	additionalProps: ScheduledContentAdditionalProps;
	apiURL?: string;
	id?: string;
	itemsActions?: IItemsActions[];
	searchAsYouType?: boolean;
	searchSuggestionsEnabled?: boolean;
}) {
	const {additionalAPIURLParameters, ...remainingAdditionalProps} =
		additionalProps || {};

	return {
		...otherProps,
		additionalAPIURLParameters,
		additionalProps: remainingAdditionalProps,
		atom: scheduledContentFDSAtom,
		customRenderers: {
			tableCell: [
				{
					component: AssetTypeRenderer,
					name: 'assetTypeTableCellRenderer',
					type: 'internal',
				} as IInternalRenderer,
				{
					component: AuthorRenderer,
					name: 'authorTableCellRenderer',
					type: 'internal',
				} as IInternalRenderer,
				{
					component: PublishDateRenderer,
					name: 'publishDateTableCellRenderer',
					type: 'internal',
				} as IInternalRenderer,
				{
					component: ({actions, itemData, options, value}) => (
						<SimpleActionLinkRenderer
							actions={actions}
							additionalProps={remainingAdditionalProps}
							itemData={itemData}
							options={options}
							value={value}
						/>
					),
					name: 'simpleActionLinkTableCellRenderer',
					type: 'internal',
				} as IInternalRenderer,
				{
					component: ({itemData}) => (
						<SpaceRendererWithCache
							scopeKey={itemData.embedded?.scopeKey}
							spaceExternalReferenceCode={getScopeExternalReferenceCode(
								itemData
							)}
						/>
					),
					name: 'spaceTableCellRenderer',
					type: 'internal',
				} as IInternalRenderer,
			],
		},
		hideManagementBarInEmptyState: true,
		itemsActions,
		searchAsYouType: otherProps.searchAsYouType ?? true,
		searchSuggestionsEnabled: otherProps.searchSuggestionsEnabled ?? true,
		sorts: [
			{
				active: true,
				direction: 'asc',
				key: 'dateDisplay',
				label: Liferay.Language.get('publish-date'),
			},
			{
				direction: 'asc',
				key: 'title',
				label: Liferay.Language.get('title'),
			},
		],
	};
}
