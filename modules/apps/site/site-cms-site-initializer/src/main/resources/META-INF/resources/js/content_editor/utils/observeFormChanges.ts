/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import interceptValueSetter, {
	EditableElement,
} from '../../common/utils/interceptValueSetter';

export default function observeFormChanges(
	form: Element,
	onChange: () => void
): () => void {
	const abortController = new AbortController();
	const {signal} = abortController;

	form.addEventListener('change', onChange, {signal});
	form.addEventListener('input', onChange, {signal});

	let observer: MutationObserver | null = null;

	const unregisters: Array<() => void> = [];

	const interceptAllInputs = () => {
		form.querySelectorAll<EditableElement>('input, textarea').forEach(
			(element) =>
				unregisters.push(interceptValueSetter(element, onChange))
		);
	};

	form.addEventListener(
		'focusin',
		() => {
			interceptAllInputs();

			observer = new MutationObserver(interceptAllInputs);

			observer.observe(form, {childList: true, subtree: true});
		},
		{once: true, signal}
	);

	return () => {
		abortController.abort();
		observer?.disconnect();
		unregisters.forEach((unregister) => unregister());
	};
}
