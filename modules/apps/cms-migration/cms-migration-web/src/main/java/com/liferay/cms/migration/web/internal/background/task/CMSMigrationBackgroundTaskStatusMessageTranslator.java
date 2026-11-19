/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.background.task;

import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatus;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatusMessageTranslator;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Lino Alves
 */
public class CMSMigrationBackgroundTaskStatusMessageTranslator
	implements BackgroundTaskStatusMessageTranslator {

	@Override
	public void translate(
		BackgroundTaskStatus backgroundTaskStatus, Message message) {

		String messageType = message.getString("messageType");

		if (messageType.equals("item")) {
			_translateItemMessage(backgroundTaskStatus, message);
		}
		else if (messageType.equals("phase")) {
			_translatePhaseMessage(backgroundTaskStatus, message);
		}
		else if (messageType.equals("plan")) {
			_translatePlanMessage(backgroundTaskStatus, message);
		}
	}

	private int _getProgress(long completed, boolean pending, long total) {
		int progress = 100;

		if (total > 0) {
			progress = (int)Math.round((double)completed / total * 100);
		}
		else if (pending) {
			progress = 0;
		}

		if (pending || (completed < total)) {
			progress = Math.min(progress, 99);
		}

		return progress;
	}

	private synchronized void _translateItemMessage(
		BackgroundTaskStatus backgroundTaskStatus, Message message) {

		Map<String, Map<String, Long>> counters =
			(Map<String, Map<String, Long>>)backgroundTaskStatus.getAttribute(
				"counters");

		if (counters == null) {
			return;
		}

		Map<String, Long> phaseCounters = counters.get(
			GetterUtil.getString(backgroundTaskStatus.getAttribute("bar")));

		if (phaseCounters != null) {
			phaseCounters.merge(
				GetterUtil.getString(
					backgroundTaskStatus.getAttribute("phase")),
				1L, Long::sum);
		}

		backgroundTaskStatus.setAttribute(
			"itemTitle", message.getString("itemTitle"));

		_updateProgress(backgroundTaskStatus);
	}

	private synchronized void _translatePhaseMessage(
		BackgroundTaskStatus backgroundTaskStatus, Message message) {

		String bar = message.getString("bar");
		String phase = message.getString("phase");

		Map<String, Map<String, Long>> plan =
			(Map<String, Map<String, Long>>)backgroundTaskStatus.getAttribute(
				"plan");

		if (plan != null) {
			Map<String, Long> phaseTotals = plan.get(bar);

			if (phaseTotals != null) {
				phaseTotals.put(phase, message.getLong("total"));
			}
		}

		backgroundTaskStatus.setAttribute("bar", bar);
		backgroundTaskStatus.setAttribute("itemTitle", "");
		backgroundTaskStatus.setAttribute("phase", phase);

		_updateProgress(backgroundTaskStatus);
	}

	private synchronized void _translatePlanMessage(
		BackgroundTaskStatus backgroundTaskStatus, Message message) {

		Map<String, Map<String, Long>> plan =
			(Map<String, Map<String, Long>>)message.get("plan");

		LinkedHashMap<String, Map<String, Long>> counters =
			new LinkedHashMap<>();

		LinkedHashMap<String, Map<String, Long>> planCopy =
			new LinkedHashMap<>();

		for (Map.Entry<String, Map<String, Long>> entry : plan.entrySet()) {
			counters.put(entry.getKey(), new HashMap<>());
			planCopy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
		}

		backgroundTaskStatus.setAttribute("counters", counters);
		backgroundTaskStatus.setAttribute("plan", planCopy);
	}

	private void _updateProgress(BackgroundTaskStatus backgroundTaskStatus) {
		Map<String, Map<String, Long>> plan =
			(Map<String, Map<String, Long>>)backgroundTaskStatus.getAttribute(
				"plan");

		if (plan == null) {
			return;
		}

		Map<String, Map<String, Long>> counters =
			(Map<String, Map<String, Long>>)backgroundTaskStatus.getAttribute(
				"counters");

		String currentBar = GetterUtil.getString(
			backgroundTaskStatus.getAttribute("bar"));

		ArrayList<HashMap<String, Serializable>> bars = new ArrayList<>();

		long overallCompleted = 0;
		boolean overallPending = false;
		long overallTotal = 0;

		for (Map.Entry<String, Map<String, Long>> entry : plan.entrySet()) {
			Map<String, Long> phaseCounters = counters.get(entry.getKey());

			long completed = 0;
			boolean pending = false;
			long total = 0;

			for (Map.Entry<String, Long> phaseEntry :
					entry.getValue(
					).entrySet()) {

				Long phaseTotal = phaseEntry.getValue();

				if (phaseTotal == null) {
					pending = true;

					continue;
				}

				completed += Math.min(
					GetterUtil.getLong(phaseCounters.get(phaseEntry.getKey())),
					phaseTotal);
				total += phaseTotal;
			}

			bars.add(
				HashMapBuilder.<String, Serializable>put(
					"completed", completed
				).put(
					"current", currentBar.equals(entry.getKey())
				).put(
					"key", entry.getKey()
				).put(
					"progress", _getProgress(completed, pending, total)
				).put(
					"total", total
				).build());

			overallCompleted += completed;

			if (pending) {
				overallPending = true;
			}

			overallTotal += total;
		}

		backgroundTaskStatus.setAttribute("bars", bars);

		if (overallTotal == 0) {
			return;
		}

		int overallProgress = Math.min(
			_getProgress(overallCompleted, overallPending, overallTotal), 99);

		backgroundTaskStatus.setAttribute(
			"overallProgress",
			Math.max(
				overallProgress,
				GetterUtil.getInteger(
					backgroundTaskStatus.getAttribute("overallProgress"))));
	}

}