/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.internal.content;

import com.liferay.cms.migration.constants.CMSMigrationConstants;
import com.liferay.cms.migration.internal.context.CMSMigrationContext;
import com.liferay.cms.migration.internal.structure.DDMFormValuesConverter;
import com.liferay.cms.migration.internal.structure.ObjectDefinitionMigrator;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFileEntryMetadata;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryMetadataLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.document.library.util.DLFileEntryTypeUtil;
import com.liferay.document.library.video.external.shortcut.DLVideoExternalShortcut;
import com.liferay.document.library.video.external.shortcut.resolver.DLVideoExternalShortcutResolver;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.storage.DDMStorageEngineManager;
import com.liferay.friendly.url.model.FriendlyURLEntry;
import com.liferay.friendly.url.service.FriendlyURLEntryLocalService;
import com.liferay.object.field.attachment.AttachmentManager;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.io.InputStream;
import java.io.Serializable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Migrates Documents and Media file entries. Regular files become entries of
 * the object definition that replaces their file entry type, and external
 * video shortcuts become CMS external videos.
 *
 * @author Lino Alves
 */
@Component(service = FileEntryMigrator.class)
public class FileEntryMigrator {

	/**
	 * Returns the attachment that replaces the file entry in the CMS. A file
	 * entry of the source group, or of any group while the source group's
	 * references are rewritten, is migrated first. A file entry that is
	 * already in the depot group is an attachment itself, so it has none.
	 */
	public FileEntry getAttachmentFileEntry(
			CMSMigrationContext cmsMigrationContext, FileEntry fileEntry)
		throws PortalException {

		if (fileEntry.getGroupId() == cmsMigrationContext.getDepotGroupId()) {
			return null;
		}

		ObjectEntry objectEntry = null;

		if (cmsMigrationContext.isRewriteSourceGroup() ||
			(fileEntry.getGroupId() ==
				cmsMigrationContext.getSourceGroupId())) {

			objectEntry = migrate(cmsMigrationContext, fileEntry);
		}
		else {
			objectEntry = cmsMigrationContext.getObjectEntry(
				DLFileEntry.class.getName(), fileEntry.getFileEntryId());
		}

		if (objectEntry == null) {
			return null;
		}

		Map<String, Serializable> values = objectEntry.getValues();

		long attachmentFileEntryId = GetterUtil.getLong(
			values.get(CMSMigrationConstants.OBJECT_FIELD_NAME_FILE));

		if (attachmentFileEntryId == 0) {
			return null;
		}

		return _dlAppLocalService.getFileEntry(attachmentFileEntryId);
	}

	public int getCount(CMSMigrationContext cmsMigrationContext) {
		return _dlFileEntryLocalService.getGroupFileEntriesCount(
			cmsMigrationContext.getSourceGroupId());
	}

	public ObjectEntry migrate(
			CMSMigrationContext cmsMigrationContext, FileEntry fileEntry)
		throws PortalException {

		ObjectEntry objectEntry = cmsMigrationContext.getObjectEntry(
			DLFileEntry.class.getName(), fileEntry.getFileEntryId());

		if (objectEntry != null) {
			return objectEntry;
		}

		FileVersion fileVersion = fileEntry.getFileVersion();

		if (Objects.equals(
				fileEntry.getMimeType(),
				ContentTypes.
					APPLICATION_VND_LIFERAY_VIDEO_EXTERNAL_SHORTCUT_HTML)) {

			return _migrateExternalVideo(
				cmsMigrationContext, fileEntry, fileVersion);
		}

		DLFileEntry dlFileEntry = (DLFileEntry)fileEntry.getModel();

		DLFileEntryType dlFileEntryType =
			_dlFileEntryTypeLocalService.fetchDLFileEntryType(
				dlFileEntry.getFileEntryTypeId());

		ObjectDefinition objectDefinition =
			_objectDefinitionMigrator.getOrAddDLFileEntryTypeObjectDefinition(
				cmsMigrationContext, dlFileEntryType);

		Map<String, Serializable> values = _getValues(
			cmsMigrationContext, fileEntry, fileVersion);

		if (dlFileEntryType != null) {
			for (DDMStructure ddmStructure :
					DLFileEntryTypeUtil.getDDMStructures(dlFileEntryType)) {

				values.putAll(
					_ddmFormValuesConverter.getValues(
						cmsMigrationContext,
						_getDDMFormValues(ddmStructure, fileVersion)));
			}
		}

		ObjectField objectField = _objectFieldLocalService.getObjectField(
			objectDefinition.getObjectDefinitionId(),
			CMSMigrationConstants.OBJECT_FIELD_NAME_FILE);

		FileEntry attachmentFileEntry;

		try (InputStream inputStream = fileVersion.getContentStream(false)) {
			attachmentFileEntry = _attachmentManager.getOrAddFileEntry(
				cmsMigrationContext.getCompanyId(),
				CMSMigrationConstants.
					EXTERNAL_REFERENCE_CODE_PREFIX_FILE_ENTRY + "FILE_" +
						fileEntry.getFileEntryId(),
				FileUtil.getBytes(inputStream), fileEntry.getFileName(),
				cmsMigrationContext.getDepotGroupId(),
				objectField.getObjectFieldId(),
				cmsMigrationContext.getServiceContext());
		}
		catch (PortalException portalException) {
			throw portalException;
		}
		catch (Exception exception) {
			throw new PortalException(exception);
		}

		values.put(
			CMSMigrationConstants.OBJECT_FIELD_NAME_FILE,
			attachmentFileEntry.getFileEntryId());

		objectEntry = _objectEntryWriter.write(
			cmsMigrationContext, DLFileEntry.class.getName(),
			fileEntry.getFileEntryId(), objectDefinition,
			_getObjectEntryFolderId(cmsMigrationContext, fileEntry),
			CMSMigrationConstants.EXTERNAL_REFERENCE_CODE_PREFIX_FILE_ENTRY +
				fileEntry.getFileEntryId(),
			fileEntry.getUserId(),
			LocaleUtil.toLanguageId(LocaleUtil.getSiteDefault()), values,
			_getFriendlyURLMap(fileEntry), fileEntry.getCreateDate(),
			fileEntry.getModifiedDate(),
			fileVersion.getStatus() == WorkflowConstants.STATUS_APPROVED);

		cmsMigrationContext.getCMSMigrationReport(
		).increment(
			CMSMigrationConstants.PHASE_FILE_ENTRIES
		);

		return objectEntry;
	}

