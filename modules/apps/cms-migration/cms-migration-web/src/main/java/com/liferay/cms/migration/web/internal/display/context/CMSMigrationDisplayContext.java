/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.display.context;

import com.liferay.cms.migration.manager.CMSMigrationManager;
import com.liferay.cms.migration.report.CMSMigrationReport;
import com.liferay.depot.constants.DepotConstants;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatus;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatusRegistryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.RenderRequest;
import jakarta.portlet.RenderResponse;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lino Alves
 */
public class CMSMigrationDisplayContext {

	public CMSMigrationDisplayContext(
		CMSMigrationManager cmsMigrationManager,
		DepotEntryLocalService depotEntryLocalService,
		GroupLocalService groupLocalService,
		HttpServletRequest httpServletRequest, RenderRequest renderRequest,
		RenderResponse renderResponse) {

		_cmsMigrationManager = cmsMigrationManager;
		_depotEntryLocalService = depotEntryLocalService;
		_groupLocalService = groupLocalService;
		_renderRequest = renderRequest;
		_renderResponse = renderResponse;

		_themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);
	}

	public List<BackgroundTask> getBackgroundTasks() {
		return _cmsMigrationManager.getCMSMigrationBackgroundTasks(
			_themeDisplay.getCompanyId());
	}

	public CMSMigrationReport getCMSMigrationReport(
		BackgroundTask backgroundTask) {

		return _cmsMigrationManager.getCMSMigrationReport(backgroundTask);
	}

	public List<DepotEntry> getDepotEntries() {
		return _depotEntryLocalService.getDepotEntries(
			_themeDisplay.getCompanyId(), DepotConstants.TYPE_SPACE);
	}

	public List<Group> getGroups() {
		List<Group> groups = new ArrayList<>();

		for (Group group :
				_groupLocalService.getActiveGroups(
					_themeDisplay.getCompanyId(), true, true, QueryUtil.ALL_POS,
					QueryUtil.ALL_POS, null)) {

			if (group.isDepot() || group.isStagingGroup() || !group.isSite()) {
				continue;
			}

			groups.add(group);
		}

		return groups;
	}

	public int getProgress(BackgroundTask backgroundTask) {
		BackgroundTaskStatus backgroundTaskStatus =
			BackgroundTaskStatusRegistryUtil.getBackgroundTaskStatus(
				backgroundTask.getBackgroundTaskId());

		if (backgroundTaskStatus == null) {
			return 0;
		}

		long total = GetterUtil.getLong(
			backgroundTaskStatus.getAttribute("total"));

		if (total == 0) {
			return 0;
		}

		long counter = GetterUtil.getLong(
			backgroundTaskStatus.getAttribute("counter"));

		return (int)Math.min(100, Math.round((double)counter / total * 100));
	}

	public RenderRequest getRenderRequest() {
		return _renderRequest;
	}

	public RenderResponse getRenderResponse() {
		return _renderResponse;
	}

	public JSONObject getStatusJSONObject(BackgroundTask backgroundTask)
		throws Exception {

		BackgroundTaskStatus backgroundTaskStatus =
			BackgroundTaskStatusRegistryUtil.getBackgroundTaskStatus(
				backgroundTask.getBackgroundTaskId());

		if (backgroundTaskStatus == null) {
			return JSONFactoryUtil.createJSONObject();
		}

		return JSONFactoryUtil.createJSONObject(
			backgroundTaskStatus.getAttributesJSON());
	}

	private final CMSMigrationManager _cmsMigrationManager;
	private final DepotEntryLocalService _depotEntryLocalService;
	private final GroupLocalService _groupLocalService;
	private final RenderRequest _renderRequest;
	private final RenderResponse _renderResponse;
	private final ThemeDisplay _themeDisplay;

}