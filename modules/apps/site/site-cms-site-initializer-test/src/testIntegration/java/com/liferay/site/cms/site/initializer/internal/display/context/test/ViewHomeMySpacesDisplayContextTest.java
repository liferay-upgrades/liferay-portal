/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.display.context.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.depot.model.DepotEntry;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Mateus Xavier
 */
@RunWith(Arquillian.class)
public class ViewHomeMySpacesDisplayContextTest
	extends BaseDisplayContextTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Test
	public void testGetProps() throws Exception {
		mockHttpServletRequest = getMockHttpServletRequest();

		ThemeDisplay themeDisplay =
			(ThemeDisplay)mockHttpServletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		themeDisplay.setPathFriendlyURLPublic("/web");

		_fragmentRenderer.render(
			fragmentRendererContext, mockHttpServletRequest,
			new MockHttpServletResponse());

		Map<String, Object> props = ReflectionTestUtil.invoke(
			mockHttpServletRequest.getAttribute(
				"com.liferay.site.cms.site.initializer.internal.display." +
					"context.ViewHomeMySpacesDisplayContext"),
			"getProps", new Class<?>[0]);

		Assert.assertEquals("/web/cms/all-spaces", props.get("allSpacesURL"));
		Assert.assertEquals(
			"/web/cms/e/space/" + portal.getClassNameId(DepotEntry.class) + "/",
			props.get("baseSpaceURL"));
	}

	@Inject(
		filter = "component.name=com.liferay.site.cms.site.initializer.internal.fragment.renderer.ViewHomeMySpacesJSPSectionFragmentRenderer"
	)
	private FragmentRenderer _fragmentRenderer;

}