/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.portlet.action;

import com.liferay.database.migration.service.DatabaseMigrationManager;
import com.liferay.database.migration.service.MigrationError;
import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.database.migration.web.internal.constants.DatabaseMigrationPortletKeys;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = {
		"jakarta.portlet.name=" + DatabaseMigrationPortletKeys.DATABASE_MIGRATION,
		"mvc.command.name=/database_migration/get_status"
	},
	service = MVCResourceCommand.class
)
public class GetStatusMVCResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		MigrationStatus migrationStatus =
			_databaseMigrationManager.getMigrationStatus();

		JSONArray tableRowCountsJSONArray = _jsonFactory.createJSONArray();

		for (Map.Entry<String, Long> entry :
				migrationStatus.getTableRowCounts(
				).entrySet()) {

			tableRowCountsJSONArray.put(
				JSONUtil.put(
					"rowCount", entry.getValue()
				).put(
					"tableName", entry.getKey()
				));
		}

		JSONArray errorsJSONArray = _jsonFactory.createJSONArray();

		for (MigrationError migrationError :
				migrationStatus.getMigrationErrors()) {

			errorsJSONArray.put(
				JSONUtil.put(
					"message", migrationError.getMessage()
				).put(
					"rowIdentifier", migrationError.getRowIdentifier()
				).put(
					"sqlState", migrationError.getSQLState()
				).put(
					"suggestedSQL", migrationError.getSuggestedSQL()
				).put(
					"tableName", migrationError.getTableName()
				));
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse,
			JSONUtil.put(
				"elapsedTime", migrationStatus.getElapsedTime()
			).put(
				"errors", errorsJSONArray
			).put(
				"message", migrationStatus.getMessage()
			).put(
				"phase", migrationStatus.getPhase()
			).put(
				"phaseLabel", migrationStatus.getPhaseLabel()
			).put(
				"progress", migrationStatus.getProgress()
			).put(
				"running", _databaseMigrationManager.isMigrationRunning()
			).put(
				"tableRowCounts", tableRowCountsJSONArray
			));
	}

	@Reference
	private DatabaseMigrationManager _databaseMigrationManager;

	@Reference
	private JSONFactory _jsonFactory;

}