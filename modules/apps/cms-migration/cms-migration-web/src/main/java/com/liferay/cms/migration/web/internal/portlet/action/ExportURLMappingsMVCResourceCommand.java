/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.portlet.action;

import com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskExecutor;
import com.liferay.cms.migration.web.internal.constants.CMSMigrationPortletKeys;
import com.liferay.cms.migration.web.internal.migration.CMSMigrationReport;
import com.liferay.cms.migration.web.internal.migration.CMSMigrationURLMapping;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManager;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.CSVUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import java.nio.charset.StandardCharsets;

import java.util.Locale;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Exports, as a CSV file, the map from the URL of every classic content item
 * that a migration replaced to the URL of its migrated entry.
 *
 * @author Lino Alves
 */
@Component(
	property = {
		"jakarta.portlet.name=" + CMSMigrationPortletKeys.CMS_MIGRATION,
		"mvc.command.name=/cms_migration/export_url_mappings"
	},
	service = MVCResourceCommand.class
)
public class ExportURLMappingsMVCResourceCommand
	extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		PermissionChecker permissionChecker =
			themeDisplay.getPermissionChecker();

		if (!permissionChecker.isCompanyAdmin()) {
			throw new PrincipalException.MustBeCompanyAdmin(
				permissionChecker.getUserId());
		}

		BackgroundTask backgroundTask =
			_backgroundTaskManager.getBackgroundTask(
				ParamUtil.getLong(resourceRequest, "backgroundTaskId"));

		if (!Objects.equals(
				backgroundTask.getTaskExecutorClassName(),
				CMSMigrationBackgroundTaskExecutor.class.getName()) ||
			(backgroundTask.getCompanyId() != themeDisplay.getCompanyId())) {

			throw new PrincipalException();
		}

		Locale locale = themeDisplay.getLocale();

		StringBundler sb = new StringBundler();

		_appendRow(
			sb, _language.get(locale, "type"),
			_language.get(locale, "cms-migration-old-url"),
			_language.get(locale, "cms-migration-new-url"),
			_language.get(locale, "status"));

		CMSMigrationReport cmsMigrationReport = CMSMigrationReport.of(
			backgroundTask);

		for (CMSMigrationURLMapping cmsMigrationURLMapping :
				cmsMigrationReport.getCMSMigrationURLMappings()) {

			_appendRow(
				sb,
				_language.get(
					locale,
					"cms-migration-" + cmsMigrationURLMapping.getType()),
				cmsMigrationURLMapping.getOldURL(),
				cmsMigrationURLMapping.getNewURL(),
				_language.get(
					locale,
					"cms-migration-url-mapping-status-" +
						cmsMigrationURLMapping.getStatus()));
		}

		String content = sb.toString();

		PortletResponseUtil.sendFile(
			resourceRequest, resourceResponse,
			"url-mappings-" + backgroundTask.getBackgroundTaskId() + ".csv",
			content.getBytes(StandardCharsets.UTF_8),
			ContentTypes.TEXT_CSV_UTF8);
	}

	private void _appendRow(StringBundler sb, String... values) {
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				sb.append(StringPool.COMMA);
			}

			sb.append(CSVUtil.encode(values[i]));
		}

		sb.append(CharPool.NEW_LINE);
	}

	@Reference
	private BackgroundTaskManager _backgroundTaskManager;

	@Reference
	private Language _language;

}