/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
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
import java.util.Set;
import java.util.TreeSet;

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

				_createTable(
					sourceConnection, targetConnection, tableName,
					createdTableNames);
			}
		}

		return createdTableNames;
	}

	public List<String> createIndexes() throws Exception {
		List<String> createdIndexNames = new ArrayList<>();

		try (Connection sourceConnection = _sourceDataSource.getConnection();
			Connection targetConnection = _targetDataSource.getConnection()) {

			for (String tableName :
					MigrationUtil.getTableNames(sourceConnection)) {

				_createIndexes(
					sourceConnection, targetConnection, tableName,
					createdIndexNames);
			}
		}

		return createdIndexNames;
	}

	private void _createIndexes(
			Connection sourceConnection, Connection targetConnection,
			String tableName, List<String> createdIndexNames)
		throws Exception {

		List<String> primaryKeyColumnNames =
			MigrationUtil.getPrimaryKeyColumnNames(sourceConnection, tableName);

		Map<String, List<String>> indexColumnNames = new LinkedHashMap<>();
		Map<String, Boolean> uniqueIndexes = new LinkedHashMap<>();

		DB db = DBManagerUtil.getDB();

		try (ResultSet resultSet = db.getIndexResultSet(
				sourceConnection,
				MigrationUtil.normalizeName(sourceConnection, tableName),
				false)) {

			while (resultSet.next()) {
				if (resultSet.getShort("TYPE") ==
						DatabaseMetaData.tableIndexStatistic) {

					continue;
				}

				String indexName = resultSet.getString("INDEX_NAME");
				String columnName = resultSet.getString("COLUMN_NAME");

				if ((indexName == null) || (columnName == null)) {
					continue;
				}

				List<String> columnNames = indexColumnNames.computeIfAbsent(
					indexName, indexNameKey -> new ArrayList<>());

				columnNames.add(columnName);

				uniqueIndexes.put(
					indexName, !resultSet.getBoolean("NON_UNIQUE"));
			}
		}

		for (Map.Entry<String, List<String>> entry :
				indexColumnNames.entrySet()) {

			List<String> columnNames = entry.getValue();

			if (_hasSameColumns(columnNames, primaryKeyColumnNames)) {
				continue;
			}

			String indexName = entry.getKey();

			StringBundler sb = new StringBundler(9);

			sb.append("create ");

			if (uniqueIndexes.get(indexName)) {
				sb.append("unique ");
			}

			sb.append("index ");
			sb.append(indexName);
			sb.append(" on ");
			sb.append(tableName);
			sb.append(" (");
			sb.append(String.join(", ", columnNames));
			sb.append(")");

			try {
				_runSQL(targetConnection, sb.toString());

				createdIndexNames.add(indexName);

				if (_log.isInfoEnabled()) {
					_log.info("Created index " + indexName);
				}
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Unable to create index ", indexName, ": ",
							exception.getMessage()));
				}
			}
		}
	}

	private void _createTable(
			Connection sourceConnection, Connection targetConnection,
			String tableName, List<String> createdTableNames)
		throws Exception {

		Map<String, Integer> columnTypes = MigrationUtil.getColumnTypes(
			sourceConnection, tableName);

		if (columnTypes.isEmpty()) {
			return;
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

		createdTableNames.add(tableName);

		if (_log.isInfoEnabled()) {
			_log.info("Created table " + tableName);
		}
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

	private boolean _hasSameColumns(
		List<String> indexColumnNames, List<String> primaryKeyColumnNames) {

		if (indexColumnNames.size() != primaryKeyColumnNames.size()) {
			return false;
		}

		Set<String> columnNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		columnNames.addAll(indexColumnNames);

		return columnNames.containsAll(primaryKeyColumnNames);
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