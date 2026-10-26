/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {IBulkActionItem, IView} from '@liferay/frontend-data-set-web';

import AssetsFDSPropsTransformer, {
	AdditionalProps,
} from './AssetsFDSPropsTransformer';

export default function RecentlyCreatedFDSPropsTransformer({
	additionalProps,
	bulkActions,
	creationMenu = {primaryItems: []},
	itemsActions = [],
	views,
	...otherProps
}: {
	additionalProps: AdditionalProps;
	apiURL?: string;
	bulkActions?: Array<IBulkActionItem>;
	creationMenu?: any;
	id?: string;
	itemsActions?: any[];
	views: IView[];
}) {
	return {
		...AssetsFDSPropsTransformer({
			additionalProps,
			bulkActions,
			creationMenu,
			itemsActions,
			views,
			...otherProps,
		}),
		sorts: [
			{
				active: true,
				direction: 'desc',
				key: 'dateCreated',
				label: Liferay.Language.get('created'),
			},
		],
	};
}
