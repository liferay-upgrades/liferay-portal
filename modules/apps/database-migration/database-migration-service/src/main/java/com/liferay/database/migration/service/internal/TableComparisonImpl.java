/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.ColumnComparison;
import com.liferay.database.migration.service.TableComparison;

import java.io.Serial;
import java.io.Serializable;

import java.util.List;

/**
 * @author Albert Gomes Cabral
 */
public class TableComparisonImpl implements Serializable, TableComparison {

	public TableComparisonImpl(
		String tableName, boolean onSource, boolean onTarget,
		long sourceRowCount, long targetRowCount,
		List<ColumnComparison> columnComparisons) {

		_tableName = tableName;
		_onSource = onSource;
		_onTarget = onTarget;
		_sourceRowCount = sourceRowCount;
		_targetRowCount = targetRowCount;
		_columnComparisons = columnComparisons;
	}

	@Override
	public List<ColumnComparison> getColumnComparisons() {
		return _columnComparisons;
	}

	@Override
	public long getSourceRowCount() {
		return _sourceRowCount;
	}

	@Override
	public String getTableName() {
		return _tableName;
	}

	@Override
	public long getTargetRowCount() {
		return _targetRowCount;
	}

	@Override
	public boolean isOnSource() {
		return _onSource;
	}

	@Override
	public boolean isOnTarget() {
		return _onTarget;
	}

	@Serial
	private static final long serialVersionUID = 1L;

	private final List<ColumnComparison> _columnComparisons;
	private final boolean _onSource;
	private final boolean _onTarget;
	private final long _sourceRowCount;
	private final String _tableName;
	private final long _targetRowCount;

}