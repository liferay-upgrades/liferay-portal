/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.fragment.renderer;

import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.site.cms.site.initializer.internal.display.context.ViewHomeMyDraftsDisplayContext;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;

/**
 * @author Lucas Emanuel
 */
@Component(service = FragmentRenderer.class)
public class ViewHomeMyDraftsJSPSectionFragmentRenderer
	extends BaseJSPSectionFragmentRenderer<ViewHomeMyDraftsDisplayContext> {

	@Override
	public String getLabelKey() {
		return "home-my-drafts";
	}

	@Override
	protected ViewHomeMyDraftsDisplayContext getDisplayContext(
		HttpServletRequest httpServletRequest) {

		return new ViewHomeMyDraftsDisplayContext(httpServletRequest);
	}

	@Override
	protected String getJSPPath() {
		return "/view_home_my_drafts.jsp";
	}

}