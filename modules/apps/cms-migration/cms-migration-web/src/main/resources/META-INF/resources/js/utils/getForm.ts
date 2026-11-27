/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {CREATE_NEW_SPACE, MIGRATABLE_CONTENT_TYPES} from './constants';

import type {IForm, IOption} from '../types/Form';
import type {MigratableContentTypeName} from '../types/MigratableContentType';

export default function getForm(
	groups: IOption[],
	spaces: IOption[],
	storedForm: Partial<IForm> = {}
): IForm {
	const migratableContentTypesChecked = Object.fromEntries(
		MIGRATABLE_CONTENT_TYPES.map(({defaultChecked, name}) => [
			name,
			storedForm[name] ?? defaultChecked ?? true,
		])
	) as Record<MigratableContentTypeName, boolean>;

	const storedSpace = spaces.find(
		({value}) => value === storedForm.depotEntryId
	);

	const depotEntryId =
		storedForm.depotEntryId === CREATE_NEW_SPACE
			? CREATE_NEW_SPACE
			: storedSpace?.value ?? spaces[0]?.value ?? CREATE_NEW_SPACE;

	const storedGroup = groups.find(
		({value}) => value === storedForm.sourceGroupId
	);

	const sourceGroupId = storedGroup?.value ?? groups[0]?.value ?? '';

	return {
		...migratableContentTypesChecked,
		depotEntryId,
		sourceGroupId,
		spaceName: storedForm.spaceName ?? '',
		titleObjectFieldNames: storedForm.titleObjectFieldNames ?? '',
	};
}
