<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
ViewHomeMyDraftsDisplayContext viewHomeMyDraftsDisplayContext = (ViewHomeMyDraftsDisplayContext)request.getAttribute(ViewHomeMyDraftsDisplayContext.class.getName());
%>

<div class="cms-section">
	<react:component
		module="{MyDrafts} from site-cms-site-initializer"
		props="<%= viewHomeMyDraftsDisplayContext.getProps() %>"
	/>
</div>