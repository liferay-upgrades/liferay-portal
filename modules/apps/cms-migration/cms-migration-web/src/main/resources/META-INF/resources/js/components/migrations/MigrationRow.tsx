/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import ClayLabel from '@clayui/label';
import ClayLayout from "@clayui/layout";
import ClayProgressBar from '@clayui/progress-bar';
import { sub } from 'frontend-js-web';
import React from "react";

import { IMigration } from "../../types/Migration";
import {downloadCSV} from '../../utils/downloadCSV';
import MigrationMessage from './MigrationMessage';


interface IMigrationProps {
    migration: IMigration;
    numberFormat: Intl.NumberFormat;
}

export default function MigrationRow({migration, numberFormat}: IMigrationProps) {
	const {
		backgroundTaskId,
		completed,
		counts,
		createDate,
		errors,
		inProgress,
		itemTitle,
		name,
		phaseLabel,
		progress,
		statusDisplayType,
		statusLabel,
		warnings,
	} = migration;

	return (
		<div className="border-bottom mb-3 pb-3">
			<ClayLayout.ContentRow noGutters="x" padded verticalAlign="center">
				<ClayLayout.ContentCol expand>
					<h3 className="h4">{name}</h3>

					<div className="text-secondary">{createDate}</div>
				</ClayLayout.ContentCol>

				<ClayLayout.ContentCol>
					<ClayLabel displayType={statusDisplayType}>
						{statusLabel}
					</ClayLabel>
				</ClayLayout.ContentCol>

				{completed && (!!errors.length || !!warnings.length) && (
					<ClayLayout.ContentCol>
						<ClayButton
							displayType="secondary"
							onClick={() =>
								downloadCSV({errors, name, warnings})
							}
							size="sm"
						>
							<span className="inline-item inline-item-before">
								<ClayIcon symbol="document-default" />
							</span>

							{Liferay.Language.get('open-logs')}

							<span className="sr-only">{` ${name}`}</span>
						</ClayButton>
					</ClayLayout.ContentCol>
				)}
			</ClayLayout.ContentRow>

			{inProgress && (
				<>
					<div className="mt-2">
						{phaseLabel}

						{itemTitle && (
							<span className="text-secondary"> {itemTitle}</span>
						)}
					</div>

					<ClayProgressBar value={progress} />
				</>
			)}

			{completed && (
				<>
					<ul
						className="c-gapx-5 d-md-grid list-unstyled mt-2"
						style={{gridTemplateColumns: 'repeat(2, max-content)'}}
					>
						{counts.map(({label, value}) => (
							<li className="text-nowrap" key={label}>
								<strong>{numberFormat.format(value)}</strong>

								{` ${label}`}
							</li>
						))}
					</ul>

					<MigrationMessage
						className="text-danger"
						label={sub(
							Liferay.Language.get('cms-migration-x-errors'),
							numberFormat.format(errors.length)
						)}
						messages={errors}
						sessionKey={`cms-migration-errors-expanded-${backgroundTaskId}`}
					/>

					<MigrationMessage
						className="text-warning"
						label={sub(
							Liferay.Language.get('cms-migration-x-warnings'),
							numberFormat.format(warnings.length)
						)}
						messages={warnings}
						sessionKey={`cms-migration-warnings-expanded-${backgroundTaskId}`}
					/>
				</>
			)}
		</div>
	);
}
