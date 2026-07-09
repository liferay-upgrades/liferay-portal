/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.object.constants.ObjectDefinitionConstants;
import com.liferay.object.constants.ObjectEntryFolderConstants;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.field.util.ObjectFieldUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalServiceUtil;
import com.liferay.object.service.ObjectEntryLocalServiceUtil;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * @author Albert Gomes Cabral
 */
public class MigrationRunRecorder {

	public void record(
		long companyId, long userId, String migrationName, String sourceJDBCURL,
		String targetJDBCURL, MigrationStatus migrationStatus) {

		String principalName = PrincipalThreadLocal.getName();

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			PrincipalThreadLocal.setName(userId);

			ObjectDefinition objectDefinition = _getObjectDefinition(
				companyId, userId);

			long rowsCopied = 0;

			for (long rowCount :
					migrationStatus.getTableRowCounts(
					).values()) {

				rowsCopied += rowCount;
			}

			String status = "COMPLETED";

			if (migrationStatus.getPhase() == MigrationStatus.PHASE_ERROR) {
				status = "ERROR";
			}

			ObjectEntryLocalServiceUtil.addObjectEntry(
				0, userId, objectDefinition.getObjectDefinitionId(),
				ObjectEntryFolderConstants.
					PARENT_OBJECT_ENTRY_FOLDER_ID_DEFAULT,
				null,
				HashMapBuilder.<String, Serializable>put(
					"durationSeconds",
					(int)(migrationStatus.getElapsedTime() / 1000)
				).put(
					"errorCount",
					migrationStatus.getMigrationErrors(
					).size()
				).put(
					"migrationName", migrationName
				).put(
					"migrationStatus", status
				).put(
					"rowsCopied", rowsCopied
				).put(
					"sourceURL", sourceJDBCURL
				).put(
					"startedAt", new Date(migrationStatus.getStartTime())
				).put(
					"tableCount",
					migrationStatus.getTableRowCounts(
					).size()
				).put(
					"targetURL", targetJDBCURL
				).build(),
				_createServiceContext(companyId, userId));
		}
		catch (Exception exception) {
			_log.error("Unable to record the migration run", exception);
		}
		finally {
			PrincipalThreadLocal.setName(principalName);
		}
	}

	private Map<Locale, String> _createLabelMap(String value) {
		return Collections.singletonMap(LocaleUtil.getDefault(), value);
	}

	private ServiceContext _createServiceContext(long companyId, long userId) {
		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(companyId);
		serviceContext.setUserId(userId);

		return serviceContext;
	}

	private ObjectDefinition _getObjectDefinition(long companyId, long userId)
		throws Exception {

		ObjectDefinition objectDefinition =
			ObjectDefinitionLocalServiceUtil.fetchObjectDefinition(
				companyId, "C_MigrationRun");

		if (objectDefinition != null) {
			return objectDefinition;
		}

		objectDefinition =
			ObjectDefinitionLocalServiceUtil.addCustomObjectDefinition(
				null, userId, 0, null, true, false, true, false, true, false,
				false, false, false, null, _createLabelMap("Migration Run"),
				"MigrationRun", "100", null, _createLabelMap("Migration Runs"),
				true, ObjectDefinitionConstants.SCOPE_COMPANY,
				ObjectDefinitionConstants.STORAGE_TYPE_DEFAULT,
				Collections.emptyList(),
				Arrays.asList(
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_TEXT,
						ObjectFieldConstants.DB_TYPE_STRING, true, false, null,
						"Migration Name", "migrationName", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_DATE,
						ObjectFieldConstants.DB_TYPE_DATE, true, false, null,
						"Started At", "startedAt", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_TEXT,
						ObjectFieldConstants.DB_TYPE_STRING, false, false, null,
						"Source URL", "sourceURL", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_TEXT,
						ObjectFieldConstants.DB_TYPE_STRING, false, false, null,
						"Target URL", "targetURL", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_TEXT,
						ObjectFieldConstants.DB_TYPE_STRING, true, false, null,
						"Status", "migrationStatus", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_INTEGER,
						ObjectFieldConstants.DB_TYPE_INTEGER, false, false,
						null, "Table Count", "tableCount", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_LONG_INTEGER,
						ObjectFieldConstants.DB_TYPE_LONG, false, false, null,
						"Rows Copied", "rowsCopied", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_INTEGER,
						ObjectFieldConstants.DB_TYPE_INTEGER, false, false,
						null, "Error Count", "errorCount", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_INTEGER,
						ObjectFieldConstants.DB_TYPE_INTEGER, false, false,
						null, "Duration Seconds", "durationSeconds", false)),
				Collections.emptyList(),
				_createServiceContext(companyId, userId));

		return ObjectDefinitionLocalServiceUtil.publishCustomObjectDefinition(
			userId, objectDefinition.getObjectDefinitionId());
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MigrationRunRecorder.class);

}