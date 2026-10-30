<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
ViewExpiringAssetsSectionDisplayContext viewExpiringAssetsSectionDisplayContext = (ViewExpiringAssetsSectionDisplayContext)request.getAttribute(ViewExpiringAssetsSectionDisplayContext.class.getName());
%>

<div class="cms-expiring-assets-view position-relative">
	<div>
		<react:component
			module="{Breadcrumb} from site-cms-site-initializer"
			props="<%= viewExpiringAssetsSectionDisplayContext.getBreadcrumbProps() %>"
		/>
	</div>

	<div class="cms-section custom-empty-state">
		<frontend-data-set:headless-display
			additionalProps="<%= viewExpiringAssetsSectionDisplayContext.getAdditionalProps() %>"
			apiURL="<%= viewExpiringAssetsSectionDisplayContext.getAPIURL() %>"
			bulkActionDropdownItems="<%= viewExpiringAssetsSectionDisplayContext.getBulkActionDropdownItems() %>"
			emptyState="<%= viewExpiringAssetsSectionDisplayContext.getEmptyState() %>"
			fdsActionDropdownItems="<%= viewExpiringAssetsSectionDisplayContext.getFDSActionDropdownItems() %>"
			formName="fm"
			id="<%= CMSSiteInitializerFDSNames.EXPIRING_ASSETS_SECTION %>"
			itemsPerPage="<%= 20 %>"
			propsTransformer="{AssetsFDSPropsTransformer} from site-cms-site-initializer"
			selectedItemsKey="embedded.id"
			selectionType="multiple"
			showSelectAll="<%= true %>"
		/>
	</div>
</div>