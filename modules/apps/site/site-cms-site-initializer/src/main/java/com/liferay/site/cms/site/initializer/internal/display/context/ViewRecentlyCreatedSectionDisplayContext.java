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
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItem;
import com.liferay.object.service.ObjectDefinitionService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.translation.exporter.TranslationInfoItemFieldValuesExporterRegistry;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * @author Álvaro Leal
 */
public class ViewRecentlyCreatedSectionDisplayContext
	extends BaseSectionDisplayContext {

	public ViewRecentlyCreatedSectionDisplayContext(
		DepotEntryLocalService depotEntryLocalService,
		DLConfiguration dlConfiguration, GroupLocalService groupLocalService,
		HttpServletRequest httpServletRequest, Language language,
		ObjectDefinitionService objectDefinitionService, Portal portal,
		TranslationInfoItemFieldValuesExporterRegistry
			translationInfoItemFieldValuesExporterRegistry,
		FDSItemsActions viewAllSectionFDSItemsActions,
		SystemFDSEntry viewRecentlyCreatedSectionSystemFDSEntry) {

		super(
			depotEntryLocalService, dlConfiguration, groupLocalService,
			httpServletRequest, language, objectDefinitionService, portal,
			translationInfoItemFieldValuesExporterRegistry);

		_viewAllSectionFDSItemsActions = viewAllSectionFDSItemsActions;
		_viewRecentlyCreatedSectionSystemFDSEntry =
			viewRecentlyCreatedSectionSystemFDSEntry;
	}

	@Override
	public String getAdditionalAPIURLParameters() {
		return _viewRecentlyCreatedSectionSystemFDSEntry.
			getAdditionalAPIURLParameters(httpServletRequest);
	}

	@Override
	public Map<String, Object> getAdditionalProps() {
		Map<String, Object> additionalProps = super.getAdditionalProps();

		try {
			additionalProps.put("breadcrumbProps", getBreadcrumbProps());
		}
		catch (PortalException portalException) {
			if (_log.isDebugEnabled()) {
				_log.debug(portalException);
			}
		}

		return additionalProps;
	}

	@Override
	public List<DropdownItem> getBulkActionDropdownItems() {
		return SectionDisplayContextUtil.getAllSectionBulkActionDropdownItems(
			httpServletRequest);
	}

	@Override
	public Map<String, Object> getEmptyState() {
		return HashMapBuilder.<String, Object>put(
			"description",
			language.get(
				httpServletRequest,
				"there-are-no-assets-created-in-the-last-7-days")
		).put(
			"image", "/states/cms_empty_state.svg"
		).put(
			"title",
			language.get(httpServletRequest, "no-recently-created-assets")
		).build();
	}

	@Override
	public List<FDSActionDropdownItem> getFDSActionDropdownItems() {
		return _viewAllSectionFDSItemsActions.getFDSActionDropdownItems(
			httpServletRequest);
	}

	@Override
	protected String getCMSSectionFilterString() {
		throw new UnsupportedOperationException(
			"ViewRecentlyCreatedSectionSystemFDSEntry must calculate this");
	}

	@Override
	protected boolean isFolderSearchEnabled() {
		return true;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ViewRecentlyCreatedSectionDisplayContext.class);

	private final FDSItemsActions _viewAllSectionFDSItemsActions;
	private final SystemFDSEntry _viewRecentlyCreatedSectionSystemFDSEntry;

}