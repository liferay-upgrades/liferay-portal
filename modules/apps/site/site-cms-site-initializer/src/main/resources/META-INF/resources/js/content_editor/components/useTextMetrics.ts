/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';

import {getContentForm, getEditedTextFields} from '../utils/getEditedContent';
import getTextMetrics, {TextMetrics} from '../utils/getTextMetrics';
import observeFormChanges from '../utils/observeFormChanges';

const UPDATE_DELAY = 300;

export default function useTextMetrics(
	objectDefinitionExternalReferenceCode: string | undefined
): TextMetrics | null {
	const [textMetrics, setTextMetrics] = useState<TextMetrics | null>(null);

	useEffect(() => {
		let current = true;
		let timeoutId: ReturnType<typeof setTimeout> | undefined;

		const updateTextMetrics = () => {
			getEditedTextFields(objectDefinitionExternalReferenceCode)
				.then((fields) => {
					if (current) {
						setTextMetrics(getTextMetrics(fields));
					}
				})
				.catch((error) => {
					console.error(error);
				});
		};

		updateTextMetrics();

		const form = getContentForm();

		const unregisterFormChanges = form
			? observeFormChanges(form, () => {
					clearTimeout(timeoutId);

					timeoutId = setTimeout(updateTextMetrics, UPDATE_DELAY);
				})
			: () => {};

		return () => {
			current = false;

			clearTimeout(timeoutId);
			unregisterFormChanges();
		};
	}, [objectDefinitionExternalReferenceCode]);

	return textMetrics;
}
