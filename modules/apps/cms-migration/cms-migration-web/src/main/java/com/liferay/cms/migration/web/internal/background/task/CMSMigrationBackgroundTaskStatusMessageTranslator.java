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
import com.liferay.portal.kernel.util.Validator;

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

		if (messageType.equals("finish")) {
			_translateFinishMessage(backgroundTaskStatus, message);
		}
		else if (messageType.equals("item")) {
			_translateItemMessage(backgroundTaskStatus, message);
		}
		else if (messageType.equals("phase")) {
			_translatePhaseMessage(backgroundTaskStatus, message);
		}
		else if (messageType.equals("plan")) {
			_translatePlanMessage(backgroundTaskStatus, message);
		}
	}

	private int _getProgress(long completed, boolean started, long total) {
		int progress = 0;

		if (total > 0) {
			progress = (int)Math.round((double)completed / total * 100);
		}
		else if (started) {
			progress = 100;
		}

		if (!started || (completed < total)) {
			progress = Math.min(progress, 99);
		}

		return progress;
	}

	private synchronized void _translateFinishMessage(
		BackgroundTaskStatus backgroundTaskStatus, Message message) {

		Map<String, Map<String, Long>> counters =
			(Map<String, Map<String, Long>>)backgroundTaskStatus.getAttribute(
				"counters");
		Map<String, Map<String, Long>> plan =
			(Map<String, Map<String, Long>>)backgroundTaskStatus.getAttribute(
				"plan");

		if ((counters == null) || (plan == null)) {
			return;
		}

		String bar = message.getString("bar");

		Map<String, Long> phaseCounters = counters.get(bar);
		Map<String, Long> phaseTotals = plan.get(bar);

		if ((phaseCounters == null) || (phaseTotals == null)) {
			return;
		}

		String phase = message.getString("phase");

		phaseCounters.putIfAbsent(phase, 0L);

		phaseTotals.put(phase, phaseCounters.get(phase));

		_updateProgress(backgroundTaskStatus);
	}

	private synchronized void _translateItemMessage(
		BackgroundTaskStatus backgroundTaskStatus, Message message) {

		Map<String, Map<String, Long>> counters =
			(Map<String, Map<String, Long>>)backgroundTaskStatus.getAttribute(
				"counters");

		if (counters == null) {
			return;
		}

		String bar = message.getString("bar");
		String phase = message.getString("phase");

		if (Validator.isNull(bar)) {
			bar = GetterUtil.getString(
				backgroundTaskStatus.getAttribute("bar"));
			phase = GetterUtil.getString(
				backgroundTaskStatus.getAttribute("phase"));
		}

		backgroundTaskStatus.setAttribute("currentBar", bar);
		backgroundTaskStatus.setAttribute("currentPhase", phase);

		Map<String, Long> phaseCounters = counters.get(bar);

		if (phaseCounters != null) {
			phaseCounters.merge(phase, 1L, Long::sum);
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

		Map<String, Map<String, Long>> counters =
			(Map<String, Map<String, Long>>)backgroundTaskStatus.getAttribute(
				"counters");

		if (counters != null) {
			Map<String, Long> phaseCounters = counters.get(bar);

			if (phaseCounters != null) {
				phaseCounters.putIfAbsent(phase, 0L);
			}
		}

		backgroundTaskStatus.setAttribute("bar", bar);
		backgroundTaskStatus.setAttribute("currentBar", bar);
		backgroundTaskStatus.setAttribute("currentPhase", phase);
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

		backgroundTaskStatus.setAttribute("barsProgress", new HashMap<>());
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

		Map<String, Integer> barsProgress =
			(Map<String, Integer>)backgroundTaskStatus.getAttribute(
				"barsProgress");
		String currentBar = GetterUtil.getString(
			backgroundTaskStatus.getAttribute("currentBar"));
		String currentPhase = GetterUtil.getString(
			backgroundTaskStatus.getAttribute("currentPhase"));

		ArrayList<HashMap<String, Serializable>> bars = new ArrayList<>();

		long overallCompleted = 0;
		boolean overallStarted = true;
		long overallTotal = 0;

		for (Map.Entry<String, Map<String, Long>> entry : plan.entrySet()) {
			boolean current = currentBar.equals(entry.getKey());
			Map<String, Long> phaseCounters = counters.get(entry.getKey());
			Map<String, Long> phaseTotals = entry.getValue();

			long completed = 0;
			boolean pending = false;
			ArrayList<HashMap<String, Serializable>> phases = new ArrayList<>();
			boolean started = true;
			long total = 0;

			for (Map.Entry<String, Long> phaseEntry : phaseTotals.entrySet()) {
				if (!phaseCounters.containsKey(phaseEntry.getKey())) {
					started = false;
				}

				long phaseCompleted = GetterUtil.getLong(
					phaseCounters.get(phaseEntry.getKey()));
				Long phaseTotal = phaseEntry.getValue();

				if (phaseTotal == null) {
					pending = true;
				}
				else {
					phaseCompleted = Math.min(phaseCompleted, phaseTotal);

					completed += phaseCompleted;

					total += phaseTotal;
				}

				phases.add(
					HashMapBuilder.<String, Serializable>put(
						"completed", phaseCompleted
					).put(
						"current",
						current && currentPhase.equals(phaseEntry.getKey())
					).put(
						"key", phaseEntry.getKey()
					).put(
						"pending", phaseTotal == null
					).put(
						"total", GetterUtil.getLong(phaseTotal)
					).build());
			}

			int progress = Math.max(
				_getProgress(completed, started, total),
				barsProgress.getOrDefault(entry.getKey(), 0));

			if (!started || (completed < total)) {
				progress = Math.min(progress, 99);
			}

			barsProgress.put(entry.getKey(), progress);

			bars.add(
				HashMapBuilder.<String, Serializable>put(
					"completed", completed
				).put(
					"current", current
				).put(
					"key", entry.getKey()
				).put(
					"pending", pending
				).put(
					"phases", phases
				).put(
					"progress", progress
				).put(
					"total", total
				).build());

			overallCompleted += completed;

			if (!started) {
				overallStarted = false;
			}

			overallTotal += total;
		}

		backgroundTaskStatus.setAttribute("bars", bars);

		if (overallTotal == 0) {
			return;
		}

		int overallProgress = Math.min(
			_getProgress(overallCompleted, overallStarted, overallTotal), 99);

		backgroundTaskStatus.setAttribute(
			"overallProgress",
			Math.max(
				overallProgress,
				GetterUtil.getInteger(
					backgroundTaskStatus.getAttribute("overallProgress"))));
	}

}