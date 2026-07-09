/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.MigrationError;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Albert Gomes Cabral
 */
public class MigrationErrorImpl implements MigrationError, Serializable {

	public MigrationErrorImpl(
		String tableName, String rowIdentifier, String sqlState, String message,
		String suggestedSQL) {

		_tableName = tableName;
		_rowIdentifier = rowIdentifier;
		_sqlState = sqlState;
		_message = message;
		_suggestedSQL = suggestedSQL;
	}

	@Override
	public String getMessage() {
		return _message;
	}

	@Override
	public String getRowIdentifier() {
		return _rowIdentifier;
	}

	@Override
	public String getSQLState() {
		return _sqlState;
	}

	@Override
	public String getSuggestedSQL() {
		return _suggestedSQL;
	}

	@Override
	public String getTableName() {
		return _tableName;
	}

	@Serial
	private static final long serialVersionUID = 1L;

	private final String _message;
	private final String _rowIdentifier;
	private final String _sqlState;
	private final String _suggestedSQL;
	private final String _tableName;

}