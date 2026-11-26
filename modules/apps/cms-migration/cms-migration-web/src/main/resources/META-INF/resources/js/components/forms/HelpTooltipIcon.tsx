/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import React from 'react';

interface IProps {
	className?: string;
	message: string;
}

export default function HelpTooltipIcon({className = 'ml-1', message}: IProps) {
	return (
		<span
			aria-label={message}
			className={classNames(
				'lfr-portal-tooltip text-secondary',
				className
			)}
			data-title={message}
			onMouseDown={(event) => event.preventDefault()}
			role="img"
			tabIndex={0}
		>
			<ClayIcon className="mt-0" symbol="question-circle" />
		</span>
	);
}
