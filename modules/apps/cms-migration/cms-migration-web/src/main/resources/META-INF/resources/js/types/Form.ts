/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {MigratableContentTypeName} from './MigratableContentType';

export interface IForm extends Record<MigratableContentTypeName, boolean> {
	depotEntryId: string;
	sourceGroupId: string;
	spaceName: string;
	titleObjectFieldNames: string;
}

export interface IOption {
	label: string;
	value: string;
}
