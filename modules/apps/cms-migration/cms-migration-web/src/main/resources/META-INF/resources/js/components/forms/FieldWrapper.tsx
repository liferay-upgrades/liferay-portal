/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayForm from '@clayui/form';
import React, {ReactNode} from 'react';

import RequiredMark from './RequiredMark';

interface IProps {
	children: ReactNode;
	fieldId: string;
	label: string;
	required?: boolean;
}

export default function FieldWrapper({
	children,
	fieldId,
	label,
	required,
}: IProps) {
	return (
		<ClayForm.Group>
			<label htmlFor={fieldId}>
				{label}

				{required && <RequiredMark />}
			</label>

			{children}
		</ClayForm.Group>
	);
}
