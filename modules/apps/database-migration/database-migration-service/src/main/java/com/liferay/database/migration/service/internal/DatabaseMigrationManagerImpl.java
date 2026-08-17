/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.DatabaseMigrationManager;
import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.database.migration.service.SourceReleaseMismatchException;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Release;
import com.liferay.portal.kernel.service.ReleaseLocalService;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(service = DatabaseMigrationManager.class)
public class DatabaseMigrationManagerImpl implements DatabaseMigrationManager {

	@Override
	public MigrationStatus getMigrationStatus() {
		return _databaseMigrator.getMigrationStatus();
	}

	@Override
	public boolean isMigrationRunning() {
		return _databaseMigrator.isMigrationRunning();
	}

	@Override
	public void startMigration(
		String sourceJDBCURL, String sourceUserName, String sourcePassword,
		String targetJDBCURL, String targetUserName, String targetPassword,
		long companyId, long userId, String migrationName) {

		if (isMigrationRunning()) {
			throw new IllegalStateException(
				"A database migration is already running");
		}

		List<String> mismatches = _getSourceReleaseMismatches(
			sourceJDBCURL, sourceUserName, sourcePassword);

		if (mismatches != null) {
			throw new SourceReleaseMismatchException(mismatches);
		}

		_executorService.submit(
			() -> {
				try {
					_databaseMigrator.migrate(
						sourceJDBCURL, sourceUserName, sourcePassword,
						targetJDBCURL, targetUserName, targetPassword,
						companyId, userId, migrationName,
						new PortableSchemaProvider(_bundleContext));
				}
				catch (Exception exception) {
					_log.error("Database migration failed", exception);
				}
			});
	}

	@Override
	public void testConnection(String jdbcURL, String userName, String password)
		throws Exception {

		DataSource dataSource = null;

		try {
			dataSource = MigrationDataSourceFactory.initDataSource(
				jdbcURL, userName, password);

			try (Connection connection = dataSource.getConnection()) {
				if (!connection.isValid(_CONNECTION_TIMEOUT)) {
					throw new SQLException("The connection is not valid");
				}
			}
		}
		finally {
			MigrationDataSourceFactory.destroy(dataSource);
		}
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;

		ThreadFactory threadFactory = runnable -> {
			Thread thread = new Thread(runnable, "Liferay Database Migration");

			thread.setDaemon(true);

			return thread;
		};

		_executorService = Executors.newSingleThreadExecutor(threadFactory);
	}

	@Deactivate
	protected void deactivate() {
		if (_executorService != null) {
			_executorService.shutdownNow();
		}
	}

	private Map<String, String> _getSchemaVersions() {
		Map<String, String> schemaVersions = new TreeMap<>();

		for (Release release :
				_releaseLocalService.getReleases(
					QueryUtil.ALL_POS, QueryUtil.ALL_POS)) {

			schemaVersions.put(
				release.getServletContextName(), release.getSchemaVersion());
		}

		return schemaVersions;
	}

	private List<String> _getSourceReleaseMismatches(
		String jdbcURL, String userName, String password) {

		if (_isCurrentConnection(jdbcURL)) {
			return null;
		}

		DataSource dataSource = null;

		try {
			dataSource = MigrationDataSourceFactory.initDataSource(
				jdbcURL, userName, password);

			try (Connection connection = dataSource.getConnection()) {
				return SourceReleaseValidator.getMismatches(
					connection, _getSchemaVersions());
			}
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to compare the source database schema versions",
					exception);
			}

			return null;
		}
		finally {
			MigrationDataSourceFactory.destroy(dataSource);
		}
	}

	private boolean _isCurrentConnection(String jdbcURL) {
		if (Validator.isNull(PropsValues.JDBC_DEFAULT_URL)) {
			return false;
		}

		return StringUtil.equals(jdbcURL, PropsValues.JDBC_DEFAULT_URL);
	}

	private static final int _CONNECTION_TIMEOUT = 10;

	private static final Log _log = LogFactoryUtil.getLog(
		DatabaseMigrationManagerImpl.class);

	private BundleContext _bundleContext;
	private final DatabaseMigrator _databaseMigrator = new DatabaseMigrator();
	private ExecutorService _executorService;

	@Reference
	private ReleaseLocalService _releaseLocalService;

}