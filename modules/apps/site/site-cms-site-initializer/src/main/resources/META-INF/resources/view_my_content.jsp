<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
ViewMyContentSectionDisplayContext viewMyContentSectionDisplayContext = (ViewMyContentSectionDisplayContext)request.getAttribute(ViewMyContentSectionDisplayContext.class.getName());
%>

<div class="cms-my-content-view position-relative">
	<div>
		<react:component
			module="{Breadcrumb} from site-cms-site-initializer"
			props="<%= viewMyContentSectionDisplayContext.getBreadcrumbProps() %>"
		/>
	</div>

	<div class="cms-section custom-empty-state">
		<frontend-data-set:headless-display
			additionalProps="<%= viewMyContentSectionDisplayContext.getAdditionalProps() %>"
			apiURL="<%= viewMyContentSectionDisplayContext.getAPIURL() %>"
			bulkActionDropdownItems="<%= viewMyContentSectionDisplayContext.getBulkActionDropdownItems() %>"
			emptyState="<%= viewMyContentSectionDisplayContext.getEmptyState() %>"
			fdsActionDropdownItems="<%= viewMyContentSectionDisplayContext.getFDSActionDropdownItems() %>"
			formName="fm"
			id="<%= CMSSiteInitializerFDSNames.MY_CONTENT_SECTION %>"
			itemsPerPage="<%= 20 %>"
			propsTransformer="{AssetsFDSPropsTransformer} from site-cms-site-initializer"
			selectedItemsKey="embedded.id"
			selectionType="multiple"
			showSelectAll="<%= true %>"
		/>
	</div>
</div>