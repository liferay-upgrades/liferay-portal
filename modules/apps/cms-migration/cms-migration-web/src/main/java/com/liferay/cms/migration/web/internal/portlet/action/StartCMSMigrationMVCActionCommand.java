/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.portlet.action;

import com.liferay.cms.migration.constants.CMSMigrationPortletKeys;
import com.liferay.cms.migration.manager.CMSMigrationManager;
import com.liferay.depot.model.DepotEntry;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Lino Alves
 */
@Component(
	property = {
		"jakarta.portlet.name=" + CMSMigrationPortletKeys.CMS_MIGRATION,
		"mvc.command.name=/cms_migration/start_cms_migration"
	},
	service = MVCActionCommand.class
)
public class StartCMSMigrationMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		PermissionChecker permissionChecker =
			themeDisplay.getPermissionChecker();

		if (!permissionChecker.isCompanyAdmin()) {
			throw new PrincipalException.MustBeCompanyAdmin(
				permissionChecker.getUserId());
		}

		try {
			_cmsMigrationManager.addCMSMigrationBackgroundTask(
				themeDisplay.getUserId(),
				ParamUtil.getLong(actionRequest, "depotEntryId"),
				ParamUtil.getString(actionRequest, "depotEntryName"),
				ParamUtil.getBoolean(actionRequest, "migrateBlogsEntries"),
				ParamUtil.getBoolean(actionRequest, "migrateFileEntries"),
				ParamUtil.getBoolean(actionRequest, "migrateJournalArticles"),
				ParamUtil.getBoolean(actionRequest, "rewritePages"),
				ParamUtil.getLong(actionRequest, "sourceGroupId"),
				ServiceContextFactory.getInstance(
					DepotEntry.class.getName(), actionRequest));

			SessionMessages.add(actionRequest, "migrationStarted");
		}
		catch (Exception exception) {
			SessionErrors.add(actionRequest, exception.getClass(), exception);

			hideDefaultErrorMessage(actionRequest);
		}
	}

	@Reference
	private CMSMigrationManager _cmsMigrationManager;

}