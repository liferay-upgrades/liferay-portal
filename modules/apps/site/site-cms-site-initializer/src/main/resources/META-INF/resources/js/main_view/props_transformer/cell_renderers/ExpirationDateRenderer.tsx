/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import React from 'react';

import {isExpiringSoon} from '../../../common/utils/expirationStatus';
import DateRenderer from './DateRenderer';

const ExpirationDateRenderer = ({
	itemData,
	value,
}: {
	itemData?: {dateExpiration?: string};
	value?: string;
}) => (
	<DateRenderer
		isWarning={isExpiringSoon}
		value={value || itemData?.dateExpiration}
	/>
);

export default ExpirationDateRenderer;
