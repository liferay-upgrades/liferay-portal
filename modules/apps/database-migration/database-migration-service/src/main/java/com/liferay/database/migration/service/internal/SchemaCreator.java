/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

/**
 * @author Albert Gomes Cabral
 */
public class SchemaCreator {

	public SchemaCreator(
		DataSource sourceDataSource, DataSource targetDataSource) {

		_sourceDataSource = sourceDataSource;
		_targetDataSource = targetDataSource;
	}

	public List<String> create() throws Exception {
		List<String> createdTableNames = new ArrayList<>();

		try (Connection sourceConnection = _sourceDataSource.getConnection();
			Connection targetConnection = _targetDataSource.getConnection()) {

			for (String tableName :
					MigrationUtil.getTableNames(sourceConnection)) {

				if (_createTable(
						sourceConnection, targetConnection, tableName)) {

					createdTableNames.add(tableName);
				}
			}
		}

		return createdTableNames;
	}

	private boolean _createTable(
			Connection sourceConnection, Connection targetConnection,
			String tableName)
		throws Exception {

		Map<String, Integer> columnTypes = MigrationUtil.getColumnTypes(
			sourceConnection, tableName);

		if (columnTypes.isEmpty()) {
			return false;
		}

		Map<String, Integer> columnSizes = _getColumnSizes(
			sourceConnection, tableName);

		StringBundler sb = new StringBundler();

		sb.append("create table ");
		sb.append(tableName);
		sb.append(" (");

		boolean first = true;

		for (Map.Entry<String, Integer> entry : columnTypes.entrySet()) {
			if (!first) {
				sb.append(", ");
			}

			first = false;

			sb.append(entry.getKey());
			sb.append(" ");
			sb.append(
				MigrationUtil.toPostgreSQLColumnType(
					entry.getValue(),
					columnSizes.getOrDefault(entry.getKey(), 0)));
		}

		List<String> primaryKeyColumnNames =
			MigrationUtil.getPrimaryKeyColumnNames(sourceConnection, tableName);

		if (!primaryKeyColumnNames.isEmpty()) {
			sb.append(", primary key (");
			sb.append(String.join(", ", primaryKeyColumnNames));
			sb.append(")");
		}

		sb.append(")");

		_runSQL(targetConnection, sb.toString());

		if (_log.isInfoEnabled()) {
			_log.info("Created table " + tableName);
		}

		return true;
	}

	private Map<String, Integer> _getColumnSizes(
			Connection connection, String tableName)
		throws Exception {

		Map<String, Integer> columnSizes = new LinkedHashMap<>();

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		try (ResultSet resultSet = databaseMetaData.getColumns(
				connection.getCatalog(), connection.getSchema(),
				MigrationUtil.normalizeName(connection, tableName), null)) {

			while (resultSet.next()) {
				columnSizes.put(
					resultSet.getString("COLUMN_NAME"),
					resultSet.getInt("COLUMN_SIZE"));
			}
		}

		return columnSizes;
	}

	private void _runSQL(Connection connection, String sql) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug(sql);
		}

		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(SchemaCreator.class);

	private final DataSource _sourceDataSource;
	private final DataSource _targetDataSource;

}