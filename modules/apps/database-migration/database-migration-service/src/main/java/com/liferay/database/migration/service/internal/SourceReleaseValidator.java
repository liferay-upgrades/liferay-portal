/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Albert Gomes Cabral
 */
public class SourceReleaseValidator {

	public static List<String> getMismatches(
			Connection connection, Map<String, String> schemaVersions)
		throws Exception {

		Set<String> tableNames = MigrationUtil.getTableNames(connection);

		if (!tableNames.contains(_TABLE_NAME)) {
			return null;
		}

		Map<String, String> sourceSchemaVersions = null;

		try {
			sourceSchemaVersions = _getSourceSchemaVersions(connection);
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to read the source database schema versions",
					exception);
			}

			return Collections.singletonList(
				"the source database schema versions are unreadable");
		}

		List<String> mismatches = new ArrayList<>();

		for (Map.Entry<String, String> entry :
				sourceSchemaVersions.entrySet()) {

			String servletContextName = entry.getKey();
			String sourceSchemaVersion = entry.getValue();

			if (!schemaVersions.containsKey(servletContextName)) {
				mismatches.add(
					_getMismatch(
						servletContextName, sourceSchemaVersion,
						"not deployed"));

				continue;
			}

			String schemaVersion = _getSchemaVersion(
				schemaVersions.get(servletContextName));

			if (!sourceSchemaVersion.equals(schemaVersion)) {
				mismatches.add(
					_getMismatch(
						servletContextName, sourceSchemaVersion,
						schemaVersion));
			}
		}

		if (mismatches.isEmpty()) {
			return null;
		}

		return mismatches;
	}

	private static String _getMismatch(
		String servletContextName, String sourceSchemaVersion,
		String schemaVersion) {

		return StringBundler.concat(
			servletContextName, ": ", sourceSchemaVersion, " (source) vs ",
			schemaVersion, " (this installation)");
	}

	private static String _getSchemaVersion(String schemaVersion) {
		if (Validator.isNull(schemaVersion)) {
			return _UNKNOWN_SCHEMA_VERSION;
		}

		return schemaVersion;
	}

	private static Map<String, String> _getSourceSchemaVersions(
			Connection connection)
		throws Exception {

		Map<String, String> sourceSchemaVersions = new TreeMap<>();

		try (Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select servletContextName, schemaVersion from " +
					MigrationUtil.normalizeName(connection, _TABLE_NAME))) {

			while (resultSet.next()) {
				String servletContextName = resultSet.getString(
					"servletContextName");

				if (Validator.isNull(servletContextName)) {
					continue;
				}

				sourceSchemaVersions.put(
					servletContextName,
					_getSchemaVersion(resultSet.getString("schemaVersion")));
			}
		}

		return sourceSchemaVersions;
	}

	private static final String _TABLE_NAME = "Release_";

	private static final String _UNKNOWN_SCHEMA_VERSION = "unknown";

	private static final Log _log = LogFactoryUtil.getLog(
		SourceReleaseValidator.class);

}