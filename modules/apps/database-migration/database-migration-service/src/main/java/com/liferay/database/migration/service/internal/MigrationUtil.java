/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.portal.kernel.util.StringUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Albert Gomes Cabral
 */
public class MigrationUtil {

	public static Map<String, String> getColumnTypeNames(
			Connection connection, String tableName)
		throws Exception {

		Map<String, String> columnTypeNames = new TreeMap<>(
			String.CASE_INSENSITIVE_ORDER);

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		try (ResultSet resultSet = databaseMetaData.getColumns(
				connection.getCatalog(), connection.getSchema(),
				normalizeName(connection, tableName), null)) {

			while (resultSet.next()) {
				columnTypeNames.put(
					resultSet.getString("COLUMN_NAME"),
					resultSet.getString("TYPE_NAME"));
			}
		}

		return columnTypeNames;
	}

	public static Map<String, Integer> getColumnTypes(
			Connection connection, String tableName)
		throws Exception {

		Map<String, Integer> columnTypes = new TreeMap<>(
			String.CASE_INSENSITIVE_ORDER);

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		try (ResultSet resultSet = databaseMetaData.getColumns(
				connection.getCatalog(), connection.getSchema(),
				normalizeName(connection, tableName), null)) {

			while (resultSet.next()) {
				columnTypes.put(
					resultSet.getString("COLUMN_NAME"),
					resultSet.getInt("DATA_TYPE"));
			}
		}

		return columnTypes;
	}

	public static List<String> getPrimaryKeyColumnNames(
			Connection connection, String tableName)
		throws Exception {

		List<String> primaryKeyColumnNames = new ArrayList<>();

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		try (ResultSet resultSet = databaseMetaData.getPrimaryKeys(
				connection.getCatalog(), connection.getSchema(),
				normalizeName(connection, tableName))) {

			while (resultSet.next()) {
				primaryKeyColumnNames.add(resultSet.getString("COLUMN_NAME"));
			}
		}

		return primaryKeyColumnNames;
	}

	public static Set<String> getTableNames(Connection connection)
		throws Exception {

		Set<String> tableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		try (ResultSet resultSet = databaseMetaData.getTables(
				connection.getCatalog(), connection.getSchema(), null,
				new String[] {"TABLE"})) {

			while (resultSet.next()) {
				tableNames.add(resultSet.getString("TABLE_NAME"));
			}
		}

		return tableNames;
	}

	public static String normalizeName(Connection connection, String name)
		throws SQLException {

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		if (databaseMetaData.storesLowerCaseIdentifiers()) {
			return StringUtil.toLowerCase(name);
		}

		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			return StringUtil.toUpperCase(name);
		}

		return name;
	}

	public static String toPostgreSQLColumnType(int sqlType, int columnSize) {
		if (((sqlType == Types.DECIMAL) || (sqlType == Types.NUMERIC)) &&
			(columnSize == 1)) {

			return "boolean";
		}

		if ((sqlType == Types.BIGINT) || (sqlType == Types.NUMERIC)) {
			return "bigint";
		}

		if ((sqlType == Types.BIT) || (sqlType == Types.BOOLEAN)) {
			return "boolean";
		}

		if ((sqlType == Types.BINARY) || (sqlType == Types.BLOB) ||
			(sqlType == Types.LONGVARBINARY) || (sqlType == Types.VARBINARY)) {

			return "bytea";
		}

		if ((sqlType == Types.CHAR) || (sqlType == Types.NCHAR) ||
			(sqlType == Types.NVARCHAR) || (sqlType == Types.VARCHAR)) {

			if ((columnSize > 0) && (columnSize <= _MAX_VARCHAR_SIZE)) {
				return "VARCHAR(" + columnSize + ")";
			}

			return "text";
		}

		if ((sqlType == Types.CLOB) || (sqlType == Types.LONGNVARCHAR) ||
			(sqlType == Types.LONGVARCHAR)) {

			return "text";
		}

		if (sqlType == Types.DECIMAL) {
			return "numeric";
		}

		if ((sqlType == Types.DOUBLE) || (sqlType == Types.FLOAT) ||
			(sqlType == Types.REAL)) {

			return "double precision";
		}

		if (sqlType == Types.INTEGER) {
			return "integer";
		}

		if ((sqlType == Types.SMALLINT) || (sqlType == Types.TINYINT)) {
			return "smallint";
		}

		if ((sqlType == Types.DATE) || (sqlType == Types.TIME) ||
			(sqlType == Types.TIMESTAMP)) {

			return "timestamp";
		}

		return "text";
	}

	private static final int _MAX_VARCHAR_SIZE = 4000;

}