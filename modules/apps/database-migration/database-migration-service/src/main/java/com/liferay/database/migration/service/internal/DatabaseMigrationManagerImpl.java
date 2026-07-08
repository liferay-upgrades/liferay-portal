/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.DatabaseMigrationManager;
import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

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
		String targetJDBCURL, String targetUserName, String targetPassword) {

		if (isMigrationRunning()) {
			throw new IllegalStateException(
				"A database migration is already running");
		}

		_executorService.submit(
			() -> {
				try {
					_databaseMigrator.migrate(
						sourceJDBCURL, sourceUserName, sourcePassword,
						targetJDBCURL, targetUserName, targetPassword);
				}
				catch (Exception exception) {
					_log.error("Database migration failed", exception);
				}
			});
	}

	@Activate
	protected void activate() {
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

	private static final Log _log = LogFactoryUtil.getLog(
		DatabaseMigrationManagerImpl.class);

	private final DatabaseMigrator _databaseMigrator = new DatabaseMigrator();
	private ExecutorService _executorService;

}