/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.frontend.data.set;

import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.object.constants.ObjectEntryFolderConstants;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.site.cms.site.initializer.internal.constants.CMSSiteInitializerFDSNames;
import com.liferay.site.cms.site.initializer.internal.display.context.SectionDisplayContextUtil;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;

/**
 * @author Samuel Lopes
 */
@Component(
	property = "frontend.data.set.name=" + CMSSiteInitializerFDSNames.MY_CONTENT_SECTION,
	service = SystemFDSEntry.class
)
public class ViewMyContentSectionSystemFDSEntry implements SystemFDSEntry {

	@Override
	public String getAdditionalAPIURLParameters(
		HttpServletRequest httpServletRequest) {

		ThemeDisplay themeDisplay =
			(ThemeDisplay)httpServletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		String filterString = SectionDisplayContextUtil.appendGroupIds(
			StringBundler.concat(
				"creatorId eq ", themeDisplay.getUserId(), " and status eq ",
				WorkflowConstants.STATUS_DRAFT,
				" and (cmsSection eq 'contents' or cmsSection eq 'files') and ",
				"objectDefinitionExternalReferenceCode ne '",
				ObjectEntryFolderConstants.
					EXTERNAL_REFERENCE_CODE_OBJECT_ENTRY_FOLDER,
				"' and rootDescendantNode eq false"),
			httpServletRequest);

		return SectionDisplayContextUtil.getAdditionalAPIURLParameters(
			filterString, httpServletRequest, null);
	}

	@Override
	public int getDefaultItemsPerPage() {
		return 20;
	}

	@Override
	public String getDescription() {
		return "CMS My Content Section";
	}

	@Override
	public boolean getHideManagementBarInEmptyState() {
		return true;
	}

	@Override
	public String getName() {
		return CMSSiteInitializerFDSNames.MY_CONTENT_SECTION;
	}

	@Override
	public String getPropsTransformer() {
		return "{AssetsFDSPropsTransformer} from site-cms-site-initializer";
	}

	@Override
	public String getRESTApplication() {
		return "/search/v1.0";
	}

	@Override
	public String getRESTEndpoint() {
		return "/v1.0/search";
	}

	@Override
	public String getRESTSchema() {
		return "SearchResult";
	}

	@Override
	public boolean getSearchAsYouType() {
		return true;
	}

	@Override
	public boolean getSearchSuggestionsEnabled() {
		return true;
	}

	@Override
	public String getSymbol() {
		return "pencil";
	}

	@Override
	public String getTitle() {
		return "My Content Section";
	}

}