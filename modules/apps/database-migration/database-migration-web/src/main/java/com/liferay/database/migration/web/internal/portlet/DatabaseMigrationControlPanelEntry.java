/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.portlet;

import com.liferay.database.migration.web.internal.constants.DatabaseMigrationPortletKeys;
import com.liferay.portal.kernel.portlet.ControlPanelEntry;
import com.liferay.portal.kernel.portlet.OmniadminControlPanelEntry;

import org.osgi.service.component.annotations.Component;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = "jakarta.portlet.name=" + DatabaseMigrationPortletKeys.DATABASE_MIGRATION,
	service = ControlPanelEntry.class
)
public class DatabaseMigrationControlPanelEntry
	extends OmniadminControlPanelEntry {
}