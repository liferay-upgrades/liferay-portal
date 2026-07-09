/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.MigrationError;
import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.sql.Connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

/**
 * @author Albert Gomes Cabral
 */
public class DatabaseMigrator {

	public MigrationStatus getMigrationStatus() {
		MigrationStatus migrationStatus = _migrationStatus.get();

		if (migrationStatus == null) {
			return new MigrationStatusImpl(
				MigrationStatus.PHASE_IDLE, "No migration in progress");
		}

		return migrationStatus;
	}

	public boolean isMigrationRunning() {
		MigrationStatus migrationStatus = _migrationStatus.get();

		if (migrationStatus == null) {
			return false;
		}

		int phase = migrationStatus.getPhase();

		if ((phase != MigrationStatus.PHASE_IDLE) &&
			(phase != MigrationStatus.PHASE_COMPLETED) &&
			(phase != MigrationStatus.PHASE_ERROR)) {

			return true;
		}

		return false;
	}

	public void migrate(
			String sourceJDBCURL, String sourceUserName, String sourcePassword,
			String targetJDBCURL, String targetUserName, String targetPassword)
		throws Exception {

		MigrationStatusImpl migrationStatusImpl = new MigrationStatusImpl(
			MigrationStatus.PHASE_DISCOVERY, "Connecting to databases");

		_migrationStatus.set(migrationStatusImpl);

		DataSource sourceDataSource = null;
		DataSource targetDataSource = null;

		try {
			sourceDataSource = MigrationDataSourceFactory.initDataSource(
				sourceJDBCURL, sourceUserName, sourcePassword);
			targetDataSource = MigrationDataSourceFactory.initDataSource(
				targetJDBCURL, targetUserName, targetPassword);

			List<String> tableNames = _discoverTables(
				sourceDataSource, migrationStatusImpl);

			_createSchema(
				sourceDataSource, targetDataSource, migrationStatusImpl);

			_copyTables(
				sourceDataSource, targetDataSource, tableNames,
				migrationStatusImpl);

			migrationStatusImpl.setPhase(MigrationStatus.PHASE_COMPLETED);

			List<MigrationError> migrationErrors =
				migrationStatusImpl.getMigrationErrors();

			String message = "Migrated " + tableNames.size() + " tables";

			if (!migrationErrors.isEmpty()) {
				message += ", skipped " + migrationErrors.size() + " rows";
			}

			migrationStatusImpl.setMessage(message);
			migrationStatusImpl.setProgress(100);
		}
		catch (Exception exception) {
			migrationStatusImpl.setPhase(MigrationStatus.PHASE_ERROR);
			migrationStatusImpl.setMessage(exception.getMessage());

			_log.error("Database migration failed", exception);

			throw exception;
		}
		finally {
			MigrationDataSourceFactory.destroy(sourceDataSource);
			MigrationDataSourceFactory.destroy(targetDataSource);
		}
	}

	private void _copyTables(
			DataSource sourceDataSource, DataSource targetDataSource,
			List<String> tableNames, MigrationStatusImpl migrationStatusImpl)
		throws Exception {

		migrationStatusImpl.setPhase(MigrationStatus.PHASE_DATA_LOAD);

		TableCopier tableCopier = new TableCopier(
			sourceDataSource, targetDataSource);

		int copied = 0;

		for (String tableName : tableNames) {
			migrationStatusImpl.setMessage("Copying " + tableName);

			long rowCount = tableCopier.copyTable(
				tableName, migrationStatusImpl::addMigrationError);

			migrationStatusImpl.addTableRowCount(tableName, rowCount);

			copied++;

			migrationStatusImpl.setProgress((copied * 100) / tableNames.size());
		}
	}

	private void _createSchema(
			DataSource sourceDataSource, DataSource targetDataSource,
			MigrationStatusImpl migrationStatusImpl)
		throws Exception {

		migrationStatusImpl.setPhase(MigrationStatus.PHASE_SCHEMA_CREATION);
		migrationStatusImpl.setMessage("Creating target schema");

		SchemaCreator schemaCreator = new SchemaCreator(
			sourceDataSource, targetDataSource);

		List<String> createdTableNames = schemaCreator.create();

		String message = "Created " + createdTableNames.size() + " tables";

		migrationStatusImpl.setMessage(message);

		if (_log.isInfoEnabled()) {
			_log.info(message);
		}
	}

	private List<String> _discoverTables(
			DataSource sourceDataSource,
			MigrationStatusImpl migrationStatusImpl)
		throws Exception {

		try (Connection connection = sourceDataSource.getConnection()) {
			Set<String> tableNames = MigrationUtil.getTableNames(connection);

			migrationStatusImpl.setMessage(
				"Found " + tableNames.size() + " source tables");

			if (_log.isInfoEnabled()) {
				_log.info("Found " + tableNames.size() + " source tables");
			}

			return new ArrayList<>(tableNames);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DatabaseMigrator.class);

	private final AtomicReference<MigrationStatus> _migrationStatus =
		new AtomicReference<>();

}