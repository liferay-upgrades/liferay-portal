/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.migration;

import com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskStatusMessageSender;
import com.liferay.petra.string.StringBundler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lucas Emanuel
 */
public class CMSMigrationPlan {

	public CMSMigrationPlan(
		CMSMigrationBackgroundTaskStatusMessageSender
			cmsMigrationBackgroundTaskStatusMessageSender) {

		_cmsMigrationBackgroundTaskStatusMessageSender =
			cmsMigrationBackgroundTaskStatusMessageSender;
	}

	public void addPendingPhase(String bar, String phase) {
		Map<String, Long> phaseTotals = _getPhaseTotals(bar);

		phaseTotals.put(phase, null);
	}

	public void addPhase(String bar, String phase, long total) {
		Map<String, Long> phaseTotals = _getPhaseTotals(bar);

		phaseTotals.put(phase, total);
	}

	public void finishPhase(String bar, String phase) {
		_cmsMigrationBackgroundTaskStatusMessageSender.sendFinishStatusMessage(
			bar, phase);
	}

	public List<String> getBars(String phase) {
		List<String> bars = new ArrayList<>();

		for (Map.Entry<String, Map<String, Long>> entry : _plan.entrySet()) {
			Map<String, Long> phaseTotals = entry.getValue();

			if (phaseTotals.containsKey(phase)) {
				bars.add(entry.getKey());
			}
		}

		return bars;
	}

	public long getPhaseTotal(String bar, String phase) {
		Map<String, Long> phaseTotals = _plan.get(bar);

		if ((phaseTotals == null) || !phaseTotals.containsKey(phase)) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"Phase \"", phase, "\" of bar \"", bar,
					"\" is not in the plan"));
		}

		Long total = phaseTotals.get(phase);

		if (total == null) {
			throw new IllegalStateException(
				StringBundler.concat(
					"Total of phase \"", phase, "\" of bar \"", bar,
					"\" is still pending"));
		}

		return total;
	}

	public void send() {
		Map<String, Map<String, Long>> plan = new LinkedHashMap<>();

		for (Map.Entry<String, Map<String, Long>> entry : _plan.entrySet()) {
			plan.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
		}

		_cmsMigrationBackgroundTaskStatusMessageSender.sendPlanStatusMessage(
			plan);
	}

	public void startPhase(String bar, String phase) {
		_cmsMigrationBackgroundTaskStatusMessageSender.sendPhaseStatusMessage(
			bar, phase, getPhaseTotal(bar, phase));
	}

	public void startPhase(String bar, String phase, long total) {
		addPhase(bar, phase, total);

		startPhase(bar, phase);
	}

	private Map<String, Long> _getPhaseTotals(String bar) {
		return _plan.computeIfAbsent(bar, key -> new LinkedHashMap<>());
	}

	private final CMSMigrationBackgroundTaskStatusMessageSender
		_cmsMigrationBackgroundTaskStatusMessageSender;
	private final Map<String, Map<String, Long>> _plan = new LinkedHashMap<>();

}