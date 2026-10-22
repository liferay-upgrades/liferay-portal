/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.manager;

import com.liferay.cms.migration.report.CMSMigrationReport;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Starts, lists, and deletes the migrations of a company, so callers never
 * depend on the background task that runs them.
 *
 * @author Mateus Xavier
 */
@ProviderType
public interface CMSMigrationManager {

	/**
	 * Adds the background task that migrates the content of the source group
	 * into the depot entry. When the depot entry ID is <code>0</code>, a new
	 * depot entry with the depot entry name is added first.
	 */
	public BackgroundTask addCMSMigrationBackgroundTask(
			long userId, long depotEntryId, String depotEntryName,
			boolean migrateBlogsEntries, boolean migrateFileEntries,
			boolean migrateJournalArticles, boolean rewritePages,
			long sourceGroupId, ServiceContext serviceContext)
		throws PortalException;

	/**
	 * Deletes the record of a migration. The migrated content stays.
	 */
	public void deleteCMSMigrationBackgroundTask(long backgroundTaskId)
		throws PortalException;

	/**
	 * Returns the migrations of the company, the most recent first.
	 */
	public List<BackgroundTask> getCMSMigrationBackgroundTasks(long companyId);

	/**
	 * Returns the report that the migration stored as its status message. A
	 * status message that is not a report becomes the only error.
	 */
	public CMSMigrationReport getCMSMigrationReport(
		BackgroundTask backgroundTask);

}