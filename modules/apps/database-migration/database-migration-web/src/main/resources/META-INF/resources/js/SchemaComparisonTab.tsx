/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayLabel from '@clayui/label';
import {fetch} from 'frontend-js-web';
import React, {useEffect, useState} from 'react';

import Pager from './Pager';
import {SchemaComparison, TableComparison} from './types';

interface SchemaComparisonTabProps {
	phase: number;
	schemaComparisonURL: string;
}

function getStatus(tableComparison: TableComparison) {
	if (!tableComparison.onTarget) {
		return {
			displayType: 'danger' as const,
			label: Liferay.Language.get('missing-on-target'),
		};
	}

	if (!tableComparison.onSource) {
		return {
			displayType: 'warning' as const,
			label: Liferay.Language.get('target-only'),
		};
	}

	if (tableComparison.sourceRowCount !== tableComparison.targetRowCount) {
		return {
			displayType: 'warning' as const,
			label: Liferay.Language.get('row-count-mismatch'),
		};
	}

	return {
		displayType: 'success' as const,
		label: Liferay.Language.get('match'),
	};
}

const SchemaComparisonTab: React.FC<SchemaComparisonTabProps> = ({
	phase,
	schemaComparisonURL,
}) => {
	const [schemaComparison, setSchemaComparison] =
		useState<SchemaComparison | null>(null);
	const [expanded, setExpanded] = useState<string[]>([]);
	const [page, setPage] = useState(1);
	const [delta, setDelta] = useState(20);

	useEffect(() => {
		fetch(schemaComparisonURL)
			.then((response) => response.json())
			.then((json) => setSchemaComparison(json))
			.catch(() => {});
	}, [schemaComparisonURL, phase]);

	if (!schemaComparison || !schemaComparison.available) {
		return (
			<p className="pt-3 text-secondary">
				{Liferay.Language.get(
					'the-schema-comparison-is-available-after-a-migration-completes'
				)}
			</p>
		);
	}

	const tableComparisons = schemaComparison.tableComparisons;

	const visibleTableComparisons = tableComparisons.slice(
		(page - 1) * delta,
		(page - 1) * delta + delta
	);

	const toggle = (tableName: string) =>
		setExpanded((previous) =>
			previous.includes(tableName)
				? previous.filter((name) => name !== tableName)
				: [...previous, tableName]
		);

	return (
		<div className="pt-3">
			<p className="text-secondary">
				{Liferay.Language.get(
					'compare-the-source-and-target-schemas-table-by-table'
				)}
			</p>

			<table className="table table-autofit table-list">
				<thead>
					<tr>
						<th />

						<th>{Liferay.Language.get('table')}</th>

						<th className="text-right">
							{Liferay.Language.get('source-rows')}
						</th>

						<th className="text-right">
							{Liferay.Language.get('target-rows')}
						</th>

						<th>{Liferay.Language.get('status')}</th>
					</tr>
				</thead>

				<tbody>
					{visibleTableComparisons.map((tableComparison) => {
						const status = getStatus(tableComparison);

						const isExpanded = expanded.includes(
							tableComparison.tableName
						);

						return (
							<React.Fragment key={tableComparison.tableName}>
								<tr>
									<td>
										<ClayButton
											aria-label={Liferay.Language.get(
												'columns'
											)}
											borderless
											displayType="secondary"
											monospaced
											onClick={() =>
												toggle(
													tableComparison.tableName
												)
											}
											size="sm"
										>
											{isExpanded ? '-' : '+'}
										</ClayButton>
									</td>

									<td>{tableComparison.tableName}</td>

									<td className="text-right">
										{tableComparison.onSource
											? tableComparison.sourceRowCount
											: '-'}
									</td>

									<td className="text-right">
										{tableComparison.onTarget
											? tableComparison.targetRowCount
											: '-'}
									</td>

									<td>
										<ClayLabel
											displayType={status.displayType}
										>
											{status.label}
										</ClayLabel>
									</td>
								</tr>

								{isExpanded && (
									<tr>
										<td colSpan={5}>
											<table className="table table-autofit table-list">
												<thead>
													<tr>
														<th>
															{Liferay.Language.get(
																'column'
															)}
														</th>

														<th>
															{Liferay.Language.get(
																'source-type'
															)}
														</th>

														<th>
															{Liferay.Language.get(
																'target-type'
															)}
														</th>
													</tr>
												</thead>

												<tbody>
													{tableComparison.columnComparisons.map(
														(columnComparison) => (
															<tr
																key={
																	columnComparison.columnName
																}
															>
																<td>
																	{
																		columnComparison.columnName
																	}
																</td>

																<td>
																	{columnComparison.sourceType ||
																		'-'}
																</td>

																<td>
																	{columnComparison.targetType ||
																		'-'}
																</td>
															</tr>
														)
													)}
												</tbody>
											</table>
										</td>
									</tr>
								)}
							</React.Fragment>
						);
					})}
				</tbody>
			</table>

			<Pager
				activePage={page}
				delta={delta}
				onActiveChange={setPage}
				onDeltaChange={(newDelta) => {
					setDelta(newDelta);
					setPage(1);
				}}
				totalItems={tableComparisons.length}
			/>
		</div>
	);
};

export default SchemaComparisonTab;
