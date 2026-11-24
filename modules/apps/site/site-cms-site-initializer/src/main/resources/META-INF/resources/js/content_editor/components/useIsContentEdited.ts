/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';

import observeFormChanges from '../utils/observeFormChanges';

export default function useIsContentEdited(formSelector: string): boolean {
	const [isContentEdited, setIsContentEdited] = useState(false);

	useEffect(() => {
		const form = document.querySelector(formSelector);

		if (!form) {
			return;
		}

		return observeFormChanges(form, () => setIsContentEdited(true));
	}, [formSelector]);

	return isContentEdited;
}
