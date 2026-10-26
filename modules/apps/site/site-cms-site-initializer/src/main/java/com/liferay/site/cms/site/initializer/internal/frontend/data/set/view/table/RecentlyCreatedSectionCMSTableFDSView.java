/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.frontend.data.set.view.table;

import com.liferay.frontend.data.set.view.FDSView;
import com.liferay.frontend.data.set.view.table.FDSTableSchema;
import com.liferay.frontend.data.set.view.table.FDSTableSchemaBuilder;
import com.liferay.site.cms.site.initializer.internal.constants.CMSSiteInitializerFDSNames;

import java.util.Locale;

import org.osgi.service.component.annotations.Component;

/**
 * @author Álvaro Leal
 */
@Component(
	property = "frontend.data.set.name=" + CMSSiteInitializerFDSNames.RECENTLY_CREATED_SECTION,
	service = FDSView.class
)
public class RecentlyCreatedSectionCMSTableFDSView
	extends BaseContentsSectionCMSTableFDSView {

	@Override
	public FDSTableSchema getFDSTableSchema(Locale locale) {
		FDSTableSchemaBuilder fdsTableSchemaBuilder =
			fdsTableSchemaBuilderFactory.create();

		return fdsTableSchemaBuilder.add(
			"embedded.title", "title",
			fdsTableSchemaField -> fdsTableSchemaField.setActionId(
				"actionLink"
			).setContentRenderer(
				"simpleActionLinkTableCellRenderer"
			)
		).add(
			"embedded.systemProperties.objectDefinitionBrief.label", "type",
			fdsTableSchemaField -> fdsTableSchemaField.setContentRenderer(
				"assetTypeTableCellRenderer")
		).add(
			"embedded.scopeKey", "space",
			fdsTableSchemaField -> fdsTableSchemaField.setContentRenderer(
				"spaceTableCellRenderer")
		).add(
			"embedded.creator.name", "author",
			fdsTableSchemaField -> fdsTableSchemaField.setContentRenderer(
				"authorTableCellRenderer")
		).add(
			addDateFDSTableSchemaField("dateCreated", "created")
		).build();
	}

	@Override
	public boolean isDefault(String fdsName) {
		return true;
	}

}