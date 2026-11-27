/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {LabelDisplayType} from '@clayui/label';

export interface IMigration {
	backgroundTaskId: number;
	bars?: IMigrationBar[];
	completed: boolean;
	counts: {label: string; value: number}[];
	createDate: string;
	errors: string[];
	inProgress: boolean;
	itemTitle?: string;
	name: string;
	overallProgress?: number;
	statusDisplayType: LabelDisplayType;
	statusLabel: string;
	warnings: string[];
}

export interface IMigrationBar {
	completed: number;
	current: boolean;
	key: string;
	label: string;
	pending: boolean;
	progress: number;
	total: number;
}
