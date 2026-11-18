/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.migration;

import com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskStatusMessageSender;
import com.liferay.cms.migration.web.internal.constants.CMSMigrationConstants;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.ArrayList;
import java.util.Arrays;
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
public class CMSMigrationPlanTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_cmsMigrationPlan = new CMSMigrationPlan(
			new CMSMigrationBackgroundTaskStatusMessageSender(_messages::add));

		_cmsMigrationPlan.addPhase(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_FILE_ENTRIES, 4);
		_cmsMigrationPlan.addPhase(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 4);
		_cmsMigrationPlan.addPhase(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, 2);
		_cmsMigrationPlan.addPendingPhase(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_TEMPLATES);
		_cmsMigrationPlan.addPhase(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 2);
		_cmsMigrationPlan.addPhase(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_PAGES, 3);
	}

	@Test
	public void testFinishPhase() {
		_cmsMigrationPlan.finishPhase(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_TEMPLATES);

		Assert.assertEquals(_messages.toString(), 1, _messages.size());

		Message message = _messages.get(0);

		Assert.assertEquals("finish", message.getString("messageType"));
		Assert.assertEquals(
			CMSMigrationConstants.BAR_WEB_CONTENT, message.getString("bar"));
		Assert.assertEquals(
			CMSMigrationConstants.PHASE_TEMPLATES, message.getString("phase"));
	}

	@Test
	public void testGetBars() {
		Assert.assertEquals(
			Arrays.asList(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.BAR_WEB_CONTENT),
			_cmsMigrationPlan.getBars(
				CMSMigrationConstants.PHASE_CONTENT_LINKS));
		Assert.assertEquals(
			Arrays.asList(CMSMigrationConstants.BAR_UPDATE_PAGES),
			_cmsMigrationPlan.getBars(CMSMigrationConstants.PHASE_PAGES));
		Assert.assertEquals(
			Arrays.asList(),
			_cmsMigrationPlan.getBars(CMSMigrationConstants.PHASE_FOLDERS));
	}

	@Test
	public void testGetPhaseTotal() {
		Assert.assertEquals(
			4,
			_cmsMigrationPlan.getPhaseTotal(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FILE_ENTRIES));
		Assert.assertEquals(
			3,
			_cmsMigrationPlan.getPhaseTotal(
				CMSMigrationConstants.BAR_UPDATE_PAGES,
				CMSMigrationConstants.PHASE_PAGES));
	}

	@Test
	public void testGetPhaseTotalWithPendingTotal() {
		try {
			_cmsMigrationPlan.getPhaseTotal(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_TEMPLATES);

			Assert.fail();
		}
		catch (IllegalStateException illegalStateException) {
			Assert.assertEquals(
				"Total of phase \"templates\" of bar \"web-content\" is " +
					"still pending",
				illegalStateException.getMessage());
		}
	}

	@Test
	public void testSend() {
		_cmsMigrationPlan.send();

		Assert.assertEquals(_messages.toString(), 1, _messages.size());

		Message message = _messages.get(0);

		Assert.assertEquals("plan", message.getString("messageType"));

		Map<String, Map<String, Long>> plan =
			(Map<String, Map<String, Long>>)message.get("plan");

		Assert.assertEquals(
			Arrays.asList(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.BAR_UPDATE_PAGES),
			new ArrayList<>(plan.keySet()));
		Assert.assertEquals(
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.PHASE_FILE_ENTRIES, 4L
			).put(
				CMSMigrationConstants.PHASE_CONTENT_LINKS, 4L
			).build(),
			plan.get(CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA));

		Map<String, Long> phaseTotals = plan.get(
			CMSMigrationConstants.BAR_WEB_CONTENT);

		Assert.assertEquals(
			Arrays.asList(
				CMSMigrationConstants.PHASE_JOURNAL_ARTICLES,
				CMSMigrationConstants.PHASE_TEMPLATES,
				CMSMigrationConstants.PHASE_CONTENT_LINKS),
			new ArrayList<>(phaseTotals.keySet()));
		Assert.assertEquals(
			Long.valueOf(2),
			phaseTotals.get(CMSMigrationConstants.PHASE_JOURNAL_ARTICLES));
		Assert.assertNull(
			phaseTotals.get(CMSMigrationConstants.PHASE_TEMPLATES));
		Assert.assertEquals(
			Long.valueOf(2),
			phaseTotals.get(CMSMigrationConstants.PHASE_CONTENT_LINKS));
	}

	@Test
	public void testStartPhase() {
		_cmsMigrationPlan.startPhase(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
			CMSMigrationConstants.PHASE_FILE_ENTRIES);

		_assertPhaseMessage(
			CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA, _messages.get(0),
			CMSMigrationConstants.PHASE_FILE_ENTRIES, 4);

		_cmsMigrationPlan.startPhase(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_TEMPLATES, 5);

		_assertPhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT, _messages.get(1),
			CMSMigrationConstants.PHASE_TEMPLATES, 5);

		_cmsMigrationPlan.startPhase(
			CMSMigrationConstants.BAR_WEB_CONTENT,
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 1);

		_assertPhaseMessage(
			CMSMigrationConstants.BAR_WEB_CONTENT, _messages.get(2),
			CMSMigrationConstants.PHASE_CONTENT_LINKS, 1);

		_cmsMigrationPlan.send();

		Message message = _messages.get(3);

		Map<String, Map<String, Long>> plan =
			(Map<String, Map<String, Long>>)message.get("plan");

		Assert.assertEquals(
			LinkedHashMapBuilder.put(
				CMSMigrationConstants.PHASE_JOURNAL_ARTICLES, 2L
			).put(
				CMSMigrationConstants.PHASE_TEMPLATES, 5L
			).put(
				CMSMigrationConstants.PHASE_CONTENT_LINKS, 1L
			).build(),
			plan.get(CMSMigrationConstants.BAR_WEB_CONTENT));
	}

	@Test
	public void testStartPhaseWithPendingTotal() {
		try {
			_cmsMigrationPlan.startPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_TEMPLATES);

			Assert.fail();
		}
		catch (IllegalStateException illegalStateException) {
			Assert.assertEquals(
				"Total of phase \"templates\" of bar \"web-content\" is " +
					"still pending",
				illegalStateException.getMessage());
		}

		Assert.assertTrue(_messages.toString(), _messages.isEmpty());
	}

	@Test
	public void testStartPhaseWithPhaseNotInPlan() {
		try {
			_cmsMigrationPlan.startPhase(
				CMSMigrationConstants.BAR_OBJECTS,
				CMSMigrationConstants.PHASE_FOLDERS);

			Assert.fail();
		}
		catch (IllegalArgumentException illegalArgumentException) {
			Assert.assertEquals(
				"Phase \"folders\" of bar \"objects\" is not in the plan",
				illegalArgumentException.getMessage());
		}

		Assert.assertTrue(_messages.toString(), _messages.isEmpty());
	}

	private void _assertPhaseMessage(
		String bar, Message message, String phase, long total) {

		Assert.assertEquals("phase", message.getString("messageType"));
		Assert.assertEquals(bar, message.getString("bar"));
		Assert.assertEquals(phase, message.getString("phase"));
		Assert.assertEquals(total, message.getLong("total"));
	}

	private CMSMigrationPlan _cmsMigrationPlan;
	private final List<Message> _messages = new ArrayList<>();

}