	public void migrateAll(CMSMigrationContext cmsMigrationContext)
		throws PortalException {

		int count = getCount(cmsMigrationContext);

		for (int start = 0; start < count; start += _BATCH_SIZE) {
			List<DLFileEntry> dlFileEntries =
				_dlFileEntryLocalService.getGroupFileEntries(
					cmsMigrationContext.getSourceGroupId(), start,
					start + _BATCH_SIZE);

			for (DLFileEntry dlFileEntry : dlFileEntries) {
				if (dlFileEntry.getRepositoryId() !=
						cmsMigrationContext.getSourceGroupId()) {

					continue;
				}

				cmsMigrationContext.sendStatus(dlFileEntry.getTitle());

				try {
					migrate(
						cmsMigrationContext,
						_dlAppLocalService.getFileEntry(
							dlFileEntry.getFileEntryId()));
				}
				catch (Exception exception) {
					cmsMigrationContext.getCMSMigrationReport(
					).addError(
						StringBundler.concat(
							"Unable to migrate the document ",
							dlFileEntry.getTitle(), ": ",
							exception.getMessage())
					);
				}
			}
		}
	}

	private DDMFormValues _getDDMFormValues(
		DDMStructure ddmStructure, FileVersion fileVersion) {

		DLFileEntryMetadata dlFileEntryMetadata =
			_dlFileEntryMetadataLocalService.fetchFileEntryMetadata(
				ddmStructure.getStructureId(), fileVersion.getFileVersionId());

		if (dlFileEntryMetadata == null) {
			return null;
		}

		try {
			return _ddmStorageEngineManager.getDDMFormValues(
				dlFileEntryMetadata.getDDMStorageId(),
				ddmStructure.getDDMForm());
		}
		catch (PortalException portalException) {
			if (_log.isDebugEnabled()) {
				_log.debug(portalException);
			}

			return null;
		}
	}

	private Map<String, String> _getFriendlyURLMap(FileEntry fileEntry) {
		FriendlyURLEntry friendlyURLEntry =
			_friendlyURLEntryLocalService.fetchMainFriendlyURLEntry(
				_portal.getClassNameId(FileEntry.class.getName()),
				fileEntry.getFileEntryId());

		if (friendlyURLEntry == null) {
			return null;
		}

		return HashMapBuilder.put(
			LocaleUtil.toLanguageId(LocaleUtil.getSiteDefault()),
			friendlyURLEntry.getUrlTitle()
		).build();
	}

	private long _getObjectEntryFolderId(
		CMSMigrationContext cmsMigrationContext, FileEntry fileEntry) {

		long objectEntryFolderId = 0;

		if (fileEntry.getGroupId() == cmsMigrationContext.getSourceGroupId()) {
			objectEntryFolderId = cmsMigrationContext.getObjectEntryFolderId(
				DLFolder.class.getName(), fileEntry.getFolderId());
		}

		if (objectEntryFolderId == 0) {
			objectEntryFolderId = cmsMigrationContext.getObjectEntryFolderId(
				DLFolder.class.getName(),
				DLFolderConstants.DEFAULT_PARENT_FOLDER_ID);
		}

		return objectEntryFolderId;
	}

