/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.internal.background.task.display.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.cms.migration.constants.CMSMigrationConstants;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskExecutor;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatus;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatusRegistryUtil;
import com.liferay.portal.kernel.backgroundtask.display.BackgroundTaskDisplay;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Mateus Xavier
 */
@RunWith(Arquillian.class)
public class CMSMigrationBackgroundTaskDisplayTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() {
		_backgroundTaskId = RandomTestUtil.randomLong();

		_backgroundTask = (BackgroundTask)ProxyUtil.newProxyInstance(
			BackgroundTask.class.getClassLoader(),
			new Class<?>[] {BackgroundTask.class},
			(proxy, method, arguments) -> {
				String methodName = method.getName();

				if (methodName.equals("getBackgroundTaskId")) {
					return _backgroundTaskId;
				}

				return null;
			});
	}

	@After
	public void tearDown() {
		BackgroundTaskStatusRegistryUtil.unregisterBackgroundTaskStatus(
			_backgroundTaskId);
	}

	@Test
	public void testGetPercentage() {
		Assert.assertEquals(0, _getBackgroundTaskDisplay().getPercentage());

		BackgroundTaskStatus backgroundTaskStatus =
			BackgroundTaskStatusRegistryUtil.registerBackgroundTaskStatus(
				_backgroundTaskId, null);

		backgroundTaskStatus.setAttribute("counter", 1L);
		backgroundTaskStatus.setAttribute("total", 0L);

		Assert.assertEquals(0, _getBackgroundTaskDisplay().getPercentage());

		backgroundTaskStatus.setAttribute("total", 4L);

		Assert.assertEquals(25, _getBackgroundTaskDisplay().getPercentage());

		backgroundTaskStatus.setAttribute("counter", 5L);

		Assert.assertEquals(100, _getBackgroundTaskDisplay().getPercentage());
	}

	@Test
	public void testRenderDisplayTemplate() {
		Assert.assertEquals(
			StringPool.BLANK,
			_getBackgroundTaskDisplay().renderDisplayTemplate(LocaleUtil.US));

		BackgroundTaskStatus backgroundTaskStatus =
			BackgroundTaskStatusRegistryUtil.registerBackgroundTaskStatus(
				_backgroundTaskId, null);

		String itemTitle = "<" + RandomTestUtil.randomString() + ">";

		backgroundTaskStatus.setAttribute("itemTitle", itemTitle);

		backgroundTaskStatus.setAttribute(
			"phase", CMSMigrationConstants.PHASE_PAGES);

		Assert.assertEquals(
			StringBundler.concat(
				_language.get(
					LocaleUtil.US,
					"cms-migration-" + CMSMigrationConstants.PHASE_PAGES),
				" <span class=\"text-secondary\">", HtmlUtil.escape(itemTitle),
				"</span>"),
			_getBackgroundTaskDisplay().renderDisplayTemplate(LocaleUtil.US));
	}

	private BackgroundTaskDisplay _getBackgroundTaskDisplay() {
		return _backgroundTaskExecutor.getBackgroundTaskDisplay(
			_backgroundTask);
	}

	private BackgroundTask _backgroundTask;

	@Inject(
		filter = "background.task.executor.class.name=com.liferay.cms.migration.internal.background.task.CMSMigrationBackgroundTaskExecutor"
	)
	private BackgroundTaskExecutor _backgroundTaskExecutor;

	private long _backgroundTaskId;

	@Inject
	private Language _language;

}