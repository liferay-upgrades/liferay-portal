<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
ViewScheduledContentSectionDisplayContext viewScheduledContentSectionDisplayContext = (ViewScheduledContentSectionDisplayContext)request.getAttribute(ViewScheduledContentSectionDisplayContext.class.getName());
%>

<div class="cms-scheduled-content-view position-relative">
	<div>
		<react:component
			module="{Breadcrumb} from site-cms-site-initializer"
			props="<%= viewScheduledContentSectionDisplayContext.getBreadcrumbProps() %>"
		/>
	</div>

	<div>
		<react:component
			module="{UpcomingPublicationsAlert} from site-cms-site-initializer"
			props="<%= viewScheduledContentSectionDisplayContext.getScheduledContentProps() %>"
		/>
	</div>

	<div>
		<react:component
			module="{ScheduledContentQuickFilters} from site-cms-site-initializer"
			props="<%= viewScheduledContentSectionDisplayContext.getScheduledContentProps() %>"
		/>
	</div>

	<div class="cms-section custom-empty-state">
		<frontend-data-set:headless-display
			additionalProps="<%= viewScheduledContentSectionDisplayContext.getAdditionalProps() %>"
			apiURL="<%= viewScheduledContentSectionDisplayContext.getAPIURL() %>"
			emptyState="<%= viewScheduledContentSectionDisplayContext.getEmptyState() %>"
			fdsActionDropdownItems="<%= viewScheduledContentSectionDisplayContext.getFDSActionDropdownItems() %>"
			formName="fm"
			id="<%= CMSSiteInitializerFDSNames.SCHEDULED_CONTENT_SECTION %>"
			itemsPerPage="<%= 20 %>"
			propsTransformer="{ScheduledContentFDSPropsTransformer} from site-cms-site-initializer"
		/>
	</div>
</div>