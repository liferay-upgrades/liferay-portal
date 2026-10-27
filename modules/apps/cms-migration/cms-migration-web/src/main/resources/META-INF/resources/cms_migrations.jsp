<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
CMSMigrationDisplayContext cmsMigrationDisplayContext = (CMSMigrationDisplayContext)request.getAttribute(WebKeys.PORTLET_DISPLAY_CONTEXT);

Format dateTimeFormat = FastDateFormatFactoryUtil.getDateTime(locale, timeZone);
%>

<portlet:renderURL var="redirectURL" />

<%
List<BackgroundTask> backgroundTasks = cmsMigrationDisplayContext.getBackgroundTasks();
%>

<c:if test="<%= backgroundTasks.isEmpty() %>">
	<div class="text-secondary">
		<liferay-ui:message key="cms-migration-no-migrations" />
	</div>
</c:if>

<%
for (BackgroundTask backgroundTask : backgroundTasks) {
	CMSMigrationReport cmsMigrationReport = cmsMigrationDisplayContext.getCMSMigrationReport(backgroundTask);
%>

	<div class="border-bottom mb-3 pb-3" data-in-progress="<%= backgroundTask.isInProgress() %>">
		<div class="autofit-row">
			<div class="autofit-col autofit-col-expand">
				<h4>
					<%= HtmlUtil.escape(backgroundTask.getName()) %>
				</h4>

				<div class="text-secondary">
					<%= dateTimeFormat.format(backgroundTask.getCreateDate()) %>
				</div>
			</div>

			<div class="autofit-col">
				<clay:label
					displayType="<%= BackgroundTaskConstants.getStatusCssClass(backgroundTask.getStatus()) %>"
					label="<%= LanguageUtil.get(request, backgroundTask.getStatusLabel()) %>"
				/>
			</div>

			<c:if test="<%= backgroundTask.isCompleted() %>">
				<div class="autofit-col">
					<portlet:actionURL name="/cms_migration/delete_cms_migration" var="deleteCMSMigrationURL">
						<portlet:param name="backgroundTaskId" value="<%= String.valueOf(backgroundTask.getBackgroundTaskId()) %>" />
						<portlet:param name="redirect" value="<%= redirectURL %>" />
					</portlet:actionURL>

					<clay:link
						displayType="secondary"
						href="<%= deleteCMSMigrationURL %>"
						label="delete"
						small="<%= true %>"
						type="button"
					/>
				</div>
			</c:if>
		</div>

		<c:if test="<%= backgroundTask.isInProgress() %>">

			<%
			BackgroundTaskDisplay backgroundTaskDisplay = cmsMigrationDisplayContext.getBackgroundTaskDisplay(backgroundTask);
			%>

			<div class="mt-2">
				<%= backgroundTaskDisplay.renderDisplayTemplate(locale) %>
			</div>

			<c:if test="<%= backgroundTaskDisplay.hasPercentage() %>">
				<clay:progressbar
					value="<%= backgroundTaskDisplay.getPercentage() %>"
				/>
			</c:if>
		</c:if>

		<c:if test="<%= backgroundTask.isCompleted() %>">
			<ul class="list-inline mt-2">

				<%
				for (Map.Entry<String, Long> entry :
						cmsMigrationReport.getCounts(
						).entrySet()) {
				%>

					<li class="list-inline-item">
						<strong><%= entry.getValue() %></strong> <liferay-ui:message key='<%= "cms-migration-" + entry.getKey() %>' />
					</li>

				<%
				}
				%>

			</ul>

			<c:if test="<%= !cmsMigrationReport.getErrors().isEmpty() %>">
				<details class="mt-2">
					<summary class="text-danger">
						<liferay-ui:message arguments="<%= cmsMigrationReport.getErrors().size() %>" key="cms-migration-x-errors" />
					</summary>

					<ul>

						<%
						for (String error : cmsMigrationReport.getErrors()) {
						%>

							<li><%= HtmlUtil.escape(error) %></li>

						<%
						}
						%>

					</ul>
				</details>
			</c:if>

			<c:if test="<%= !cmsMigrationReport.getWarnings().isEmpty() %>">
				<details class="mt-2">
					<summary class="text-warning">
						<liferay-ui:message arguments="<%= cmsMigrationReport.getWarnings().size() %>" key="cms-migration-x-warnings" />
					</summary>

					<ul>

						<%
						for (String warning : cmsMigrationReport.getWarnings()) {
						%>

							<li><%= HtmlUtil.escape(warning) %></li>

						<%
						}
						%>

					</ul>
				</details>
			</c:if>
		</c:if>
	</div>

<%
}
%>