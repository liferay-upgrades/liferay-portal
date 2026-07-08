/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export const PHASE_COMPLETED = 6;

export const PHASE_DATA_LOAD = 4;

export const PHASE_DISCOVERY = 1;

export const PHASE_ERROR = -1;

export const PHASE_IDLE = 0;

export const PHASE_INDEX_CREATION = 5;

export const PHASE_SCHEMA_CREATION = 3;

export interface TableRowCount {
	rowCount: number;
	tableName: string;
}

export interface MigrationErrorItem {
	message: string;
	rowIdentifier: string;
	sqlState: string;
	suggestedSQL: string;
	tableName: string;
}

export interface Status {
	elapsedTime: number;
	errors: MigrationErrorItem[];
	message: string;
	phase: number;
	phaseLabel: string;
	progress: number;
	running: boolean;
	tableRowCounts: TableRowCount[];
}

export interface ColumnComparison {
	columnName: string;
	sourceType: string | null;
	targetType: string | null;
}

export interface TableComparison {
	columnComparisons: ColumnComparison[];
	onSource: boolean;
	onTarget: boolean;
	sourceRowCount: number;
	tableName: string;
	targetRowCount: number;
}

export interface SchemaComparison {
	available: boolean;
	tableComparisons: TableComparison[];
}

export interface Props {
	namespace: string;
	schemaComparisonURL: string;
	startMigrationURL: string;
	statusURL: string;
}
