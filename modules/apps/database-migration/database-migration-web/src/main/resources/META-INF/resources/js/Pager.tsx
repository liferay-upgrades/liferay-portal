/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayPaginationBarWithBasicItems} from '@clayui/pagination-bar';
import React from 'react';

const DELTAS = [{label: 10}, {label: 20}, {label: 50}, {label: 100}];

interface PagerProps {
	activePage: number;
	delta: number;
	onActiveChange: (page: number) => void;
	onDeltaChange: (delta: number) => void;
	totalItems: number;
}

const Pager: React.FC<PagerProps> = ({
	activePage,
	delta,
	onActiveChange,
	onDeltaChange,
	totalItems,
}) => {
	if (totalItems === 0) {
		return null;
	}

	return (
		<ClayPaginationBarWithBasicItems
			active={activePage}
			activeDelta={delta}
			deltas={DELTAS}
			ellipsisBuffer={3}
			onActiveChange={onActiveChange}
			onDeltaChange={onDeltaChange}
			totalItems={totalItems}
		/>
	);
};

export default Pager;
