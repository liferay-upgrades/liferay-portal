/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.jdbc.DataSourceFactoryUtil;

import javax.sql.DataSource;

/**
 * @author Albert Gomes Cabral
 */
public class MigrationDataSourceFactory {

	public static void destroy(DataSource dataSource) {
		if (dataSource == null) {
			return;
		}

		try {
			DataSourceFactoryUtil.destroyDataSource(dataSource);
		}
		catch (Exception exception) {
			throw new RuntimeException(
				"Unable to destroy data source", exception);
		}
	}

	public static String getDriverClassName(String jdbcURL) {
		if (jdbcURL == null) {
			return null;
		}

		if (jdbcURL.contains("db2")) {
			return "com.ibm.db2.jcc.DB2Driver";
		}

		if (jdbcURL.contains("mariadb")) {
			return "org.mariadb.jdbc.Driver";
		}

		if (jdbcURL.contains("mysql")) {
			return "com.mysql.cj.jdbc.Driver";
		}

		if (jdbcURL.contains("oracle")) {
			return "oracle.jdbc.OracleDriver";
		}

		if (jdbcURL.contains("postgresql")) {
			return "org.postgresql.Driver";
		}

		if (jdbcURL.contains("sqlserver")) {
			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		}

		return null;
	}

	public static DataSource initDataSource(
			String jdbcURL, String userName, String password)
		throws Exception {

		String driverClassName = getDriverClassName(jdbcURL);

		if (driverClassName == null) {
			throw new IllegalArgumentException(
				"Unable to determine a JDBC driver for " + jdbcURL);
		}

		return DataSourceFactoryUtil.initDataSource(
			driverClassName, jdbcURL, userName, password, StringPool.BLANK);
	}

}