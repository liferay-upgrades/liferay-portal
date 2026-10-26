/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.display.context.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Álvaro Leal
 */
@RunWith(Arquillian.class)
@Sync
public class ViewRecentlyCreatedSectionDisplayContextTest
	extends BaseDisplayContextTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Test
	public void testGetAPIURL() throws Exception {
		Object displayContext = _getDisplayContext(
			_viewRecentlyCreatedJSPSectionFragmentRenderer,
			_CLASS_NAME_VIEW_RECENTLY_CREATED_SECTION_DISPLAY_CONTEXT);

		Assert.assertEquals(
			"/o/search/v1.0/search",
			ReflectionTestUtil.invoke(
				displayContext, "getAPIURL", new Class<?>[0]));

		Map<String, Object> additionalProps = ReflectionTestUtil.invoke(
			displayContext, "getAdditionalProps", new Class<?>[0]);

		String additionalAPIURLParameters = (String)additionalProps.get(
			"additionalAPIURLParameters");

		Assert.assertTrue(
			additionalAPIURLParameters,
			additionalAPIURLParameters.startsWith("emptySearch=true&filter="));
		Assert.assertTrue(
			additionalAPIURLParameters,
			additionalAPIURLParameters.endsWith("&sort=dateCreated:desc"));
	}

	@Test
	public void testGetAdditionalAPIURLParameters() throws Exception {
		Instant startInstant = Instant.now();

		String additionalAPIURLParameters = ReflectionTestUtil.invoke(
			_getDisplayContext(
				_viewRecentlyCreatedJSPSectionFragmentRenderer,
				_CLASS_NAME_VIEW_RECENTLY_CREATED_SECTION_DISPLAY_CONTEXT),
			"getAdditionalAPIURLParameters", new Class<?>[0]);

		Instant endInstant = Instant.now();

		Assert.assertTrue(
			additionalAPIURLParameters,
			additionalAPIURLParameters.endsWith("&sort=dateCreated:desc"));

		Matcher matcher = _dateCreatedPattern.matcher(
			additionalAPIURLParameters);

		Assert.assertTrue(additionalAPIURLParameters, matcher.find());

		Instant dateCreatedInstant = Instant.parse(matcher.group(1));

		Assert.assertFalse(
			additionalAPIURLParameters,
			dateCreatedInstant.isAfter(endInstant.minus(7, ChronoUnit.DAYS)));
		Assert.assertFalse(
			additionalAPIURLParameters,
			dateCreatedInstant.isBefore(
				startInstant.minus(
					7, ChronoUnit.DAYS
				).truncatedTo(
					ChronoUnit.SECONDS
				)));
	}

	@Test
	public void testGetBulkActionDropdownItems() throws Exception {
		Assert.assertEquals(
			ReflectionTestUtil.<List<Object>>invoke(
				_getDisplayContext(
					_viewAllJSPSectionFragmentRenderer,
					_CLASS_NAME_VIEW_ALL_SECTION_DISPLAY_CONTEXT),
				"getBulkActionDropdownItems", new Class<?>[0]),
			ReflectionTestUtil.<List<Object>>invoke(
				_getDisplayContext(
					_viewRecentlyCreatedJSPSectionFragmentRenderer,
					_CLASS_NAME_VIEW_RECENTLY_CREATED_SECTION_DISPLAY_CONTEXT),
				"getBulkActionDropdownItems", new Class<?>[0]));
	}

	@Test
	public void testGetEmptyState() throws Exception {
		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"description", "There are no assets created in the last 7 days."
			).put(
				"image", "/states/cms_empty_state.svg"
			).put(
				"title", "No Recently Created Assets"
			).build(),
			ReflectionTestUtil.<Map<String, Object>>invoke(
				_getDisplayContext(
					_viewRecentlyCreatedJSPSectionFragmentRenderer,
					_CLASS_NAME_VIEW_RECENTLY_CREATED_SECTION_DISPLAY_CONTEXT),
				"getEmptyState", new Class<?>[0]));
	}

	@Test
	public void testGetFDSActionDropdownItems() throws Exception {
		Assert.assertEquals(
			ReflectionTestUtil.<List<Object>>invoke(
				_getDisplayContext(
					_viewAllJSPSectionFragmentRenderer,
					_CLASS_NAME_VIEW_ALL_SECTION_DISPLAY_CONTEXT),
				"getFDSActionDropdownItems", new Class<?>[0]),
			ReflectionTestUtil.<List<Object>>invoke(
				_getDisplayContext(
					_viewRecentlyCreatedJSPSectionFragmentRenderer,
					_CLASS_NAME_VIEW_RECENTLY_CREATED_SECTION_DISPLAY_CONTEXT),
				"getFDSActionDropdownItems", new Class<?>[0]));
	}

	private Object _getDisplayContext(
			FragmentRenderer fragmentRenderer, String className)
		throws Exception {

		fragmentRenderer.render(
			fragmentRendererContext, mockHttpServletRequest,
			new MockHttpServletResponse());

		Object displayContext = mockHttpServletRequest.getAttribute(className);

		Assert.assertNotNull(displayContext);

		return displayContext;
	}

	private static final String _CLASS_NAME_VIEW_ALL_SECTION_DISPLAY_CONTEXT =
		"com.liferay.site.cms.site.initializer.internal.display.context." +
			"ViewAllSectionDisplayContext";

	private static final String
		_CLASS_NAME_VIEW_RECENTLY_CREATED_SECTION_DISPLAY_CONTEXT =
			"com.liferay.site.cms.site.initializer.internal.display.context." +
				"ViewRecentlyCreatedSectionDisplayContext";

	private static final Pattern _dateCreatedPattern = Pattern.compile(
		"dateCreated ge (\\S+)");

	@Inject(
		filter = "component.name=com.liferay.site.cms.site.initializer.internal.fragment.renderer.ViewAllJSPSectionFragmentRenderer"
	)
	private FragmentRenderer _viewAllJSPSectionFragmentRenderer;

	@Inject(
		filter = "component.name=com.liferay.site.cms.site.initializer.internal.fragment.renderer.ViewRecentlyCreatedJSPSectionFragmentRenderer"
	)
	private FragmentRenderer _viewRecentlyCreatedJSPSectionFragmentRenderer;

}