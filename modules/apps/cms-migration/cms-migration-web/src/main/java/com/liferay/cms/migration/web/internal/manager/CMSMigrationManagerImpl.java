/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.manager;

import com.liferay.cms.migration.manager.CMSMigrationManager;
import com.liferay.cms.migration.report.CMSMigrationReport;
import com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskExecutor;
import com.liferay.depot.constants.DepotConstants;
import com.liferay.depot.exception.DepotEntryNameException;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManager;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.Serializable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Mateus Xavier
 */
@Component(service = CMSMigrationManager.class)
public class CMSMigrationManagerImpl implements CMSMigrationManager {

	@Override
	public BackgroundTask addCMSMigrationBackgroundTask(
			long userId, long depotEntryId, String depotEntryName,
			boolean migrateBlogsEntries, boolean migrateFileEntries,
			boolean migrateJournalArticles, boolean rewritePages,
			long sourceGroupId, ServiceContext serviceContext)
		throws PortalException {

		Group sourceGroup = _groupLocalService.getGroup(sourceGroupId);

		DepotEntry depotEntry = _getDepotEntry(
			depotEntryId, depotEntryName, serviceContext);

		Group companyGroup = _groupLocalService.getCompanyGroup(
			sourceGroup.getCompanyId());

		return _backgroundTaskManager.addBackgroundTask(
			userId, companyGroup.getGroupId(),
			sourceGroup.getDescriptiveName(serviceContext.getLocale()),
			CMSMigrationBackgroundTaskExecutor.class.getName(),
			HashMapBuilder.<String, Serializable>put(
				"depotEntryId", depotEntry.getDepotEntryId()
			).put(
				"migrateBlogsEntries", migrateBlogsEntries
			).put(
				"migrateFileEntries", migrateFileEntries
			).put(
				"migrateJournalArticles", migrateJournalArticles
			).put(
				"rewritePages", rewritePages
			).put(
				"sourceGroupId", sourceGroup.getGroupId()
			).build(),
			serviceContext);
	}

	@Override
	public void deleteCMSMigrationBackgroundTask(long backgroundTaskId)
		throws PortalException {

		BackgroundTask backgroundTask =
			_backgroundTaskManager.getBackgroundTask(backgroundTaskId);

		if (!Objects.equals(
				backgroundTask.getTaskExecutorClassName(),
				CMSMigrationBackgroundTaskExecutor.class.getName()) ||
			(backgroundTask.getCompanyId() !=
				CompanyThreadLocal.getCompanyId())) {

			throw new PrincipalException();
		}

		_backgroundTaskManager.deleteBackgroundTask(backgroundTaskId);
	}

	@Override
	public List<BackgroundTask> getCMSMigrationBackgroundTasks(long companyId) {
		Group companyGroup = _groupLocalService.fetchCompanyGroup(companyId);

		return ListUtil.sort(
			_backgroundTaskManager.getBackgroundTasks(
				companyGroup.getGroupId(),
				CMSMigrationBackgroundTaskExecutor.class.getName()),
			(backgroundTask1, backgroundTask2) -> Long.compare(
				backgroundTask2.getBackgroundTaskId(),
				backgroundTask1.getBackgroundTaskId()));
	}

	@Override
	public CMSMigrationReport getCMSMigrationReport(
		BackgroundTask backgroundTask) {

		String statusMessage = backgroundTask.getStatusMessage();

		if (Validator.isNull(statusMessage)) {
			return new CMSMigrationReport();
		}

		try {
			return CMSMigrationReport.of(
				_jsonFactory.createJSONObject(statusMessage));
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}

			CMSMigrationReport cmsMigrationReport = new CMSMigrationReport();

			cmsMigrationReport.addError(statusMessage);

			return cmsMigrationReport;
		}
	}

	private DepotEntry _getDepotEntry(
			long depotEntryId, String depotEntryName,
			ServiceContext serviceContext)
		throws PortalException {

		if (depotEntryId > 0) {
			return _depotEntryLocalService.getDepotEntry(depotEntryId);
		}

		if (Validator.isNull(depotEntryName)) {
			throw new DepotEntryNameException();
		}

		return _depotEntryLocalService.addDepotEntry(
			Collections.singletonMap(LocaleUtil.getDefault(), depotEntryName),
			Collections.singletonMap(LocaleUtil.getDefault(), StringPool.BLANK),
			DepotConstants.TYPE_SPACE, serviceContext);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CMSMigrationManagerImpl.class);

	@Reference
	private BackgroundTaskManager _backgroundTaskManager;

	@Reference
	private DepotEntryLocalService _depotEntryLocalService;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private JSONFactory _jsonFactory;

}