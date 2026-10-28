/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.display.context;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.site.cms.site.initializer.internal.constants.CMSSiteInitializerFDSNames;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * @author Lucas Emanuel
 */
public class ViewHomeMyDraftsDisplayContext {

	public ViewHomeMyDraftsDisplayContext(
		HttpServletRequest httpServletRequest) {

		_themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);
	}

	public Map<String, Object> getProps() {
		return HashMapBuilder.<String, Object>put(
			"allSectionFDSName", CMSSiteInitializerFDSNames.ALL_SECTION
		).put(
			"editContentItemURL",
			StringBundler.concat(
				_themeDisplay.getPortalURL(), _themeDisplay.getPathMain(),
				GroupConstants.CMS_FRIENDLY_URL, "/edit_content_item")
		).put(
			"maxItems", 5
		).put(
			"redirect", _themeDisplay.getURLCurrent()
		).put(
			"userId", _themeDisplay.getUserId()
		).put(
			"userName",
			() -> {
				User user = _themeDisplay.getUser();

				return user.getFullName();
			}
		).build();
	}

	private final ThemeDisplay _themeDisplay;

}