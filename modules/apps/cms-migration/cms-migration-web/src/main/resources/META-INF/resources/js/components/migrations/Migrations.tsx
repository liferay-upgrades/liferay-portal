/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLayout from '@clayui/layout';
import ClayPanel from '@clayui/panel';
import {useSessionState} from 'frontend-js-components-web';
import {sub} from 'frontend-js-web';
import React, {useMemo} from 'react';

import {IMigration} from '../../types/Migration';
import MigrationRow from './MigrationRow';

interface IProps {
	migrationIncomplete: boolean;
	migrations: IMigration[];
}

const Migration = React.memo(
	MigrationRow,
	({migration}) => migration.completed
);

export default function Migrations({migrationIncomplete, migrations}: IProps) {
	const completedMigrations = migrations.filter(
		(migration) => migration.completed
	);
	const [completedMigrationsExpanded, setCompletedMigrationsExpanded] =
		useSessionState<boolean>('cms-migration-completed-migrations-expanded');
	const incompleteMigrations = migrations.filter(
		(migration) => !migration.completed
	);

	const numberFormat = useMemo(() => {
		return new Intl.NumberFormat(Liferay.ThemeDisplay.getBCP47LanguageId());
	}, []);

	return (
		<ClayLayout.Sheet>
			<ClayLayout.SheetHeader>
				<h2 className="sheet-title">
					{Liferay.Language.get('cms-migration-migrations')}
				</h2>
			</ClayLayout.SheetHeader>

			<ClayLayout.SheetSection>
				{!migrations.length && (
					<div className="text-secondary">
						{Liferay.Language.get('cms-migration-no-migrations')}
					</div>
				)}

				{incompleteMigrations.map((migration) => (
					<Migration
						key={migration.backgroundTaskId}
						migration={migration}
						numberFormat={numberFormat}
					/>
				))}

				{!!completedMigrations.length && (
					<ClayPanel
						collapsable
						displayTitle={sub(
							Liferay.Language.get('x-y'),
							Liferay.Language.get('completed'),
							numberFormat.format(completedMigrations.length)
						)}
						displayType="secondary"
						expanded={
							completedMigrationsExpanded ?? !migrationIncomplete
						}
						onExpandedChange={setCompletedMigrationsExpanded}
						showCollapseIcon
						style={
							{
								'--panel-header-collapse-icon-right':
									'var(--spacer-3)',
							} as React.CSSProperties
						}
					>
						<ClayPanel.Body className="text-body">
							{completedMigrations.map((migration) => (
								<Migration
									key={migration.backgroundTaskId}
									migration={migration}
									numberFormat={numberFormat}
								/>
							))}
						</ClayPanel.Body>
					</ClayPanel>
				)}
			</ClayLayout.SheetSection>
		</ClayLayout.Sheet>
	);
}

