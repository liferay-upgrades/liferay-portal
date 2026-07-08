/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import React, {useState} from 'react';

import Pager from './Pager';
import {MigrationErrorItem} from './types';

interface ErrorsTabProps {
	errors: MigrationErrorItem[];
}

const ErrorsTab: React.FC<ErrorsTabProps> = ({errors}) => {
	const [page, setPage] = useState(1);
	const [delta, setDelta] = useState(20);

	if (!errors || !errors.length) {
		return (
			<p className="pt-3 text-secondary">
				{Liferay.Language.get('no-errors-were-found')}
			</p>
		);
	}

	const visibleErrors = errors.slice(
		(page - 1) * delta,
		(page - 1) * delta + delta
	);

	return (
		<div className="pt-3">
			<p className="text-secondary">
				{Liferay.Language.get(
					'some-rows-could-not-be-copied-and-were-skipped'
				)}
			</p>

			<table className="table table-autofit table-list">
				<thead>
					<tr>
						<th>{Liferay.Language.get('table')}</th>

						<th>{Liferay.Language.get('row')}</th>

						<th>{Liferay.Language.get('error')}</th>

						<th>{Liferay.Language.get('suggested-fix-query')}</th>
					</tr>
				</thead>

				<tbody>
					{visibleErrors.map((error, index) => (
						<tr key={`${error.tableName}-${index}`}>
							<td>{error.tableName}</td>

							<td>
								<code>{error.rowIdentifier}</code>
							</td>

							<td>
								{error.sqlState ? (
									<span className="text-danger">
										{`[${error.sqlState}] `}
									</span>
								) : null}

								{error.message}
							</td>

							<td>
								<textarea
									className="form-control"
									readOnly
									rows={2}
									value={error.suggestedSQL}
								/>

								<ClayButton
									className="mt-2"
									displayType="secondary"
									onClick={() =>
										navigator.clipboard.writeText(
											error.suggestedSQL
										)
									}
									small
								>
									{Liferay.Language.get('copy')}
								</ClayButton>
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
				totalItems={errors.length}
			/>
		</div>
	);
};

export default ErrorsTab;
