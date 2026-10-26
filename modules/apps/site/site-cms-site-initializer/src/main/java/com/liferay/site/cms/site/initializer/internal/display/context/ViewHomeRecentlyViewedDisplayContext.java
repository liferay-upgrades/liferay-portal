/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.display.context;

import com.liferay.document.library.configuration.DLConfiguration;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.util.Map;

/**
 * @author Lucas Emanuel
 */
public class ViewHomeRecentlyViewedDisplayContext {

	public ViewHomeRecentlyViewedDisplayContext(
		DLConfiguration dlConfiguration) {

		_dlConfiguration = dlConfiguration;
	}

	public Map<String, Object> getProps() {
		return HashMapBuilder.<String, Object>put(
			"fileMimeTypeCssClasses",
			() -> {
				if (_dlConfiguration == null) {
					return null;
				}

				return SectionDisplayContextUtil.getFileMimeTypeCssClasses(
					_dlConfiguration);
			}
		).put(
			"fileMimeTypeIcons",
			() -> {
				if (_dlConfiguration == null) {
					return null;
				}

				return SectionDisplayContextUtil.getFileMimeTypeIcons(
					_dlConfiguration);
			}
		).put(
			"objectDefinitionCssClasses",
			SectionDisplayContextUtil.getObjectDefinitionCssClasses()
		).put(
			"objectDefinitionIcons",
			SectionDisplayContextUtil.getObjectDefinitionIcons()
		).build();
	}

	private final DLConfiguration _dlConfiguration;

}