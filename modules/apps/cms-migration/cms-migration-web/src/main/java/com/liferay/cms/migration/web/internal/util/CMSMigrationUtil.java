/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.util;

import com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskExecutor;
import com.liferay.cms.migration.web.internal.migration.CMSMigrationReport;
import com.liferay.cms.migration.web.internal.migration.CMSMigrationURLMapping;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManager;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatus;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatusRegistryUtil;
import com.liferay.portal.kernel.backgroundtask.constants.BackgroundTaskConstants;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.url.builder.PortletURLBuilder;
import com.liferay.portal.kernel.portlet.url.builder.ResourceURLBuilder;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Validator;

import java.text.Format;

import java.util.List;
import java.util.Map;

/**
 * @author Albert Gomes Cabral
 */
public class CMSMigrationUtil {

	public static JSONArray getMigrationsJSONArray(
			BackgroundTaskManager backgroundTaskManager,
			LiferayPortletResponse liferayPortletResponse, String redirect,
			ThemeDisplay themeDisplay)
		throws Exception {

		return JSONUtil.toJSONArray(
			ListUtil.sort(
				backgroundTaskManager.getBackgroundTasks(
					themeDisplay.getCompanyGroupId(),
					CMSMigrationBackgroundTaskExecutor.class.getName()),
				(backgroundTask1, backgroundTask2) -> Long.compare(
					backgroundTask2.getBackgroundTaskId(),
					backgroundTask1.getBackgroundTaskId())),
			backgroundTask -> _toJSONObject(
				backgroundTask, liferayPortletResponse, redirect,
				themeDisplay));
	}

	private static int _getProgress(BackgroundTask backgroundTask) {
		BackgroundTaskStatus backgroundTaskStatus =
			BackgroundTaskStatusRegistryUtil.getBackgroundTaskStatus(
				backgroundTask.getBackgroundTaskId());

		if (backgroundTaskStatus == null) {
			return 0;
		}

		long total = GetterUtil.getLong(
			backgroundTaskStatus.getAttribute("total"));

		if (total == 0) {
			return 0;
		}

		long counter = GetterUtil.getLong(
			backgroundTaskStatus.getAttribute("counter"));

		return (int)Math.min(100, Math.round((double)counter / total * 100));
	}

	private static String _getStatusDisplayType(int status) {
		if (status == BackgroundTaskConstants.STATUS_CANCELLED) {
			return "secondary";
		}
		else if (status ==
					BackgroundTaskConstants.STATUS_COMPLETED_WITH_ERRORS) {

			return "warning";
		}
		else if (status == BackgroundTaskConstants.STATUS_FAILED) {
			return "danger";
		}
		else if ((status == BackgroundTaskConstants.STATUS_IN_PROGRESS) ||
				 (status == BackgroundTaskConstants.STATUS_NEW) ||
				 (status == BackgroundTaskConstants.STATUS_QUEUED)) {

			return "info";
		}
		else if (status == BackgroundTaskConstants.STATUS_SUCCESSFUL) {
			return "success";
		}

		return "secondary";
	}

	private static JSONObject _getStatusJSONObject(
			BackgroundTask backgroundTask)
		throws Exception {

		BackgroundTaskStatus backgroundTaskStatus =
			BackgroundTaskStatusRegistryUtil.getBackgroundTaskStatus(
				backgroundTask.getBackgroundTaskId());

		if (backgroundTaskStatus == null) {
			return JSONFactoryUtil.createJSONObject();
		}

		return JSONFactoryUtil.createJSONObject(
			backgroundTaskStatus.getAttributes());
	}

