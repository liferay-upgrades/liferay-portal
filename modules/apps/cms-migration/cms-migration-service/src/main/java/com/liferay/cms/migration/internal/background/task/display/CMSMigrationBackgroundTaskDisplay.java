/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.internal.background.task.display;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.background.task.display.BaseBackgroundTaskDisplay;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.template.TemplateResource;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Locale;
import java.util.Map;

/**
 * Reports the progress of a migration from the counters that the status
 * message translator keeps for the current phase.
 *
 * @author Mateus Xavier
 */
public class CMSMigrationBackgroundTaskDisplay
	extends BaseBackgroundTaskDisplay {

	public CMSMigrationBackgroundTaskDisplay(BackgroundTask backgroundTask) {
		super(backgroundTask);
	}

	@Override
	public int getPercentage() {
		long total = getBackgroundTaskStatusAttributeLong("total");

		if (total == 0) {
			return PERCENTAGE_MIN;
		}

		long counter = getBackgroundTaskStatusAttributeLong("counter");

		return (int)Math.min(
			PERCENTAGE_MAX,
			Math.round((double)counter / total * PERCENTAGE_MAX));
	}

	@Override
	public String renderDisplayTemplate(Locale locale) {
		String phase = getBackgroundTaskStatusAttributeString("phase");

		if (Validator.isNull(phase)) {
			return StringPool.BLANK;
		}

		return StringBundler.concat(
			LanguageUtil.get(locale, "cms-migration-" + phase),
			" <span class=\"text-secondary\">",
			HtmlUtil.escape(
				getBackgroundTaskStatusAttributeString("itemTitle")),
			"</span>");
	}

	@Override
	protected TemplateResource getTemplateResource() {
		return null;
	}

	@Override
	protected Map<String, Object> getTemplateVars() {
		return null;
	}

}