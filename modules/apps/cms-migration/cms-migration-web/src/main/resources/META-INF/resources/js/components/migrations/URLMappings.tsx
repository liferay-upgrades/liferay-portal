/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLabel from '@clayui/label';
import ClayPanel from '@clayui/panel';
import ClayTable from '@clayui/table';
import {useSessionState} from 'frontend-js-components-web';
import {sub} from 'frontend-js-web';
import React from 'react';

import {IURLMapping} from '../../types/Migration';

export default function URLMappings({
	exportURLMappingsURL,
	numberFormat,
	sessionKey,
	urlMappings,
	urlMappingsActionRequiredCount,
	urlMappingsCount,
}: {
	exportURLMappingsURL: string;
	numberFormat: Intl.NumberFormat;
	sessionKey: string;
	urlMappings: IURLMapping[];
	urlMappingsActionRequiredCount: number;
	urlMappingsCount: number;
}) {
	const [expanded, setExpanded] = useSessionState<boolean>(sessionKey);

	return (
		<>
			{!!urlMappingsActionRequiredCount && (
				<div className="mt-2 text-warning">
					{sub(
						Liferay.Language.get(
							'cms-migration-x-url-mappings-must-be-updated-manually'
						),
						numberFormat.format(urlMappingsActionRequiredCount)
					)}
				</div>
			)}

			<ClayPanel
				className="mt-2"
				collapsable
				displayTitle={
					<ClayPanel.Title>
						{sub(
							Liferay.Language.get(
								'cms-migration-x-url-mappings'
							),
							numberFormat.format(urlMappingsCount)
						)}
					</ClayPanel.Title>
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
					<div className="align-items-center d-flex justify-content-between mb-2">
						<span className="text-secondary">
							{urlMappingsCount > urlMappings.length
								? sub(
										Liferay.Language.get(
											'cms-migration-showing-x-of-y-url-mappings'
										),
										numberFormat.format(urlMappings.length),
										numberFormat.format(urlMappingsCount)
									)
								: null}
						</span>

						<a
							className="btn btn-secondary btn-sm"
							href={exportURLMappingsURL}
						>
							{Liferay.Language.get('cms-migration-download-csv')}
						</a>
					</div>

					<ClayTable borderless hover={false} responsive>
						<ClayTable.Head>
							<ClayTable.Row>
								<ClayTable.Cell headingCell>
									{Liferay.Language.get('type')}
								</ClayTable.Cell>

								<ClayTable.Cell expanded headingCell>
									{Liferay.Language.get(
										'cms-migration-old-url'
									)}
								</ClayTable.Cell>

								<ClayTable.Cell expanded headingCell>
									{Liferay.Language.get(
										'cms-migration-new-url'
									)}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{Liferay.Language.get('status')}
								</ClayTable.Cell>
							</ClayTable.Row>
						</ClayTable.Head>

						<ClayTable.Body>
							{urlMappings.map(
								(
									{
										actionRequired,
										newURL,
										oldURL,
										statusLabel,
										type,
									},
									index
								) => (
									<ClayTable.Row key={index}>
										<ClayTable.Cell className="text-nowrap">
											{type}
										</ClayTable.Cell>

										<ClayTable.Cell className="text-break">
											{oldURL}
										</ClayTable.Cell>

										<ClayTable.Cell className="text-break">
											{newURL}
										</ClayTable.Cell>

										<ClayTable.Cell>
											<ClayLabel
												displayType={
													actionRequired
														? 'warning'
														: 'secondary'
												}
											>
												{statusLabel}
											</ClayLabel>
										</ClayTable.Cell>
									</ClayTable.Row>
								)
							)}
						</ClayTable.Body>
					</ClayTable>
				</ClayPanel.Body>
			</ClayPanel>
		</>
	);
}