	private String _getTitle(
		CMSMigrationContext cmsMigrationContext, FileEntry fileEntry) {

		String title = fileEntry.getTitle();

		if (title.length() > _TITLE_MAX_LENGTH) {
			cmsMigrationContext.getCMSMigrationReport(
			).addWarning(
				"Truncated the title of the document " + title
			);

			title = title.substring(0, _TITLE_MAX_LENGTH);
		}

		return title;
	}

	private Map<String, Serializable> _getValues(
		CMSMigrationContext cmsMigrationContext, FileEntry fileEntry,
		FileVersion fileVersion) {

		String languageId = LocaleUtil.toLanguageId(
			LocaleUtil.getSiteDefault());

		return HashMapBuilder.<String, Serializable>put(
			CMSMigrationConstants.OBJECT_FIELD_NAME_DESCRIPTION + "_i18n",
			() -> {
				if (Validator.isNull(fileVersion.getDescription())) {
					return null;
				}

				return HashMapBuilder.put(
					languageId, fileVersion.getDescription()
				).build();
			}
		).put(
			CMSMigrationConstants.OBJECT_FIELD_NAME_TITLE + "_i18n",
			HashMapBuilder.put(
				languageId, _getTitle(cmsMigrationContext, fileEntry)
			).build()
		).build();
	}

	private ObjectEntry _migrateExternalVideo(
			CMSMigrationContext cmsMigrationContext, FileEntry fileEntry,
			FileVersion fileVersion)
		throws PortalException {

		DLVideoExternalShortcut dlVideoExternalShortcut =
			_dlVideoExternalShortcutResolver.resolve(fileVersion);

		String url = StringPool.BLANK;

		if (dlVideoExternalShortcut != null) {
			url = dlVideoExternalShortcut.getURL();
		}

		String languageId = LocaleUtil.toLanguageId(
			LocaleUtil.getSiteDefault());

		Map<String, Serializable> values =
			HashMapBuilder.<String, Serializable>put(
				CMSMigrationConstants.OBJECT_FIELD_NAME_TITLE + "_i18n",
				HashMapBuilder.put(
					languageId, _getTitle(cmsMigrationContext, fileEntry)
				).build()
			).put(
				CMSMigrationConstants.OBJECT_FIELD_NAME_VIDEO_URL + "_i18n",
				HashMapBuilder.put(
					languageId, url
				).build()
			).build();

		ObjectEntry objectEntry = _objectEntryWriter.write(
			cmsMigrationContext, DLFileEntry.class.getName(),
			fileEntry.getFileEntryId(),
			_objectDefinitionMigrator.getExternalVideoObjectDefinition(
				cmsMigrationContext),
			_getObjectEntryFolderId(cmsMigrationContext, fileEntry),
			CMSMigrationConstants.EXTERNAL_REFERENCE_CODE_PREFIX_FILE_ENTRY +
				fileEntry.getFileEntryId(),
			fileEntry.getUserId(), languageId, values,
			_getFriendlyURLMap(fileEntry), fileEntry.getCreateDate(),
			fileEntry.getModifiedDate(),
			fileVersion.getStatus() == WorkflowConstants.STATUS_APPROVED);

		cmsMigrationContext.getCMSMigrationReport(
		).increment(
			CMSMigrationConstants.PHASE_FILE_ENTRIES
		);

		return objectEntry;
	}

	private static final int _BATCH_SIZE = 100;

	private static final int _TITLE_MAX_LENGTH = 280;

	private static final Log _log = LogFactoryUtil.getLog(
		FileEntryMigrator.class);

	@Reference
	private AttachmentManager _attachmentManager;

	@Reference
	private DDMFormValuesConverter _ddmFormValuesConverter;

	@Reference
	private DDMStorageEngineManager _ddmStorageEngineManager;

	@Reference
	private DLAppLocalService _dlAppLocalService;

	@Reference
	private DLFileEntryLocalService _dlFileEntryLocalService;

	@Reference
	private DLFileEntryMetadataLocalService _dlFileEntryMetadataLocalService;

	@Reference
	private DLFileEntryTypeLocalService _dlFileEntryTypeLocalService;

	@Reference
	private DLVideoExternalShortcutResolver _dlVideoExternalShortcutResolver;

	@Reference
	private FriendlyURLEntryLocalService _friendlyURLEntryLocalService;

	@Reference
	private ObjectDefinitionMigrator _objectDefinitionMigrator;

	@Reference
	private ObjectEntryWriter _objectEntryWriter;

	@Reference
	private ObjectFieldLocalService _objectFieldLocalService;

	@Reference
	private Portal _portal;

}