/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLabel from '@clayui/label';
import React, {useState} from 'react';

import Pager from './Pager';
import {
	PHASE_COMPLETED,
	PHASE_DATA_LOAD,
	PHASE_DISCOVERY,
	PHASE_ERROR,
	PHASE_INDEX_CREATION,
	PHASE_SCHEMA_CREATION,
	PHASE_SCHEMA_VALIDATION,
	Status,
} from './types';

interface StatusTabProps {
	status: Status;
}

function getDisplayType(phase: number) {
	if (phase === PHASE_COMPLETED) {
		return 'success';
	}

	if (phase === PHASE_ERROR) {
		return 'danger';
	}

	return 'info';
}

function getPhaseLabel(phase: number): string {
	if (phase === PHASE_DISCOVERY) {
		return Liferay.Language.get('discovery');
	}

	if (phase === PHASE_SCHEMA_CREATION) {
		return Liferay.Language.get('creating-schema');
	}

	if (phase === PHASE_DATA_LOAD) {
		return Liferay.Language.get('copying-data');
	}

	if (phase === PHASE_INDEX_CREATION) {
		return Liferay.Language.get('creating-indexes');
	}

	if (phase === PHASE_SCHEMA_VALIDATION) {
		return Liferay.Language.get('validating-schema');
	}

	if (phase === PHASE_COMPLETED) {
		return Liferay.Language.get('completed');
	}

	if (phase === PHASE_ERROR) {
		return Liferay.Language.get('error');
	}

	return Liferay.Language.get('idle');
}

function formatDuration(milliseconds: number): string {
	const totalSeconds = Math.floor((milliseconds || 0) / 1000);

	const hours = Math.floor(totalSeconds / 3600);
	const minutes = Math.floor((totalSeconds % 3600) / 60);
	const seconds = totalSeconds % 60;

	const pad = (value: number) => String(value).padStart(2, '0');

	if (hours > 0) {
		return `${hours}:${pad(minutes)}:${pad(seconds)}`;
	}

	return `${minutes}:${pad(seconds)}`;
}

const StatusTab: React.FC<StatusTabProps> = ({status}) => {
	const [page, setPage] = useState(1);
	const [delta, setDelta] = useState(20);

	const tableRowCounts = status.tableRowCounts || [];

	const totalRowsCopied = tableRowCounts.reduce(
		(total, tableRowCount) => total + tableRowCount.rowCount,
		0
	);

	const indeterminate = status.running && status.progress === 0;

	const visibleTableRowCounts = tableRowCounts.slice(
		(page - 1) * delta,
		(page - 1) * delta + delta
	);

	return (
		<div className="pt-3">
			<div className="align-items-center d-flex justify-content-between">
				<h4 className="mb-0">
					{Liferay.Language.get('migration-status')}
				</h4>

				<ClayLabel displayType={getDisplayType(status.phase)}>
					{getPhaseLabel(status.phase)}
				</ClayLabel>
			</div>

			<div className="mt-3 progress">
				<div
					aria-valuemax={100}
					aria-valuemin={0}
					aria-valuenow={status.progress}
					className={
						indeterminate
							? 'progress-bar progress-bar-animated progress-bar-striped'
							: 'progress-bar'
					}
					role="progressbar"
					style={{
						width: indeterminate ? '100%' : `${status.progress}%`,
					}}
				>
					{indeterminate ? '' : `${status.progress}%`}
				</div>
			</div>

			<p className="mb-1 mt-3 text-secondary">{status.message}</p>

			<div className="d-flex flex-wrap text-secondary">
				<span className="mr-4">
					{`${Liferay.Language.get('elapsed-time')}: ${formatDuration(
						status.elapsedTime
					)}`}
				</span>

				<span className="mr-4">
					{`${Liferay.Language.get('tables')}: ${tableRowCounts.length}`}
				</span>

				<span>
					{`${Liferay.Language.get(
						'rows-copied'
					)}: ${totalRowsCopied.toLocaleString()}`}
				</span>
			</div>

			{!!tableRowCounts.length && (
				<>
					<table className="mt-3 table table-autofit table-list">
						<thead>
							<tr>
								<th>{Liferay.Language.get('table')}</th>

								<th className="text-right">
									{Liferay.Language.get('rows-copied')}
								</th>
							</tr>
						</thead>

						<tbody>
							{visibleTableRowCounts.map((tableRowCount) => (
								<tr key={tableRowCount.tableName}>
									<td>{tableRowCount.tableName}</td>

									<td className="text-right">
										{tableRowCount.rowCount}
									</td>
								</tr>
							))}
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
						totalItems={tableRowCounts.length}
					/>
				</>
			)}
		</div>
	);
};

export default StatusTab;
