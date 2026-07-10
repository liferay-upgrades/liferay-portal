/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.portlet.action;

import com.liferay.database.migration.service.DatabaseMigrationManager;
import com.liferay.database.migration.web.internal.constants.DatabaseMigrationPortletKeys;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = {
		"jakarta.portlet.name=" + DatabaseMigrationPortletKeys.DATABASE_MIGRATION,
		"mvc.command.name=/database_migration/test_connection"
	},
	service = MVCResourceCommand.class
)
public class TestConnectionMVCResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		boolean valid = true;
		String message = null;

		try {
			_databaseMigrationManager.testConnection(
				ParamUtil.getString(resourceRequest, "jdbcURL"),
				ParamUtil.getString(resourceRequest, "userName"),
				ParamUtil.getString(resourceRequest, "password"));
		}
		catch (Exception exception) {
			valid = false;
			message = exception.getMessage();
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse,
			JSONUtil.put(
				"message", message
			).put(
				"valid", valid
			));
	}

	@Reference
	private DatabaseMigrationManager _databaseMigrationManager;

}