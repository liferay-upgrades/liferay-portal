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

	if (phase === PHASE_COMPLETED) {
		return Liferay.Language.get('completed');
	}

	if (phase === PHASE_ERROR) {
		return Liferay.Language.get('error');
	}

	return Liferay.Language.get('idle');
}

const StatusTab: React.FC<StatusTabProps> = ({status}) => {
	const [page, setPage] = useState(1);
	const [delta, setDelta] = useState(20);

	const tableRowCounts = status.tableRowCounts || [];

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
					className="progress-bar"
					role="progressbar"
					style={{width: `${status.progress}%`}}
				>
					{`${status.progress}%`}
				</div>
			</div>

			<p className="mt-3 text-secondary">{status.message}</p>

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
