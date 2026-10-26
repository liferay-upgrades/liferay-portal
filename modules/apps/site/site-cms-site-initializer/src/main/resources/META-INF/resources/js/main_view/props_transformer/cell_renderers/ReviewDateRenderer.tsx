/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import React from 'react';

import {isReviewDateOverdue} from '../../../common/utils/reviewDateStatus';
import DateRenderer from './DateRenderer';

const ReviewDateRenderer = ({
	itemData,
	value,
}: {
	itemData?: {dateReview?: string};
	value?: string;
}) => (
	<DateRenderer
		isWarning={isReviewDateOverdue}
		value={value || itemData?.dateReview}
	/>
);

export default ReviewDateRenderer;
