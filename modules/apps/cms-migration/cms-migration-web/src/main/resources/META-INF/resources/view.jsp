<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
CMSMigrationDisplayContext cmsMigrationDisplayContext = (CMSMigrationDisplayContext)request.getAttribute(WebKeys.PORTLET_DISPLAY_CONTEXT);
%>

<clay:container-fluid
	cssClass="container-form-lg"
>
	<liferay-ui:success key="migrationStarted" message="cms-migration-started" />

	<liferay-ui:error exception="<%= DepotEntryNameException.class %>" message="please-enter-a-valid-name" />
	<liferay-ui:error exception="<%= Exception.class %>" message="an-unexpected-error-occurred" />

	<clay:sheet>
		<portlet:actionURL name="/cms_migration/start_cms_migration" var="startCMSMigrationURL" />

		<aui:form action="<%= startCMSMigrationURL %>" method="post" name="fm">
			<clay:sheet-header>
				<h2 class="sheet-title">
					<%= HtmlUtil.escape(portletDisplay.getTitle()) %>
				</h2>

				<div class="sheet-text">
					<liferay-ui:message key="cms-migration-description" />
				</div>
			</clay:sheet-header>

			<clay:sheet-section>
				<aui:select label="cms-migration-source-site" name="sourceGroupId" required="<%= true %>">

					<%
					for (Group group : cmsMigrationDisplayContext.getGroups()) {
					%>

						<aui:option label="<%= HtmlUtil.escape(group.getDescriptiveName(locale)) %>" value="<%= group.getGroupId() %>" />

					<%
					}
					%>

				</aui:select>

				<%
				List<DepotEntry> depotEntries = cmsMigrationDisplayContext.getDepotEntries();
				%>

				<aui:select label="cms-migration-target-space" name="depotEntryId">
					<aui:option label="cms-migration-create-new-space" selected="<%= depotEntries.isEmpty() %>" value="0" />

					<%
					for (int i = 0; i < depotEntries.size(); i++) {
						DepotEntry depotEntry = depotEntries.get(i);

						Group depotGroup = depotEntry.getGroup();
					%>

						<aui:option label="<%= HtmlUtil.escape(depotGroup.getDescriptiveName(locale)) %>" selected="<%= i == 0 %>" value="<%= depotEntry.getDepotEntryId() %>" />

					<%
					}
					%>

				</aui:select>

				<div class="<%= depotEntries.isEmpty() ? StringPool.BLANK : "d-none" %>" id="<portlet:namespace />depotEntryNameContainer">
					<aui:input label="cms-migration-new-space-name" name="depotEntryName" type="text" />
				</div>
			</clay:sheet-section>

			<clay:sheet-section>
				<h3 class="sheet-subtitle">
					<liferay-ui:message key="cms-migration-content-types" />
				</h3>

				<aui:input checked="<%= true %>" label="web-content" name="migrateJournalArticles" type="checkbox" />

				<aui:input checked="<%= true %>" label="blogs" name="migrateBlogsEntries" type="checkbox" />

				<aui:input checked="<%= true %>" label="documents-and-media" name="migrateFileEntries" type="checkbox" />

				<aui:input checked="<%= true %>" helpMessage="cms-migration-rewrite-pages-help" label="cms-migration-rewrite-pages" name="rewritePages" type="checkbox" />
			</clay:sheet-section>

			<clay:sheet-footer>
				<aui:button type="submit" value="cms-migration-start" />
			</clay:sheet-footer>
		</aui:form>
	</clay:sheet>

	<clay:sheet>
		<clay:sheet-header>
			<h2 class="sheet-title">
				<liferay-ui:message key="cms-migration-migrations" />
			</h2>
		</clay:sheet-header>

		<clay:sheet-section>
			<div id="<portlet:namespace />cmsMigrations">
				<liferay-util:include page="/cms_migrations.jsp" servletContext="<%= application %>" />
			</div>
		</clay:sheet-section>
	</clay:sheet>
</clay:container-fluid>

<portlet:resourceURL id="/cms_migration/get_cms_migrations" var="getCMSMigrationsURL" />

<aui:script>
	const depotEntryIdSelect = document.getElementById(
		'<portlet:namespace />depotEntryId'
	);
	const depotEntryNameContainer = document.getElementById(
		'<portlet:namespace />depotEntryNameContainer'
	);

	depotEntryIdSelect.addEventListener('change', () => {
		depotEntryNameContainer.classList.toggle(
			'd-none',
			depotEntryIdSelect.value !== '0'
		);
	});

	const cmsMigrations = document.getElementById(
		'<portlet:namespace />cmsMigrations'
	);

	const refreshCMSMigrations = () => {
		if (!cmsMigrations.querySelector('[data-in-progress="true"]')) {
			return;
		}

		setTimeout(() => {
			Liferay.Util.fetch('<%= getCMSMigrationsURL %>')
				.then((response) => response.text())
				.then((html) => {
					cmsMigrations.innerHTML = html;

					refreshCMSMigrations();
				});
		}, 5000);
	};

	refreshCMSMigrations();
</aui:script>