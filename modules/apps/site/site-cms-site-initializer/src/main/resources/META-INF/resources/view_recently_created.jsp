<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
ViewRecentlyCreatedSectionDisplayContext viewRecentlyCreatedSectionDisplayContext = (ViewRecentlyCreatedSectionDisplayContext)request.getAttribute(ViewRecentlyCreatedSectionDisplayContext.class.getName());
%>

<div class="cms-recently-created-view position-relative">
	<div class="pt-4 px-4">
		<h2 class="cms-recently-created-view__title font-weight-semi-bold mb-1 text-dark">
			<liferay-ui:message key="recently-created" />
		</h2>

		<p class="font-weight-normal mb-0 text-3 text-secondary">
			<liferay-ui:message key="assets-created-in-the-last-7-days,-newest-first" />
		</p>
	</div>

	<div class="cms-section custom-empty-state">
		<frontend-data-set:headless-display
			additionalProps="<%= viewRecentlyCreatedSectionDisplayContext.getAdditionalProps() %>"
			apiURL="<%= viewRecentlyCreatedSectionDisplayContext.getAPIURL() %>"
			bulkActionDropdownItems="<%= viewRecentlyCreatedSectionDisplayContext.getBulkActionDropdownItems() %>"
			emptyState="<%= viewRecentlyCreatedSectionDisplayContext.getEmptyState() %>"
			fdsActionDropdownItems="<%= viewRecentlyCreatedSectionDisplayContext.getFDSActionDropdownItems() %>"
			formName="fm"
			id="<%= CMSSiteInitializerFDSNames.RECENTLY_CREATED_SECTION %>"
			itemsPerPage="<%= 20 %>"
			propsTransformer="{RecentlyCreatedFDSPropsTransformer} from site-cms-site-initializer"
			selectedItemsKey="embedded.id"
			selectionType="multiple"
			showSelectAll="<%= true %>"
		/>
	</div>
</div>