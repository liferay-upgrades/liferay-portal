/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.display.context;

import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.document.library.configuration.DLConfiguration;
import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.frontend.data.set.action.FDSItemsActions;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.object.service.ObjectDefinitionService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.translation.exporter.TranslationInfoItemFieldValuesExporterRegistry;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * @author Rodrigo Cavalcanti
 */
public class ViewScheduledContentSectionDisplayContext
	extends BaseSectionDisplayContext {

	public ViewScheduledContentSectionDisplayContext(
		DepotEntryLocalService depotEntryLocalService,
		DLConfiguration dlConfiguration, GroupLocalService groupLocalService,
		HttpServletRequest httpServletRequest, Language language,
		ObjectDefinitionService objectDefinitionService, Portal portal,
		TranslationInfoItemFieldValuesExporterRegistry
			translationInfoItemFieldValuesExporterRegistry,
		FDSItemsActions viewScheduledContentSectionFDSItemsActions,
		SystemFDSEntry viewScheduledContentSectionSystemFDSEntry) {

		super(
			depotEntryLocalService, dlConfiguration, groupLocalService,
			httpServletRequest, language, objectDefinitionService, portal,
			translationInfoItemFieldValuesExporterRegistry);

		_viewScheduledContentSectionFDSItemsActions =
			viewScheduledContentSectionFDSItemsActions;
		_viewScheduledContentSectionSystemFDSEntry =
			viewScheduledContentSectionSystemFDSEntry;
	}

	@Override
	public String getAdditionalAPIURLParameters() {
		return _viewScheduledContentSectionSystemFDSEntry.
			getAdditionalAPIURLParameters(httpServletRequest);
	}

	@Override
	public Map<String, Object> getBreadcrumbProps() throws PortalException {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		addBreadcrumbItem(
			jsonArray, true, null,
			language.get(httpServletRequest, "scheduled-content"));

		return HashMapBuilder.<String, Object>put(
			"breadcrumbItems", jsonArray
		).put(
			"hideSpace", true
		).build();
	}

	@Override
	public Map<String, Object> getEmptyState() {
		return HashMapBuilder.<String, Object>put(
			"description",
			language.get(
				httpServletRequest,
				"there-is-no-content-scheduled-to-be-published-yet")
		).put(
			"image", "/states/cms_empty_state.svg"
		).put(
			"title", language.get(httpServletRequest, "no-scheduled-content")
		).build();
	}

	@Override
	public List<FDSActionDropdownItem> getFDSActionDropdownItems() {
		return _viewScheduledContentSectionFDSItemsActions.
			getFDSActionDropdownItems(httpServletRequest);
	}

	public Map<String, Object> getScheduledContentProps() {
		return HashMapBuilder.<String, Object>put(
			"apiURL", getAPIURL()
		).build();
	}

	@Override
	protected String getCMSSectionFilterString() {
		throw new UnsupportedOperationException(
			"ViewScheduledContentSectionSystemFDSEntry must calculate this");
	}

	private final FDSItemsActions _viewScheduledContentSectionFDSItemsActions;
	private final SystemFDSEntry _viewScheduledContentSectionSystemFDSEntry;

}