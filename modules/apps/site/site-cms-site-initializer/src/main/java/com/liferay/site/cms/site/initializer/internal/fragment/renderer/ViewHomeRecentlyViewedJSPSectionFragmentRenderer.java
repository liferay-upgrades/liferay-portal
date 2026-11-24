/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.fragment.renderer;

import com.liferay.document.library.configuration.DLConfiguration;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.site.cms.site.initializer.internal.display.context.ViewHomeRecentlyViewedDisplayContext;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/**
 * @author Lucas Emanuel
 */
@Component(
	configurationPid = "com.liferay.document.library.configuration.DLConfiguration",
	service = FragmentRenderer.class
)
public class ViewHomeRecentlyViewedJSPSectionFragmentRenderer
	extends BaseJSPSectionFragmentRenderer
		<ViewHomeRecentlyViewedDisplayContext> {

	@Override
	public String getLabelKey() {
		return "home-recently-viewed";
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_dlConfiguration = ConfigurableUtil.createConfigurable(
			DLConfiguration.class, properties);
	}

	@Override
	protected ViewHomeRecentlyViewedDisplayContext getDisplayContext(
		HttpServletRequest httpServletRequest) {

		return new ViewHomeRecentlyViewedDisplayContext(_dlConfiguration);
	}

	@Override
	protected String getJSPPath() {
		return "/view_home_recently_viewed.jsp";
	}

	private volatile DLConfiguration _dlConfiguration;

}