/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.background.task;

import com.liferay.cms.migration.web.internal.constants.CMSMigrationConstants;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatus;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.Serializable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Lucas Emanuel
 */
public class CMSMigrationBackgroundTaskStatusMessageTranslatorTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_backgroundTaskStatus = new BackgroundTaskStatus();

		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				_addPendingPhases(
					LinkedHashMapBuilder.put(
						CMSMigrationConstants.PHASE_FOLDERS, 2L
					).build(),
					CMSMigrationConstants.PHASE_CONTENT_LINKS)
			).put(
				CMSMigrationConstants.BAR_UPDATE_PAGES,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_PAGES, 2L
				).build()
			).build());

		_translate(message);
	}

	@Test
	public void testTranslate() {
		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_FOLDERS, 2);

		_assertBar(
			0, 0, true, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 0, 2);
		_assertBar(1, 0, false, CMSMigrationConstants.BAR_UPDATE_PAGES, 0, 2);
		Assert.assertEquals(0, _getOverallProgress());

		_translateItemMessage(RandomTestUtil.randomString());

		String itemTitle = RandomTestUtil.randomString();

		_translateItemMessage(itemTitle);

		_assertBar(
			0, 2, true, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 99, 2);
		Assert.assertEquals(
			itemTitle, _backgroundTaskStatus.getAttribute("itemTitle"));
		Assert.assertEquals(50, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 1);

		Assert.assertEquals(
			"", _backgroundTaskStatus.getAttribute("itemTitle"));
		_assertBar(
			0, 2, true, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 67, 3);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(
			0, 3, true, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 100, 3);
		Assert.assertEquals(60, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_PAGES, 2);

		_assertBar(
			0, 3, false, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 100, 3);
		_assertBar(1, 0, true, CMSMigrationConstants.BAR_UPDATE_PAGES, 0, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(1, 2, true, CMSMigrationConstants.BAR_UPDATE_PAGES, 100, 2);
		Assert.assertEquals(99, _getOverallProgress());
	}

	@Test
	public void testTranslateWithOneItemLeft() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_BLOGS,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_BLOGS_ENTRIES, 1000L
				).build()
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_BLOGS,
			CMSMigrationConstants.PHASE_BLOGS_ENTRIES, 1000);

		for (int i = 0; i < 999; i++) {
			_translateItemMessage(RandomTestUtil.randomString());
		}

		_assertBar(0, 999, true, CMSMigrationConstants.BAR_BLOGS, 99, 1000);
		Assert.assertEquals(99, _getOverallProgress());

		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(0, 1000, true, CMSMigrationConstants.BAR_BLOGS, 100, 1000);
		Assert.assertEquals(99, _getOverallProgress());
	}

	@Test
	public void testTranslateWithPendingTotal() {
		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_FOLDERS, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_PAGES, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		Assert.assertEquals(99, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 4);

		Assert.assertEquals(99, _getOverallProgress());
	}

	@Test
	public void testTranslateWithoutItems() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_BLOGS,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_BLOGS_ENTRIES, 0L
				).build()
			).put(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				_addPendingPhases(
					new LinkedHashMap<>(),
					CMSMigrationConstants.PHASE_JOURNAL_ARTICLES)
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_BLOGS,
			CMSMigrationConstants.PHASE_BLOGS_ENTRIES, 0);

		_assertBar(0, 0, true, CMSMigrationConstants.BAR_BLOGS, 100, 0);
		_assertBar(1, 0, false, CMSMigrationConstants.BAR_WEB_CONTENT, 0, 0);
	}

	private LinkedHashMap<String, Long> _addPendingPhases(
		LinkedHashMap<String, Long> phaseTotals, String... phases) {

		for (String phase : phases) {
			phaseTotals.put(phase, null);
		}

		return phaseTotals;
	}

	private void _assertBar(
		int index, long completed, boolean current, String key, int progress,
		long total) {

		List<Map<String, Serializable>> bars =
			(List<Map<String, Serializable>>)_backgroundTaskStatus.getAttribute(
				"bars");

		Map<String, Serializable> bar = bars.get(index);

		Assert.assertEquals(completed, bar.get("completed"));
		Assert.assertEquals(current, bar.get("current"));
		Assert.assertEquals(key, bar.get("key"));
		Assert.assertEquals(progress, bar.get("progress"));
		Assert.assertEquals(total, bar.get("total"));
	}

	private Message _createMessage(String messageType) {
		Message message = new Message();

		message.put("messageType", messageType);

		return message;
	}

	private int _getOverallProgress() {
		return GetterUtil.getInteger(
			_backgroundTaskStatus.getAttribute("overallProgress"));
	}

	private void _translate(Message message) {
		_cmsMigrationBackgroundTaskStatusMessageTranslator.translate(
			_backgroundTaskStatus, message);
	}

	private void _translateItemMessage(String itemTitle) {
		Message message = _createMessage("item");

		message.put("itemTitle", itemTitle);

		_translate(message);
	}

	private void _translatePhaseMessage(String bar, String phase, long total) {
		Message message = _createMessage("phase");

		message.put("bar", bar);
		message.put("phase", phase);
		message.put("total", total);

		_translate(message);
	}

	private BackgroundTaskStatus _backgroundTaskStatus;
	private final CMSMigrationBackgroundTaskStatusMessageTranslator
		_cmsMigrationBackgroundTaskStatusMessageTranslator =
			new CMSMigrationBackgroundTaskStatusMessageTranslator();

}