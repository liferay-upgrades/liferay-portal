/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.ColumnComparison;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Albert Gomes Cabral
 */
public class ColumnComparisonImpl implements ColumnComparison, Serializable {

	public ColumnComparisonImpl(
		String columnName, String sourceType, String targetType) {

		_columnName = columnName;
		_sourceType = sourceType;
		_targetType = targetType;
	}

	@Override
	public String getColumnName() {
		return _columnName;
	}

	@Override
	public String getSourceType() {
		return _sourceType;
	}

	@Override
	public String getTargetType() {
		return _targetType;
	}

	@Serial
	private static final long serialVersionUID = 1L;

	private final String _columnName;
	private final String _sourceType;
	private final String _targetType;

}