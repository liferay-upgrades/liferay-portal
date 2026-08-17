/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectField;
import com.liferay.object.petra.sql.dsl.DynamicObjectDefinitionLocalizationTable;
import com.liferay.object.petra.sql.dsl.DynamicObjectDefinitionLocalizationTableFactory;
import com.liferay.object.petra.sql.dsl.DynamicObjectDefinitionTable;
import com.liferay.object.petra.sql.dsl.DynamicObjectDefinitionTableUtil;
import com.liferay.object.service.ObjectDefinitionLocalServiceUtil;
import com.liferay.object.service.ObjectFieldLocalServiceUtil;
import com.liferay.petra.sql.dsl.Column;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public class ObjectSchemaProvider {

	public ObjectSchemaProvider(Connection connection) {
		_index(connection);
	}

	public Set<String> getColumnNames(String tableName) {
		return _columnNames.getOrDefault(tableName, Collections.emptySet());
	}

	public String getCreateTableSQL(String tableName) {
		return _createTableSQLs.get(tableName);
	}

	public boolean isObjectTable(String tableName) {
		if (_tableNames.contains(tableName)) {
			return true;
		}

		return _objectTableNamePattern.matcher(
			StringUtil.toLowerCase(tableName)
		).matches();
	}

	private Map<Long, List<ObjectField>> _getObjectFields(Connection connection)
		throws Exception {

		Map<Long, List<ObjectField>> objectFieldsMap = new HashMap<>();

		try (Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select objectDefinitionId, businessType, dbColumnName, " +
					"dbTableName, dbType, localized from " +
						_TABLE_NAME_OBJECT_FIELD)) {

			while (resultSet.next()) {
				ObjectField objectField =
					ObjectFieldLocalServiceUtil.createObjectField(0);

				objectField.setBusinessType(
					resultSet.getString("businessType"));
				objectField.setDBColumnName(
					resultSet.getString("dbColumnName"));
				objectField.setDBTableName(resultSet.getString("dbTableName"));
				objectField.setDBType(resultSet.getString("dbType"));
				objectField.setLocalized(resultSet.getBoolean("localized"));

				List<ObjectField> objectFields =
					objectFieldsMap.computeIfAbsent(
						resultSet.getLong("objectDefinitionId"),
						objectDefinitionId -> new ArrayList<>());

				objectFields.add(objectField);
			}
		}

		return objectFieldsMap;
	}

	private void _index(Connection connection) {
		try {
			Set<String> tableNames = MigrationUtil.getTableNames(connection);

			if (!tableNames.contains(_TABLE_NAME_OBJECT_DEFINITION) ||
				!tableNames.contains(_TABLE_NAME_OBJECT_FIELD)) {

				return;
			}

			Map<Long, ObjectDefinition> objectDefinitions =
				_indexObjectDefinitions(connection);

			Map<Long, List<ObjectField>> objectFieldsMap = _getObjectFields(
				connection);

			for (Map.Entry<Long, ObjectDefinition> entry :
					objectDefinitions.entrySet()) {

				ObjectDefinition objectDefinition = entry.getValue();
				List<ObjectField> objectFields = objectFieldsMap.getOrDefault(
					entry.getKey(), Collections.emptyList());

				if (!_isResolvable(objectDefinition, objectFields)) {
					continue;
				}

				_indexCreateTableSQL(false, objectDefinition, objectFields);
				_indexCreateTableSQL(true, objectDefinition, objectFields);

				_indexLocalizationCreateTableSQL(
					objectDefinition, objectFields);
			}
		}
		catch (Exception exception) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Unable to read Liferay Objects metadata; using table " +
						"name detection",
					exception);
			}
		}
	}

	private void _indexColumnNames(
		Collection<? extends Column<?, ?>> columns, String tableName) {

		Set<String> columnNames = _columnNames.computeIfAbsent(
			tableName, key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

		for (Column<?, ?> column : columns) {
			columnNames.add(column.getName());
		}
	}

	private void _indexCreateTableSQL(
		boolean extension, ObjectDefinition objectDefinition,
		List<ObjectField> objectFields) {

		DynamicObjectDefinitionTable dynamicObjectDefinitionTable =
			DynamicObjectDefinitionTableUtil.getDynamicObjectDefinitionTable(
				extension, objectDefinition, objectFields);

		_createTableSQLs.put(
			dynamicObjectDefinitionTable.getTableName(),
			dynamicObjectDefinitionTable.getCreateTableSQL());

		_indexColumnNames(
			dynamicObjectDefinitionTable.getColumns(),
			dynamicObjectDefinitionTable.getTableName());
	}

	private void _indexLocalizationCreateTableSQL(
		ObjectDefinition objectDefinition, List<ObjectField> objectFields) {

		DynamicObjectDefinitionLocalizationTable
			dynamicObjectDefinitionLocalizationTable =
				DynamicObjectDefinitionLocalizationTableFactory.create(
					objectDefinition, objectFields);

		if (dynamicObjectDefinitionLocalizationTable == null) {
			return;
		}

		_createTableSQLs.put(
			dynamicObjectDefinitionLocalizationTable.getTableName(),
			dynamicObjectDefinitionLocalizationTable.getCreateTableSQL());

		_indexColumnNames(
			dynamicObjectDefinitionLocalizationTable.getColumns(),
			dynamicObjectDefinitionLocalizationTable.getTableName());
	}

	private Map<Long, ObjectDefinition> _indexObjectDefinitions(
			Connection connection)
		throws Exception {

		Map<Long, ObjectDefinition> objectDefinitions = new HashMap<>();

		try (Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select objectDefinitionId, companyId, dbTableName, " +
					"modifiable, system_, pkObjectFieldDBColumnName from " +
						_TABLE_NAME_OBJECT_DEFINITION)) {

			while (resultSet.next()) {
				String dbTableName = resultSet.getString("dbTableName");

				if (Validator.isNull(dbTableName)) {
					continue;
				}

				ObjectDefinition objectDefinition =
					ObjectDefinitionLocalServiceUtil.createObjectDefinition(0);

				objectDefinition.setObjectDefinitionId(
					resultSet.getLong("objectDefinitionId"));
				objectDefinition.setCompanyId(resultSet.getLong("companyId"));
				objectDefinition.setDBTableName(dbTableName);
				objectDefinition.setModifiable(
					resultSet.getBoolean("modifiable"));
				objectDefinition.setPKObjectFieldDBColumnName(
					resultSet.getString("pkObjectFieldDBColumnName"));
				objectDefinition.setSystem(resultSet.getBoolean("system_"));

				_tableNames.add(dbTableName);
				_tableNames.add(objectDefinition.getExtensionDBTableName());
				_tableNames.add(objectDefinition.getLocalizationDBTableName());

				objectDefinitions.put(
					objectDefinition.getObjectDefinitionId(), objectDefinition);
			}
		}

		return objectDefinitions;
	}

	private boolean _isResolvable(
		ObjectDefinition objectDefinition, List<ObjectField> objectFields) {

		if (Validator.isNull(objectDefinition.getPKObjectFieldDBColumnName())) {
			return false;
		}

		for (ObjectField objectField : objectFields) {
			String dataType = DynamicObjectDefinitionTableUtil.getDataType(
				objectField.getBusinessType(), objectField.getDBType());

			if (dataType != null) {
				continue;
			}

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Unable to resolve the data type of column \"",
						objectField.getDBColumnName(), "\" in table \"",
						objectField.getDBTableName(),
						"\" because the source database declares an unknown ",
						"database type \"", objectField.getDBType(), "\""));
			}

			return false;
		}

		return true;
	}

	private static final String _TABLE_NAME_OBJECT_DEFINITION =
		"ObjectDefinition";

	private static final String _TABLE_NAME_OBJECT_FIELD = "ObjectField";

	private static final Log _log = LogFactoryUtil.getLog(
		ObjectSchemaProvider.class);

	private static final Pattern _objectTableNamePattern = Pattern.compile(
		"^o_[0-9]+_.*|^l_[0-9]+_.*|.*_x_[0-9]+$");

	private final Map<String, Set<String>> _columnNames = new TreeMap<>(
		String.CASE_INSENSITIVE_ORDER);
	private final Map<String, String> _createTableSQLs = new TreeMap<>(
		String.CASE_INSENSITIVE_ORDER);
	private final Set<String> _tableNames = new TreeSet<>(
		String.CASE_INSENSITIVE_ORDER);

}