	private static JSONObject _toJSONObject(
			BackgroundTask backgroundTask,
			LiferayPortletResponse liferayPortletResponse, String redirect,
			ThemeDisplay themeDisplay)
		throws Exception {

		CMSMigrationReport cmsMigrationReport = CMSMigrationReport.of(
			backgroundTask);

		Map<String, Long> counts = cmsMigrationReport.getCounts();

		Format dateTimeFormat = FastDateFormatFactoryUtil.getDateTime(
			themeDisplay.getLocale(), themeDisplay.getTimeZone());

		JSONObject jsonObject = JSONUtil.put(
			"backgroundTaskId", backgroundTask.getBackgroundTaskId()
		).put(
			"completed", backgroundTask.isCompleted()
		).put(
			"counts",
			JSONUtil.toJSONArray(
				counts.entrySet(),
				entry -> JSONUtil.put(
					"label",
					LanguageUtil.get(
						themeDisplay.getLocale(),
						"cms-migration-" + entry.getKey())
				).put(
					"value", entry.getValue()
				))
		).put(
			"createDate", dateTimeFormat.format(backgroundTask.getCreateDate())
		).put(
			"errors",
			JSONFactoryUtil.createJSONArray(cmsMigrationReport.getErrors())
		).put(
			"inProgress", backgroundTask.isInProgress()
		).put(
			"name", backgroundTask.getName()
		).put(
			"progress", _getProgress(backgroundTask)
		).put(
			"statusDisplayType",
			_getStatusDisplayType(backgroundTask.getStatus())
		).put(
			"statusLabel",
			LanguageUtil.get(
				themeDisplay.getLocale(), backgroundTask.getStatusLabel())
		).put(
			"warnings",
			JSONFactoryUtil.createJSONArray(cmsMigrationReport.getWarnings())
		);

		if (backgroundTask.isCompleted()) {
			jsonObject.put(
				"deleteURL",
				PortletURLBuilder.createActionURL(
					liferayPortletResponse
				).setActionName(
					"/cms_migration/delete_cms_migration"
				).setRedirect(
					redirect
				).setParameter(
					"backgroundTaskId", backgroundTask.getBackgroundTaskId()
				).buildString());
		}

		List<CMSMigrationURLMapping> cmsMigrationURLMappings =
			cmsMigrationReport.getCMSMigrationURLMappings();

		if (!cmsMigrationURLMappings.isEmpty()) {
			jsonObject.put(
				"exportURLMappingsURL",
				ResourceURLBuilder.createResourceURL(
					liferayPortletResponse
				).setParameter(
					"backgroundTaskId", backgroundTask.getBackgroundTaskId()
				).setResourceID(
					"/cms_migration/export_url_mappings"
				).buildString()
			).put(
				"urlMappings",
				JSONUtil.toJSONArray(
					ListUtil.subList(
						cmsMigrationURLMappings, 0,
						_URL_MAPPINGS_DISPLAY_LIMIT),
					cmsMigrationURLMapping -> JSONUtil.put(
						"actionRequired",
						cmsMigrationURLMapping.isActionRequired()
					).put(
						"newURL", cmsMigrationURLMapping.getNewURL()
					).put(
						"oldURL", cmsMigrationURLMapping.getOldURL()
					).put(
						"status", cmsMigrationURLMapping.getStatus()
					).put(
						"statusLabel",
						LanguageUtil.get(
							themeDisplay.getLocale(),
							"cms-migration-url-mapping-status-" +
								cmsMigrationURLMapping.getStatus())
					).put(
						"type",
						LanguageUtil.get(
							themeDisplay.getLocale(),
							"cms-migration-" + cmsMigrationURLMapping.getType())
					))
			).put(
				"urlMappingsActionRequiredCount",
				ListUtil.count(
					cmsMigrationURLMappings,
					CMSMigrationURLMapping::isActionRequired)
			).put(
				"urlMappingsCount", cmsMigrationURLMappings.size()
			);
		}

		if (backgroundTask.isInProgress()) {
			JSONObject statusJSONObject = _getStatusJSONObject(backgroundTask);

			jsonObject.put(
				"itemTitle", statusJSONObject.getString("itemTitle"));

			String phase = statusJSONObject.getString("phase");

			if (Validator.isNotNull(phase)) {
				jsonObject.put(
					"phaseLabel",
					LanguageUtil.get(
						themeDisplay.getLocale(), "cms-migration-" + phase));
			}
		}

		return jsonObject;
	}

	private static final int _URL_MAPPINGS_DISPLAY_LIMIT = 100;

}