<%--
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
MigrationStatus migrationStatus = databaseMigrationDisplayContext.getMigrationStatus();

boolean migrationRunning = databaseMigrationDisplayContext.isMigrationRunning();
%>

<clay:container-fluid
	cssClass="container-fluid-max-xl"
>
	<liferay-ui:success key="migrationStarted" message="the-database-migration-has-started" />

	<liferay-ui:error key="connectionInformationRequired" message="both-source-and-target-connection-details-are-required" />
	<liferay-ui:error key="migrationAlreadyRunning" message="a-database-migration-is-already-running" />
	<liferay-ui:error key="targetDatabaseMustBePostgreSQL" message="the-target-database-url-must-be-postgresql" />

	<div class="mb-4">
		<h2><liferay-ui:message key="database-migration-tool" /></h2>

		<p class="text-secondary">
			<liferay-ui:message key="database-migration-discovery-help" />
		</p>
	</div>

	<c:if test="<%= migrationStatus.getPhase() != MigrationStatus.PHASE_IDLE %>">
		<clay:sheet
			cssClass="mb-4"
		>
			<div class="d-flex justify-content-between">
				<h3 class="sheet-title"><liferay-ui:message key="migration-status" /></h3>

				<span class="label <%= (migrationStatus.getPhase() == MigrationStatus.PHASE_ERROR) ? "label-danger" : ((migrationStatus.getPhase() == MigrationStatus.PHASE_COMPLETED) ? "label-success" : "label-info") %>">
					<%= HtmlUtil.escape(migrationStatus.getPhaseLabel()) %>
				</span>
			</div>

			<div class="mt-3">
				<clay:progressbar
					value="<%= migrationStatus.getProgress() %>"
				/>
			</div>

			<p class="mt-3 text-secondary">
				<%= HtmlUtil.escape(migrationStatus.getMessage()) %>
			</p>

			<c:if test="<%= !migrationStatus.getTableRowCounts().isEmpty() %>">

				<%
				List<Map.Entry<String, Long>> tableRowCountEntries =
					new ArrayList<>(
						migrationStatus.getTableRowCounts(
						).entrySet());
				%>

				<div class="mt-3">
					<liferay-ui:search-container
						delta="<%= 20 %>"
						total="<%= tableRowCountEntries.size() %>"
						var="tableRowCountSearchContainer"
					>
						<liferay-ui:search-container-results
							results="<%= ListUtil.subList(tableRowCountEntries, tableRowCountSearchContainer.getStart(), tableRowCountSearchContainer.getEnd()) %>"
						/>

						<liferay-ui:search-container-row
							className="java.util.Map.Entry"
							modelVar="entry"
						>
							<liferay-ui:search-container-column-text
								name="table"
								value="<%= HtmlUtil.escape((String)entry.getKey()) %>"
							/>

							<liferay-ui:search-container-column-text
								cssClass="text-right"
								name="rows-copied"
								value="<%= String.valueOf(entry.getValue()) %>"
							/>
						</liferay-ui:search-container-row>

						<liferay-ui:search-iterator
							markupView="lexicon"
						/>
					</liferay-ui:search-container>
				</div>
			</c:if>
		</clay:sheet>
	</c:if>

	<c:if test="<%= !migrationStatus.getMigrationErrors().isEmpty() %>">
		<clay:sheet
			cssClass="mb-4"
		>
			<h3 class="sheet-title">
				<liferay-ui:message key="migration-errors" /> (<%= migrationStatus.getMigrationErrors().size() %>)
			</h3>

			<p class="text-secondary">
				<liferay-ui:message key="some-rows-could-not-be-copied-and-were-skipped" />
			</p>

			<table class="mt-3 table table-autofit table-list">
				<thead>
					<tr>
						<th><liferay-ui:message key="table" /></th>
						<th><liferay-ui:message key="row" /></th>
						<th><liferay-ui:message key="error" /></th>
						<th><liferay-ui:message key="suggested-fix-query" /></th>
					</tr>
				</thead>

				<tbody>

					<%
					int errorIndex = 0;

					for (MigrationError migrationError : migrationStatus.getMigrationErrors()) {
						String suggestedSQLId = liferayPortletResponse.getNamespace() + "suggestedSQL" + errorIndex++;
					%>

						<tr>
							<td><%= HtmlUtil.escape(migrationError.getTableName()) %></td>
							<td>
								<code><%= HtmlUtil.escape(migrationError.getRowIdentifier()) %></code>
							</td>
							<td>
								<c:if test="<%= Validator.isNotNull(migrationError.getSQLState()) %>">
									<span class="text-danger">[<%= HtmlUtil.escape(migrationError.getSQLState()) %>]</span>
								</c:if>

								<%= HtmlUtil.escape(migrationError.getMessage()) %>
							</td>
							<td>
								<textarea class="form-control monospace-font" id="<%= suggestedSQLId %>" readonly="readonly" rows="2"><%= HtmlUtil.escape(migrationError.getSuggestedSQL()) %></textarea>

								<button class="btn btn-secondary btn-sm database-migration-copy-button mt-2" data-textarea-id="<%= suggestedSQLId %>" type="button">
									<liferay-ui:message key="copy" />
								</button>
							</td>
						</tr>

					<%
					}
					%>

				</tbody>
			</table>

			<aui:script>
				document.querySelectorAll(
					'.database-migration-copy-button'
				).forEach(
					function(button) {
						button.addEventListener(
							'click',
							function() {
								var textarea = document.getElementById(
									button.getAttribute('data-textarea-id'));

								if (textarea) {
									navigator.clipboard.writeText(textarea.value);
								}
							}
						);
					}
				);
			</aui:script>
		</clay:sheet>
	</c:if>

	<portlet:actionURL name="/database_migration/start_migration" var="startMigrationURL" />

	<aui:form action="<%= startMigrationURL %>" cssClass="sheet" method="post" name="fm">
		<div class="row">
			<div class="col-md-6">
				<h3 class="sheet-subtitle"><liferay-ui:message key="source-database" /></h3>

				<aui:input disabled="<%= migrationRunning %>" label="jdbc-url" name="sourceJDBCURL" placeholder="jdbc:mysql://host:3306/lportal" required="<%= true %>" type="text" value='<%= ParamUtil.getString(renderRequest, "sourceJDBCURL") %>' />

				<aui:input disabled="<%= migrationRunning %>" label="user-name" name="sourceUserName" required="<%= true %>" type="text" value='<%= ParamUtil.getString(renderRequest, "sourceUserName") %>' />

				<aui:input disabled="<%= migrationRunning %>" label="password" name="sourcePassword" type="password" value='<%= ParamUtil.getString(renderRequest, "sourcePassword") %>' />
			</div>

			<div class="col-md-6">
				<h3 class="sheet-subtitle"><liferay-ui:message key="target-database-postgresql" /></h3>

				<aui:input disabled="<%= migrationRunning %>" label="jdbc-url" name="targetJDBCURL" placeholder="jdbc:postgresql://host:5432/lportal" required="<%= true %>" type="text" value='<%= ParamUtil.getString(renderRequest, "targetJDBCURL") %>' />

				<aui:input disabled="<%= migrationRunning %>" label="user-name" name="targetUserName" required="<%= true %>" type="text" value='<%= ParamUtil.getString(renderRequest, "targetUserName") %>' />

				<aui:input disabled="<%= migrationRunning %>" label="password" name="targetPassword" type="password" value='<%= ParamUtil.getString(renderRequest, "targetPassword") %>' />
			</div>
		</div>

		<aui:button-row>
			<aui:button disabled="<%= migrationRunning %>" primary="<%= true %>" type="submit" value="start-migration" />
		</aui:button-row>
	</aui:form>

	<c:if test="<%= migrationRunning %>">
		<aui:script>
			setTimeout(
				function() {
					window.location.reload();
				},
				3000
			);
		</aui:script>
	</c:if>
</clay:container-fluid>