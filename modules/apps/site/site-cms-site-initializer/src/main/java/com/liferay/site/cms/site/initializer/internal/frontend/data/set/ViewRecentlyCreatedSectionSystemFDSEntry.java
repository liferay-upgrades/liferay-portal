/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.frontend.data.set;

import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.object.constants.ObjectEntryFolderConstants;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.site.cms.site.initializer.contributor.CMSSectionTypeContributor;
import com.liferay.site.cms.site.initializer.internal.constants.CMSSiteInitializerFDSNames;
import com.liferay.site.cms.site.initializer.internal.display.context.SectionDisplayContextUtil;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * @author Álvaro Leal
 */
@Component(
	property = "frontend.data.set.name=" + CMSSiteInitializerFDSNames.RECENTLY_CREATED_SECTION,
	service = SystemFDSEntry.class
)
public class ViewRecentlyCreatedSectionSystemFDSEntry
	implements SystemFDSEntry {

	@Override
	public String getAdditionalAPIURLParameters(
		HttpServletRequest httpServletRequest) {

		StringBundler sb = new StringBundler();

		sb.append("(cmsSection eq 'contents' or cmsSection eq 'files'");

		for (CMSSectionTypeContributor cmsSectionTypeContributor :
				_cmsSectionTypeContributors) {

			String cmsSectionType =
				cmsSectionTypeContributor.getCMSSectionType();

			if (Validator.isNull(cmsSectionType)) {
				continue;
			}

			sb.append(" or cmsSection eq '");
			sb.append(cmsSectionType);
			sb.append("'");
		}

		sb.append(") and dateCreated ge ");

		Instant instant = Instant.now();

		instant = instant.minus(_RECENTLY_CREATED_DAYS, ChronoUnit.DAYS);

		sb.append(instant.truncatedTo(ChronoUnit.SECONDS));

		sb.append(" and objectDefinitionExternalReferenceCode ne '");
		sb.append(
			ObjectEntryFolderConstants.
				EXTERNAL_REFERENCE_CODE_OBJECT_ENTRY_FOLDER);
		sb.append("' and rootDescendantNode eq false");

		return SectionDisplayContextUtil.getAdditionalAPIURLParameters(
			SectionDisplayContextUtil.appendStatus(
				SectionDisplayContextUtil.appendGroupIds(
					sb.toString(), httpServletRequest)),
			httpServletRequest, null, "dateCreated:desc");
	}

	@Override
	public int getDefaultItemsPerPage() {
		return 20;
	}

	@Override
	public String getDescription() {
		return "CMS Recently Created Section";
	}

	@Override
	public boolean getHideManagementBarInEmptyState() {
		return true;
	}

	@Override
	public String getName() {
		return CMSSiteInitializerFDSNames.RECENTLY_CREATED_SECTION;
	}

	@Override
	public String getPropsTransformer() {
		return "{RecentlyCreatedFDSPropsTransformer} from " +
			"site-cms-site-initializer";
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
		return "time";
	}

	@Override
	public String getTitle() {
		return "Recently Created Section";
	}

	private static final int _RECENTLY_CREATED_DAYS = 7;

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private volatile List<CMSSectionTypeContributor>
		_cmsSectionTypeContributors;

}