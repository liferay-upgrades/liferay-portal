/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.portlet.action;

import com.liferay.database.migration.service.DatabaseMigrationManager;
import com.liferay.database.migration.web.internal.constants.DatabaseMigrationPortletKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = {
		"jakarta.portlet.name=" + DatabaseMigrationPortletKeys.DATABASE_MIGRATION,
		"mvc.command.name=/database_migration/start_migration"
	},
	service = MVCActionCommand.class
)
public class StartMigrationMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
		ActionRequest actionRequest, ActionResponse actionResponse) {

		String sourceJDBCURL = ParamUtil.getString(
			actionRequest, "sourceJDBCURL");
		String sourceUserName = ParamUtil.getString(
			actionRequest, "sourceUserName");
		String sourcePassword = ParamUtil.getString(
			actionRequest, "sourcePassword");
		String targetJDBCURL = ParamUtil.getString(
			actionRequest, "targetJDBCURL");
		String targetUserName = ParamUtil.getString(
			actionRequest, "targetUserName");
		String targetPassword = ParamUtil.getString(
			actionRequest, "targetPassword");

		if (Validator.isNull(sourceJDBCURL) ||
			Validator.isNull(sourceUserName) ||
			Validator.isNull(targetJDBCURL) ||
			Validator.isNull(targetUserName)) {

			SessionErrors.add(actionRequest, "connectionInformationRequired");

			_preserveConnectionParameters(
				actionResponse, sourceJDBCURL, sourceUserName, sourcePassword,
				targetJDBCURL, targetUserName, targetPassword);

			return;
		}

		if (!StringUtil.toLowerCase(
				targetJDBCURL
			).contains(
				"postgresql"
			)) {

			SessionErrors.add(actionRequest, "targetDatabaseMustBePostgreSQL");

			_preserveConnectionParameters(
				actionResponse, sourceJDBCURL, sourceUserName, sourcePassword,
				targetJDBCURL, targetUserName, targetPassword);

			return;
		}

		try {
			_databaseMigrationManager.startMigration(
				sourceJDBCURL, sourceUserName, sourcePassword, targetJDBCURL,
				targetUserName, targetPassword);

			SessionMessages.add(actionRequest, "migrationStarted");
		}
		catch (IllegalStateException illegalStateException) {
			if (_log.isDebugEnabled()) {
				_log.debug(illegalStateException);
			}

			SessionErrors.add(actionRequest, "migrationAlreadyRunning");

			_preserveConnectionParameters(
				actionResponse, sourceJDBCURL, sourceUserName, sourcePassword,
				targetJDBCURL, targetUserName, targetPassword);
		}
	}

	private void _preserveConnectionParameters(
		ActionResponse actionResponse, String sourceJDBCURL,
		String sourceUserName, String sourcePassword, String targetJDBCURL,
		String targetUserName, String targetPassword) {

		actionResponse.setRenderParameter("sourceJDBCURL", sourceJDBCURL);
		actionResponse.setRenderParameter("sourceUserName", sourceUserName);
		actionResponse.setRenderParameter("sourcePassword", sourcePassword);
		actionResponse.setRenderParameter("targetJDBCURL", targetJDBCURL);
		actionResponse.setRenderParameter("targetUserName", targetUserName);
		actionResponse.setRenderParameter("targetPassword", targetPassword);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		StartMigrationMVCActionCommand.class);

	@Reference
	private DatabaseMigrationManager _databaseMigrationManager;

}