/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.portlet.action;

import com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskExecutor;
import com.liferay.cms.migration.web.internal.constants.CMSMigrationConstants;
import com.liferay.cms.migration.web.internal.constants.CMSMigrationPortletKeys;
import com.liferay.cms.migration.web.internal.migration.CMSMigrationReport;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.object.constants.ObjectEntryFolderConstants;
import com.liferay.object.field.attachment.AttachmentManager;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntryFolder;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryFolderLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManager;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.CSVUtil;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.text.Format;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Samuel Lopes
 */
@Component(
	property = {
		"jakarta.portlet.name=" + CMSMigrationPortletKeys.CMS_MIGRATION,
		"mvc.command.name=/cms_migration/save_cms_migration_logs"
	},
	service = MVCActionCommand.class
)
public class SaveCMSMigrationLogsMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		hideDefaultSuccessMessage(actionRequest);

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		try {
			_checkPermission(themeDisplay);

			JSONPortletResponseUtil.writeJSON(
				actionRequest, actionResponse,
				_saveLogs(
					_getCMSMigrationBackgroundTask(actionRequest, themeDisplay),
					themeDisplay));
		}
		catch (Exception exception) {
			_log.error(exception);

			JSONPortletResponseUtil.writeJSON(
				actionRequest, actionResponse,
				JSONUtil.put(
					"error",
					_language.get(
						themeDisplay.getLocale(),
						"an-unexpected-error-occurred")));
		}
	}

	private void _appendRow(StringBundler sb, String... values) {
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				sb.append(StringPool.COMMA);
			}

			String value = values[i];

			if (!value.isEmpty() &&
				(_FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0)) {

				value = StringPool.APOSTROPHE + value;
			}

			sb.append(CSVUtil.encode(value));
		}

		sb.append(StringPool.RETURN_NEW_LINE);
	}

	private void _checkPermission(ThemeDisplay themeDisplay)
		throws PrincipalException {

		PermissionChecker permissionChecker =
			themeDisplay.getPermissionChecker();

		if (!permissionChecker.isCompanyAdmin()) {
			throw new PrincipalException.MustBeCompanyAdmin(
				permissionChecker.getUserId());
		}
	}

	private BackgroundTask _getCMSMigrationBackgroundTask(
			ActionRequest actionRequest, ThemeDisplay themeDisplay)
		throws Exception {

		BackgroundTask backgroundTask =
			_backgroundTaskManager.getBackgroundTask(
				ParamUtil.getLong(actionRequest, "backgroundTaskId"));

		if (!Objects.equals(
				backgroundTask.getTaskExecutorClassName(),
				CMSMigrationBackgroundTaskExecutor.class.getName()) ||
			(backgroundTask.getCompanyId() != themeDisplay.getCompanyId())) {

			throw new PrincipalException();
		}

		if (!backgroundTask.isCompleted()) {
			throw new PortalException(
				"Background task " + backgroundTask.getBackgroundTaskId() +
					" is not completed");
		}

		return backgroundTask;
	}

	private byte[] _getCSVBytes(
		CMSMigrationReport cmsMigrationReport, Locale locale) {

		StringBundler sb = new StringBundler();

		sb.append("\uFEFF");

		_appendRow(
			sb, _language.get(locale, "type"),
			_language.get(locale, "message"));

		String error = _language.get(locale, "error");

		for (String message : cmsMigrationReport.getErrors()) {
			_appendRow(sb, error, message);
		}

		String warning = _language.get(locale, "warning");

		for (String message : cmsMigrationReport.getWarnings()) {
			_appendRow(sb, warning, message);
		}

		String csv = sb.toString();

		return csv.getBytes(StandardCharsets.UTF_8);
	}

	private String _getFileName(
		BackgroundTask backgroundTask, ThemeDisplay themeDisplay) {

		StringBundler sb = new StringBundler(5);

		sb.append("cms-migration-");

		String name = StringUtil.toLowerCase(backgroundTask.getName());

		name = name.replaceAll("[^\\p{L}\\p{N}]+", StringPool.DASH);
		name = name.replaceAll("^-|-$", StringPool.BLANK);

		if (Validator.isNotNull(name)) {
			sb.append(name);
			sb.append(StringPool.DASH);
		}

		Format format = FastDateFormatFactoryUtil.getSimpleDateFormat(
			"yyyy-MM-dd-HHmmss", themeDisplay.getLocale(),
			themeDisplay.getTimeZone());

		sb.append(format.format(new Date()));

		sb.append(".csv");

		return sb.toString();
	}

	private ObjectEntryFolder _getOrAddObjectEntryFolder(
			long groupId, ServiceContext serviceContext)
		throws Exception {

		ObjectEntryFolder filesObjectEntryFolder =
			_objectEntryFolderLocalService.
				getObjectEntryFolderByExternalReferenceCode(
					ObjectEntryFolderConstants.EXTERNAL_REFERENCE_CODE_FILES,
					groupId, serviceContext.getCompanyId());

		String name = _language.get(
			LocaleUtil.getSiteDefault(), "cms-migration-logs");

		ObjectEntryFolder objectEntryFolder =
			_objectEntryFolderLocalService.fetchObjectEntryFolder(
				groupId, serviceContext.getCompanyId(),
				filesObjectEntryFolder.getObjectEntryFolderId(), name);

		if (objectEntryFolder != null) {
			return objectEntryFolder;
		}

		return _objectEntryFolderLocalService.addObjectEntryFolder(
			null, groupId, serviceContext.getUserId(),
			filesObjectEntryFolder.getObjectEntryFolderId(), StringPool.BLANK,
			HashMapBuilder.put(
				LocaleUtil.getSiteDefault(), name
			).build(),
			name, serviceContext);
	}

	private ServiceContext _getServiceContext(
		long groupId, ThemeDisplay themeDisplay) {

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(themeDisplay.getCompanyId());
		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(themeDisplay.getUserId());
		serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

		return serviceContext;
	}

	private JSONObject _saveLogs(
			BackgroundTask backgroundTask, ThemeDisplay themeDisplay)
		throws Exception {

		Map<String, Serializable> taskContextMap =
			backgroundTask.getTaskContextMap();

		DepotEntry depotEntry = _depotEntryLocalService.getDepotEntry(
			GetterUtil.getLong(taskContextMap.get("depotEntryId")));

		long groupId = depotEntry.getGroupId();

		ServiceContext serviceContext = _getServiceContext(
			groupId, themeDisplay);

		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.
				getObjectDefinitionByExternalReferenceCode(
					"L_CMS_BASIC_DOCUMENT", themeDisplay.getCompanyId());

		ObjectField objectField = _objectFieldLocalService.getObjectField(
			objectDefinition.getObjectDefinitionId(),
			CMSMigrationConstants.OBJECT_FIELD_NAME_FILE);

		FileEntry fileEntry = null;

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.
					setInitializingPortalInstanceWithSafeCloseable(true)) {

			fileEntry = _attachmentManager.getOrAddFileEntry(
				themeDisplay.getCompanyId(), null,
				_getCSVBytes(
					CMSMigrationReport.of(backgroundTask),
					themeDisplay.getLocale()),
				_getFileName(backgroundTask, themeDisplay), groupId,
				objectField.getObjectFieldId(), serviceContext);
		}

		String fileName = fileEntry.getFileName();

		String languageId = LocaleUtil.toLanguageId(
			LocaleUtil.getSiteDefault());

		ObjectEntryFolder objectEntryFolder = _getOrAddObjectEntryFolder(
			groupId, serviceContext);

		_objectEntryLocalService.addObjectEntry(
			groupId, themeDisplay.getUserId(),
			objectDefinition.getObjectDefinitionId(),
			objectEntryFolder.getObjectEntryFolderId(), languageId,
			HashMapBuilder.<String, Serializable>put(
				CMSMigrationConstants.OBJECT_FIELD_NAME_FILE,
				fileEntry.getFileEntryId()
			).put(
				CMSMigrationConstants.OBJECT_FIELD_NAME_TITLE + "_i18n",
				HashMapBuilder.put(
					languageId, fileName
				).build()
			).build(),
			serviceContext);

		return JSONUtil.put(
			"fileName", fileName
		).put(
			"folderName", objectEntryFolder.getName()
		).put(
			"folderURL",
			StringBundler.concat(
				themeDisplay.getPathFriendlyURLPublic(),
				GroupConstants.CMS_FRIENDLY_URL, "/e/view-folder/",
				_portal.getClassNameId(ObjectEntryFolder.class),
				StringPool.SLASH, objectEntryFolder.getObjectEntryFolderId())
		);
	}

	private static final String _FORMULA_PREFIXES = "\t\r+-=@";

	private static final Log _log = LogFactoryUtil.getLog(
		SaveCMSMigrationLogsMVCActionCommand.class);

	@Reference
	private AttachmentManager _attachmentManager;

	@Reference
	private BackgroundTaskManager _backgroundTaskManager;

	@Reference
	private DepotEntryLocalService _depotEntryLocalService;

	@Reference
	private Language _language;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectEntryFolderLocalService _objectEntryFolderLocalService;

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

	@Reference
	private ObjectFieldLocalService _objectFieldLocalService;

	@Reference
	private Portal _portal;

}