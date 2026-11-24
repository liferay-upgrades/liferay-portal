<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
ViewHomeRecentlyViewedDisplayContext viewHomeRecentlyViewedDisplayContext = (ViewHomeRecentlyViewedDisplayContext)request.getAttribute(ViewHomeRecentlyViewedDisplayContext.class.getName());
%>

<div class="cms-section">
	<react:component
		module="{RecentlyViewed} from site-cms-site-initializer"
		props="<%= viewHomeRecentlyViewedDisplayContext.getProps() %>"
	/>
</div>