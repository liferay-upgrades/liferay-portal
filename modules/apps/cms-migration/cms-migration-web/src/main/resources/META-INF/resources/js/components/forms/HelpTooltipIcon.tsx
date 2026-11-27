/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import React from 'react';

interface IProps {
	message: string;
}

export default function HelpTooltipIcon({message}: IProps) {
	return (
		<span
			aria-label={message}
			className="lfr-portal-tooltip ml-1 text-secondary"
			data-title={message}
			onMouseDown={(event) => event.preventDefault()}
			role="img"
			tabIndex={0}
		>
			<ClayIcon className="mt-0" symbol="question-circle" />
		</span>
	);
}
