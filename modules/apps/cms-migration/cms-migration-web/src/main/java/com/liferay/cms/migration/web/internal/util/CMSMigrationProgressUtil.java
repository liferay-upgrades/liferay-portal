/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.util;

import com.liferay.cms.migration.web.internal.constants.CMSMigrationConstants;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatus;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatusRegistryUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.io.Serializable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Lucas Emanuel
 */
public class CMSMigrationProgressUtil {

	public static void putProgress(
			BackgroundTask backgroundTask, JSONObject jsonObject, Locale locale)
		throws Exception {

		BackgroundTaskStatus backgroundTaskStatus =
			BackgroundTaskStatusRegistryUtil.getBackgroundTaskStatus(
				backgroundTask.getBackgroundTaskId());

		if (backgroundTaskStatus == null) {
			jsonObject.put(
				"bars", JSONFactoryUtil.createJSONArray()
			).put(
				"itemTitle", StringPool.BLANK
			).put(
				"overallProgress", 0
			);

			return;
		}

		List<Map<String, Serializable>> bars =
			(List<Map<String, Serializable>>)backgroundTaskStatus.getAttribute(
				"bars");

		if (bars == null) {
			bars = Collections.emptyList();
		}

		jsonObject.put(
			"bars",
			JSONUtil.toJSONArray(
				bars,
				bar -> {
					String key = GetterUtil.getString(bar.get("key"));

					return JSONFactoryUtil.createJSONObject(
						bar
					).put(
						"label",
						LanguageUtil.get(
							locale, _barLanguageKeys.getOrDefault(key, key))
					).put(
						"phases",
						JSONUtil.toJSONArray(
							(List<Map<String, Serializable>>)bar.get("phases"),
							phase -> JSONFactoryUtil.createJSONObject(phase))
					);
				})
		).put(
			"itemTitle",
			GetterUtil.getString(backgroundTaskStatus.getAttribute("itemTitle"))
		).put(
			"overallProgress",
			GetterUtil.getInteger(
				backgroundTaskStatus.getAttribute("overallProgress"))
		);
	}

	private static final Map<String, String> _barLanguageKeys =
		HashMapBuilder.put(
			CMSMigrationConstants.BAR_REPEATABLE_FIELDS,
			"cms-migration-repeatable-fields-progress"
		).put(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			"cms-migration-update-pages-progress"
		).build();

}