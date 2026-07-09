/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.application.list;

import com.liferay.application.list.BasePanelApp;
import com.liferay.application.list.PanelApp;
import com.liferay.application.list.constants.PanelCategoryKeys;
import com.liferay.database.migration.web.internal.constants.DatabaseMigrationPortletKeys;
import com.liferay.portal.kernel.model.Portlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = {
		"panel.app.order:Integer=150",
		"panel.category.key=" + PanelCategoryKeys.CONTROL_PANEL_SYSTEM
	},
	service = PanelApp.class
)
public class DatabaseMigrationPanelApp extends BasePanelApp {

	@Override
	public String getIcon() {
		return "database";
	}

	@Override
	public Portlet getPortlet() {
		return _portlet;
	}

	@Override
	public String getPortletId() {
		return DatabaseMigrationPortletKeys.DATABASE_MIGRATION;
	}

	@Reference(
		target = "(jakarta.portlet.name=" + DatabaseMigrationPortletKeys.DATABASE_MIGRATION + ")"
	)
	private Portlet _portlet;

}