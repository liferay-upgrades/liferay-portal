/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.display.context;

import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.document.library.configuration.DLConfiguration;
import com.liferay.frontend.data.set.SystemFDSEntry;
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

import java.util.Map;

/**
 * @author Henrique Pereira
 */
public class ViewExpiringAssetsSectionDisplayContext
	extends BaseSectionDisplayContext {

	public ViewExpiringAssetsSectionDisplayContext(
		DepotEntryLocalService depotEntryLocalService,
		DLConfiguration dlConfiguration, GroupLocalService groupLocalService,
		HttpServletRequest httpServletRequest, Language language,
		ObjectDefinitionService objectDefinitionService, Portal portal,
		TranslationInfoItemFieldValuesExporterRegistry
			translationInfoItemFieldValuesExporterRegistry,
		SystemFDSEntry viewExpiringAssetsSectionSystemFDSEntry) {

		super(
			depotEntryLocalService, dlConfiguration, groupLocalService,
			httpServletRequest, language, objectDefinitionService, portal,
			translationInfoItemFieldValuesExporterRegistry);

		_viewExpiringAssetsSectionSystemFDSEntry =
			viewExpiringAssetsSectionSystemFDSEntry;
	}

	@Override
	public String getAdditionalAPIURLParameters() {
		return _viewExpiringAssetsSectionSystemFDSEntry.
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
	public Map<String, Object> getEmptyState() {
		return HashMapBuilder.<String, Object>put(
			"description",
			language.get(httpServletRequest, "there-are-no-expiring-assets")
		).put(
			"image", "/states/cms_empty_state.svg"
		).put(
			"title", language.get(httpServletRequest, "no-expiring-assets")
		).build();
	}

	@Override
	protected String getCMSSectionFilterString() {
		throw new UnsupportedOperationException(
			"ViewExpiringAssetsSectionSystemFDSEntry must calculate this");
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ViewExpiringAssetsSectionDisplayContext.class);

	private final SystemFDSEntry _viewExpiringAssetsSectionSystemFDSEntry;

}