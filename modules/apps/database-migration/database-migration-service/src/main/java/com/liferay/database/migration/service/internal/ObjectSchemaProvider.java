/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.petra.sql.dsl.DynamicObjectDefinitionTableUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

	private void _addColumnName(String tableName, String columnName) {
		if (Validator.isNull(tableName) || Validator.isNull(columnName)) {
			return;
		}

		Set<String> columnNames = _columnNames.computeIfAbsent(
			tableName, key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

		columnNames.add(columnName);
	}

	private String _getColumnDefinition(
		String tableName, String businessType, String dbColumnName,
		String dbType, boolean appendSQLColumnNull) {

		String dataType = DynamicObjectDefinitionTableUtil.getDataType(
			businessType, dbType);

		if (dataType == null) {
			return null;
		}

		_addColumnName(tableName, dbColumnName);

		if (!appendSQLColumnNull) {
			return StringBundler.concat(dbColumnName, " ", dataType);
		}

		return StringBundler.concat(
			dbColumnName, " ", dataType,
			DynamicObjectDefinitionTableUtil.getSQLColumnNull(dbType));
	}

	private String _getExtensionDBTableName(
		String dbTableName, boolean unmodifiableSystemObject, long companyId) {

		if (!unmodifiableSystemObject) {
			return dbTableName + "_x";
		}

		if (dbTableName.endsWith("_")) {
			return dbTableName + "x_" + companyId;
		}

		return dbTableName + "_x_" + companyId;
	}

	private void _index(Connection connection) {
		try {
			Set<String> tableNames = MigrationUtil.getTableNames(connection);

			if (!tableNames.contains(_TABLE_NAME_OBJECT_DEFINITION) ||
				!tableNames.contains(_TABLE_NAME_OBJECT_FIELD)) {

				return;
			}

			Map<Long, ObjectDefinitionMetadata> objectDefinitionMetadatas =
				_indexObjectDefinitions(connection);

			_indexObjectFields(connection, objectDefinitionMetadatas);

			for (ObjectDefinitionMetadata objectDefinitionMetadata :
					objectDefinitionMetadatas.values()) {

				_indexCreateTableSQL(
					objectDefinitionMetadata,
					objectDefinitionMetadata.getDBTableName());
				_indexCreateTableSQL(
					objectDefinitionMetadata,
					objectDefinitionMetadata.getExtensionDBTableName());
				_indexLocalizationCreateTableSQL(objectDefinitionMetadata);
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

	private void _indexCreateTableSQL(
		ObjectDefinitionMetadata objectDefinitionMetadata, String tableName) {

		String primaryKeyColumnName =
			objectDefinitionMetadata.getPKObjectFieldDBColumnName();

		if (Validator.isNull(primaryKeyColumnName)) {
			return;
		}

		List<String> columnDefinitions = new ArrayList<>();

		for (ObjectFieldMetadata objectFieldMetadata :
				objectDefinitionMetadata.getObjectFieldMetadata()) {

			if (objectFieldMetadata.isLocalized() ||
				!objectFieldMetadata.hasInsertValues() ||
				!StringUtil.equalsIgnoreCase(
					tableName, objectFieldMetadata.getDBTableName())) {

				continue;
			}

			for (String dbColumnName : objectFieldMetadata.getDBColumnNames()) {
				String columnDefinition = _getColumnDefinition(
					tableName, objectFieldMetadata.getBusinessType(),
					dbColumnName, objectFieldMetadata.getDBType(), true);

				if (columnDefinition == null) {
					return;
				}

				columnDefinitions.add(columnDefinition);
			}

			if (objectFieldMetadata.compareBusinessType(
					ObjectFieldConstants.BUSINESS_TYPE_AUTO_INCREMENT)) {

				String columnDefinition = _getColumnDefinition(
					tableName, objectFieldMetadata.getBusinessType(),
					objectFieldMetadata.getSortableDBColumnName(),
					ObjectFieldConstants.DB_TYPE_LONG, true);

				if (columnDefinition == null) {
					return;
				}

				columnDefinitions.add(columnDefinition);
			}
		}

		_addColumnName(tableName, primaryKeyColumnName);

		StringBundler sb = new StringBundler();

		sb.append("create table ");
		sb.append(tableName);
		sb.append(" (");
		sb.append(primaryKeyColumnName);
		sb.append(" LONG not null primary key");

		for (String columnDefinition : columnDefinitions) {
			sb.append(", ");
			sb.append(columnDefinition);
		}

		sb.append(");");

		_createTableSQLs.put(tableName, sb.toString());
	}

	private void _indexLocalizationCreateTableSQL(
		ObjectDefinitionMetadata objectDefinitionMetadata) {

		String primaryKeyColumnName =
			objectDefinitionMetadata.getPKObjectFieldDBColumnName();

		if (Validator.isNull(primaryKeyColumnName)) {
			return;
		}

		String tableName =
			objectDefinitionMetadata.getLocalizationDBTableName();

		List<String> columnDefinitions = new ArrayList<>();

		for (ObjectFieldMetadata objectFieldMetadata :
				objectDefinitionMetadata.getObjectFieldMetadata()) {

			if (!objectFieldMetadata.isLocalized()) {
				continue;
			}

			String columnDefinition = _getColumnDefinition(
				tableName, objectFieldMetadata.getBusinessType(),
				objectFieldMetadata.getDBColumnName(),
				objectFieldMetadata.getDBType(), false);

			if (columnDefinition == null) {
				return;
			}

			columnDefinitions.add(columnDefinition);
		}

		if (columnDefinitions.isEmpty()) {
			return;
		}

		_addColumnName(tableName, primaryKeyColumnName);
		_addColumnName(tableName, "languageId");

		StringBundler sb = new StringBundler();

		sb.append("create table ");
		sb.append(tableName);
		sb.append(" (");
		sb.append(primaryKeyColumnName);
		sb.append(" LONG not null, languageId VARCHAR(10) not null");

		for (String columnDefinition : columnDefinitions) {
			sb.append(", ");
			sb.append(columnDefinition);
		}

		sb.append(", primary key (");
		sb.append(primaryKeyColumnName);
		sb.append(", languageId));");

		_createTableSQLs.put(tableName, sb.toString());
	}

	private Map<Long, ObjectDefinitionMetadata> _indexObjectDefinitions(
			Connection connection)
		throws Exception {

		Map<Long, ObjectDefinitionMetadata> objectDefinitionMetadatas =
			new HashMap<>();

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

				long companyId = resultSet.getLong("companyId");

				boolean unmodifiableSystemObject = false;

				if (resultSet.getBoolean("system_") &&
					!resultSet.getBoolean("modifiable")) {

					unmodifiableSystemObject = true;
				}

				String extensionDBTableName = _getExtensionDBTableName(
					dbTableName, unmodifiableSystemObject, companyId);

				_tableNames.add(dbTableName);
				_tableNames.add(dbTableName + _LOCALIZATION_TABLE_NAME_SUFFIX);
				_tableNames.add(extensionDBTableName);

				String pkObjectFieldDBColumnName = resultSet.getString(
					"pkObjectFieldDBColumnName");

				_addColumnName(dbTableName, pkObjectFieldDBColumnName);
				_addColumnName(extensionDBTableName, pkObjectFieldDBColumnName);

				objectDefinitionMetadatas.put(
					resultSet.getLong("objectDefinitionId"),
					new ObjectDefinitionMetadata(
						dbTableName, extensionDBTableName,
						pkObjectFieldDBColumnName));
			}
		}

		return objectDefinitionMetadatas;
	}

	private void _indexObjectFields(
			Connection connection,
			Map<Long, ObjectDefinitionMetadata> objectDefinitionMetadatas)
		throws Exception {

		try (Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select objectDefinitionId, businessType, dbColumnName, " +
					"dbTableName, dbType, localized from " +
						_TABLE_NAME_OBJECT_FIELD)) {

			while (resultSet.next()) {
				String dbColumnName = resultSet.getString("dbColumnName");
				String dbTableName = resultSet.getString("dbTableName");

				_addColumnName(dbTableName, dbColumnName);

				ObjectDefinitionMetadata objectDefinitionMetadata =
					objectDefinitionMetadatas.get(
						resultSet.getLong("objectDefinitionId"));

				if (objectDefinitionMetadata == null) {
					continue;
				}

				objectDefinitionMetadata.addObjectFieldMetadata(
					new ObjectFieldMetadata(
						resultSet.getString("businessType"), dbColumnName,
						dbTableName, resultSet.getString("dbType"),
						resultSet.getBoolean("localized")));
			}
		}
	}

	private static final String _LOCALIZATION_TABLE_NAME_SUFFIX = "_l";

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

	private static class ObjectDefinitionMetadata {

		public ObjectDefinitionMetadata(
			String dbTableName, String extensionDBTableName,
			String pkObjectFieldDBColumnName) {

			_dbTableName = dbTableName;
			_extensionDBTableName = extensionDBTableName;
			_pkObjectFieldDBColumnName = pkObjectFieldDBColumnName;
		}

		public void addObjectFieldMetadata(
			ObjectFieldMetadata objectFieldMetadata) {

			_objectFieldMetadata.add(objectFieldMetadata);
		}

		public String getDBTableName() {
			return _dbTableName;
		}

		public String getExtensionDBTableName() {
			return _extensionDBTableName;
		}

		public String getLocalizationDBTableName() {
			return _dbTableName + _LOCALIZATION_TABLE_NAME_SUFFIX;
		}

		public List<ObjectFieldMetadata> getObjectFieldMetadata() {
			return _objectFieldMetadata;
		}

		public String getPKObjectFieldDBColumnName() {
			return _pkObjectFieldDBColumnName;
		}

		private final String _dbTableName;
		private final String _extensionDBTableName;
		private final List<ObjectFieldMetadata> _objectFieldMetadata =
			new ArrayList<>();
		private final String _pkObjectFieldDBColumnName;

	}

	private static class ObjectFieldMetadata {

		public ObjectFieldMetadata(
			String businessType, String dbColumnName, String dbTableName,
			String dbType, boolean localized) {

			_businessType = businessType;
			_dbColumnName = dbColumnName;
			_dbTableName = dbTableName;
			_dbType = dbType;
			_localized = localized;
		}

		public boolean compareBusinessType(String businessType) {
			return Objects.equals(_businessType, businessType);
		}

		public String getBusinessType() {
			return _businessType;
		}

		public String getDBColumnName() {
			return _dbColumnName;
		}

		public List<String> getDBColumnNames() {
			if (compareBusinessType(
					ObjectFieldConstants.BUSINESS_TYPE_ASSIGNEE)) {

				return Arrays.asList(
					"classNameId_" + _dbColumnName, "classPK_" + _dbColumnName);
			}

			return Collections.singletonList(_dbColumnName);
		}

		public String getDBTableName() {
			return _dbTableName;
		}

		public String getDBType() {
			return _dbType;
		}

		public String getSortableDBColumnName() {
			return _dbColumnName + Field.SORTABLE_FIELD_SUFFIX;
		}

		public boolean hasInsertValues() {
			if (compareBusinessType(
					ObjectFieldConstants.BUSINESS_TYPE_AGGREGATION) ||
				compareBusinessType(
					ObjectFieldConstants.BUSINESS_TYPE_FORMULA)) {

				return false;
			}

			return true;
		}

		public boolean isLocalized() {
			return _localized;
		}

		private final String _businessType;
		private final String _dbColumnName;
		private final String _dbTableName;
		private final String _dbType;
		private final boolean _localized;

	}

}