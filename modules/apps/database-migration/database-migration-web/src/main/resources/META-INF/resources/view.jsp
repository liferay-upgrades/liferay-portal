<%--
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
DatabaseMigrationDisplayContext databaseMigrationDisplayContext = (DatabaseMigrationDisplayContext)renderRequest.getAttribute(DatabaseMigrationDisplayContext.class.getName());
%>

<react:component
	module="{DatabaseMigration} from database-migration-web"
	props="<%= databaseMigrationDisplayContext.getReactData() %>"
/>