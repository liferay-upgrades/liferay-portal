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
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_FOLDERS, 2L
				).put(
					CMSMigrationConstants.PHASE_CONTENT_LINKS, 2L
				).build()
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
			0, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 0, 4);
		_assertBar(0, false, 1, CMSMigrationConstants.BAR_UPDATE_PAGES, 0, 2);
		Assert.assertEquals(0, _getOverallProgress());

		_translateItemMessage(RandomTestUtil.randomString());

		String itemTitle = RandomTestUtil.randomString();

		_translateItemMessage(itemTitle);

		_assertBar(
			2, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 50, 4);
		Assert.assertEquals(
			itemTitle, _backgroundTaskStatus.getAttribute("itemTitle"));
		Assert.assertEquals(33, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 1);

		Assert.assertEquals(
			"", _backgroundTaskStatus.getAttribute("itemTitle"));
		_assertBar(
			2, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 67, 3);

		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(
			3, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 100, 3);
		Assert.assertEquals(60, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_PAGES, 2);

		_assertBar(
			3, false, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 100, 3);
		_assertBar(0, true, 1, CMSMigrationConstants.BAR_UPDATE_PAGES, 0, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(2, true, 1, CMSMigrationConstants.BAR_UPDATE_PAGES, 100, 2);
		Assert.assertEquals(99, _getOverallProgress());
	}

	@Test
	public void testTranslateWithEmptyBar() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_BLOGS,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_BLOGS_ENTRIES, 1L
				).build()
			).put(
				CMSMigrationConstants.BAR_OBJECTS,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_OBJECT_ENTRIES, 0L
				).build()
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_BLOGS,
			CMSMigrationConstants.PHASE_BLOGS_ENTRIES, 1);

		_assertBar(0, false, 1, CMSMigrationConstants.BAR_OBJECTS, 0, 0);

		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(1, true, 0, CMSMigrationConstants.BAR_BLOGS, 100, 1);
		_assertBar(0, false, 1, CMSMigrationConstants.BAR_OBJECTS, 0, 0);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_OBJECTS,
			CMSMigrationConstants.PHASE_OBJECT_ENTRIES, 0);

		_assertBar(0, true, 1, CMSMigrationConstants.BAR_OBJECTS, 100, 0);
	}

	@Test
	public void testTranslateWithEstimatedTotal() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_FILE_ENTRIES, 4L
				).put(
					CMSMigrationConstants.PHASE_CONTENT_LINKS, 4L
				).build()
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_FILE_ENTRIES, 4);

		for (int i = 0; i < 4; i++) {
			_translateItemMessage(RandomTestUtil.randomString());
		}

		_assertBar(
			4, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 50, 8);
		Assert.assertEquals(50, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 2);

		_assertBar(
			4, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 67, 6);
		Assert.assertEquals(67, _getOverallProgress());

		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(
			5, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 83, 6);
		Assert.assertEquals(83, _getOverallProgress());
	}

	@Test
	public void testTranslateWithFinishedPhase() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_FOLDERS, 3L
				).build()
			).put(
				CMSMigrationConstants.BAR_REPEATABLE_FIELDS,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_REPEATABLE_GROUPS, 2L
				).build()
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_FOLDERS, 3);

		_translateItemMessage(RandomTestUtil.randomString());

		_translateItemMessage(
			CMSMigrationConstants.BAR_REPEATABLE_FIELDS,
			RandomTestUtil.randomString(),
			CMSMigrationConstants.PHASE_REPEATABLE_GROUPS);

		_assertBar(
			1, false, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 33, 3);
		_assertBar(
			1, true, 1, CMSMigrationConstants.BAR_REPEATABLE_FIELDS, 50, 2);

		_translateFinishMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_FOLDERS);

		_assertBar(
			1, false, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 100, 1);

		_translateFinishMessage(
			CMSMigrationConstants.BAR_REPEATABLE_FIELDS,
			CMSMigrationConstants.PHASE_REPEATABLE_GROUPS);

		_assertBar(
			1, true, 1, CMSMigrationConstants.BAR_REPEATABLE_FIELDS, 100, 1);
	}

	@Test
	public void testTranslateWithItemOfAnotherBar() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, 2L
				).build()
			).put(
				CMSMigrationConstants.BAR_REPEATABLE_FIELDS,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_REPEATABLE_GROUPS, 1L
				).build()
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, 2);

		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(1, true, 0, CMSMigrationConstants.BAR_WEB_CONTENT, 50, 2);
		_assertBar(
			0, false, 1, CMSMigrationConstants.BAR_REPEATABLE_FIELDS, 0, 1);

		_translateItemMessage(
			CMSMigrationConstants.BAR_REPEATABLE_FIELDS,
			RandomTestUtil.randomString(),
			CMSMigrationConstants.PHASE_REPEATABLE_GROUPS);

		_assertBar(1, false, 0, CMSMigrationConstants.BAR_WEB_CONTENT, 50, 2);
		_assertBar(
			1, true, 1, CMSMigrationConstants.BAR_REPEATABLE_FIELDS, 100, 1);

		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(2, true, 0, CMSMigrationConstants.BAR_WEB_CONTENT, 100, 2);
		_assertBar(
			1, false, 1, CMSMigrationConstants.BAR_REPEATABLE_FIELDS, 100, 1);
	}

	@Test
	public void testTranslateWithItemPhase() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_CONTENT_LINKS, 1L
				).build()
			).put(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_CONTENT_LINKS, 2L
				).build()
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 1);
		_translatePhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 2);

		_translateItemMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			RandomTestUtil.randomString(),
			CMSMigrationConstants.PHASE_CONTENT_LINKS);

		_assertBar(
			0, false, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 0, 1);
		_assertBar(1, true, 1, CMSMigrationConstants.BAR_WEB_CONTENT, 50, 2);

		_translateItemMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			RandomTestUtil.randomString(),
			CMSMigrationConstants.PHASE_CONTENT_LINKS);

		_assertBar(
			1, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 100, 1);
		_assertBar(1, false, 1, CMSMigrationConstants.BAR_WEB_CONTENT, 50, 2);

		_translateItemMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			RandomTestUtil.randomString(),
			CMSMigrationConstants.PHASE_CONTENT_LINKS);

		_assertBar(2, true, 1, CMSMigrationConstants.BAR_WEB_CONTENT, 100, 2);
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

		_assertBar(999, true, 0, CMSMigrationConstants.BAR_BLOGS, 99, 1000);
		Assert.assertEquals(99, _getOverallProgress());

		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(1000, true, 0, CMSMigrationConstants.BAR_BLOGS, 100, 1000);
		Assert.assertEquals(99, _getOverallProgress());
	}

	@Test
	public void testTranslateWithPendingPhase() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				_addPendingPhases(
					LinkedHashMapBuilder.put(
						CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, 2L
					).build(),
					CMSMigrationConstants.PHASE_TEMPLATES)
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(2, true, 0, CMSMigrationConstants.BAR_WEB_CONTENT, 99, 2);
		_assertBarPending(0, true);
		_assertPhase(
			0, 2, true, 0, CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, false,
			2);
		_assertPhase(
			0, 0, false, 1, CMSMigrationConstants.PHASE_TEMPLATES, true, 0);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_TEMPLATES, 3);

		_assertBar(2, true, 0, CMSMigrationConstants.BAR_WEB_CONTENT, 99, 5);
		_assertBarPending(0, false);
		_assertPhase(
			0, 2, false, 0, CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, false,
			2);
		_assertPhase(
			0, 0, true, 1, CMSMigrationConstants.PHASE_TEMPLATES, false, 3);

		for (int i = 0; i < 3; i++) {
			_translateItemMessage(RandomTestUtil.randomString());
		}

		_assertBar(5, true, 0, CMSMigrationConstants.BAR_WEB_CONTENT, 100, 5);
	}

	@Test
	public void testTranslateWithPendingTotal() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				_addPendingPhases(
					LinkedHashMapBuilder.put(
						CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, 2L
					).put(
						CMSMigrationConstants.PHASE_CONTENT_LINKS, 2L
					).build(),
					CMSMigrationConstants.PHASE_TEMPLATES)
			).put(
				CMSMigrationConstants.BAR_UPDATE_PAGES,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_PAGES, 2L
				).build()
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		Assert.assertEquals(33, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_TEMPLATES, 2);

		Assert.assertEquals(33, _getOverallProgress());

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		Assert.assertEquals(50, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		Assert.assertEquals(75, _getOverallProgress());

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_PAGES, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		Assert.assertEquals(99, _getOverallProgress());
	}

	@Test
	public void testTranslateWithPhaseNotStarted() {
		Message message = _createMessage("plan");

		message.put(
			"plan",
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_FILE_ENTRIES, 2L
				).put(
					CMSMigrationConstants.PHASE_CONTENT_LINKS, 0L
				).build()
			).build());

		_translate(message);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_FILE_ENTRIES, 2);

		_translateItemMessage(RandomTestUtil.randomString());
		_translateItemMessage(RandomTestUtil.randomString());

		_assertBar(
			2, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 99, 2);

		_translatePhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 0);

		_assertBar(
			2, true, 0, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, 100, 2);
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

		_assertBar(0, true, 0, CMSMigrationConstants.BAR_BLOGS, 100, 0);
		_assertBar(0, false, 1, CMSMigrationConstants.BAR_WEB_CONTENT, 0, 0);
	}

	private LinkedHashMap<String, Long> _addPendingPhases(
		LinkedHashMap<String, Long> phaseTotals, String... phases) {

		for (String phase : phases) {
			phaseTotals.put(phase, null);
		}

		return phaseTotals;
	}

	private void _assertBar(
		long completed, boolean current, int index, String key, int progress,
		long total) {

		Map<String, Serializable> bar = _getBar(index);

		Assert.assertEquals(completed, bar.get("completed"));
		Assert.assertEquals(current, bar.get("current"));
		Assert.assertEquals(key, bar.get("key"));
		Assert.assertEquals(progress, bar.get("progress"));
		Assert.assertEquals(total, bar.get("total"));
	}

	private void _assertBarPending(int index, boolean pending) {
		Map<String, Serializable> bar = _getBar(index);

		Assert.assertEquals(pending, bar.get("pending"));
	}

	private void _assertPhase(
		int barIndex, long completed, boolean current, int index, String key,
		boolean pending, long total) {

		Map<String, Serializable> bar = _getBar(barIndex);

		List<Map<String, Serializable>> phases =
			(List<Map<String, Serializable>>)bar.get("phases");

		Map<String, Serializable> phase = phases.get(index);

		Assert.assertEquals(completed, phase.get("completed"));
		Assert.assertEquals(current, phase.get("current"));
		Assert.assertEquals(key, phase.get("key"));
		Assert.assertEquals(pending, phase.get("pending"));
		Assert.assertEquals(total, phase.get("total"));
	}

	private Message _createMessage(String messageType) {
		Message message = new Message();

		message.put("messageType", messageType);

		return message;
	}

	private Map<String, Serializable> _getBar(int index) {
		List<Map<String, Serializable>> bars =
			(List<Map<String, Serializable>>)_backgroundTaskStatus.getAttribute(
				"bars");

		return bars.get(index);
	}

	private int _getOverallProgress() {
		return GetterUtil.getInteger(
			_backgroundTaskStatus.getAttribute("overallProgress"));
	}

	private void _translate(Message message) {
		_cmsMigrationBackgroundTaskStatusMessageTranslator.translate(
			_backgroundTaskStatus, message);
	}

	private void _translateFinishMessage(String bar, String phase) {
		Message message = _createMessage("finish");

		message.put("bar", bar);
		message.put("phase", phase);

		_translate(message);
	}

	private void _translateItemMessage(String itemTitle) {
		Message message = _createMessage("item");

		message.put("itemTitle", itemTitle);

		_translate(message);
	}

	private void _translateItemMessage(
		String bar, String itemTitle, String phase) {

		Message message = _createMessage("item");

		message.put("bar", bar);
		message.put("itemTitle", itemTitle);
		message.put("phase", phase);

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