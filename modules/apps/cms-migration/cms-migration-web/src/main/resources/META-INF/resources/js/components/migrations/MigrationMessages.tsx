/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayPanel from '@clayui/panel';
import {useSessionState} from 'frontend-js-components-web';
import React from 'react';

export default function Messages({
	className,
	label,
	messages,
	sessionKey,
}: {
	className: string;
	label: string;
	messages: string[];
	sessionKey: string;
}) {
	const [expanded, setExpanded] = useSessionState<boolean>(sessionKey);

	if (!messages.length) {
		return null;
	}

	return (
		<ClayPanel
			className="mt-2"
			collapsable
			displayTitle={
				<ClayPanel.Title className={className}>{label}</ClayPanel.Title>
			}
			displayType="unstyled"
			expanded={!!expanded}
			onExpandedChange={setExpanded}
			showCollapseIcon
			style={
				{
					'--panel-header-collapse-icon-right': '0',
				} as React.CSSProperties
			}
		>
			<ClayPanel.Body>
				<ul>
					{messages.map((message, index) => (
						<li key={index}>{message}</li>
					))}
				</ul>
			</ClayPanel.Body>
		</ClayPanel>
	);
}
