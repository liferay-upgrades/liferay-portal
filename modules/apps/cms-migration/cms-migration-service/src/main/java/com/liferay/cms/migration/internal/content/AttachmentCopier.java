/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.internal.content;

import com.liferay.cms.migration.internal.context.CMSMigrationContext;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.object.field.attachment.AttachmentManager;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.Image;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ImageLocalService;
import com.liferay.portal.kernel.util.FileUtil;

import java.io.InputStream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Copies classic images into attachment fields of migrated object entries.
 *
 * @author Lino Alves
 */
@Component(service = AttachmentCopier.class)
public class AttachmentCopier {

	public long copy(
		CMSMigrationContext cmsMigrationContext,
		ObjectDefinition objectDefinition, String objectFieldName,
		String externalReferenceCode, long fileEntryId) {

		if (fileEntryId <= 0) {
			return 0;
		}

		try {
			FileEntry fileEntry = _dlAppLocalService.getFileEntry(fileEntryId);

			try (InputStream inputStream = fileEntry.getContentStream()) {
				return _copy(
					cmsMigrationContext, objectDefinition, objectFieldName,
					externalReferenceCode, FileUtil.getBytes(inputStream),
					fileEntry.getFileName());
			}
		}
		catch (Exception exception) {
			cmsMigrationContext.getCMSMigrationReport(
			).addWarning(
				StringBundler.concat(
					"Unable to copy the image ", fileEntryId, " to ",
					objectFieldName, ": ", exception.getMessage())
			);

			return 0;
		}
	}

	public long copyImage(
		CMSMigrationContext cmsMigrationContext,
		ObjectDefinition objectDefinition, String objectFieldName,
		String externalReferenceCode, long imageId) {

		if (imageId <= 0) {
			return 0;
		}

		try {
			Image image = _imageLocalService.getImage(imageId);

			return _copy(
				cmsMigrationContext, objectDefinition, objectFieldName,
				externalReferenceCode, image.getTextObj(),
				"image." + image.getType());
		}
		catch (Exception exception) {
			cmsMigrationContext.getCMSMigrationReport(
			).addWarning(
				StringBundler.concat(
					"Unable to copy the image ", imageId, " to ",
					objectFieldName, ": ", exception.getMessage())
			);

			return 0;
		}
	}

	private long _copy(
			CMSMigrationContext cmsMigrationContext,
			ObjectDefinition objectDefinition, String objectFieldName,
			String externalReferenceCode, byte[] bytes, String fileName)
		throws Exception {

		ObjectField objectField = _objectFieldLocalService.getObjectField(
			objectDefinition.getObjectDefinitionId(), objectFieldName);

		FileEntry fileEntry = _attachmentManager.getOrAddFileEntry(
			cmsMigrationContext.getCompanyId(), externalReferenceCode, bytes,
			fileName, cmsMigrationContext.getDepotGroupId(),
			objectField.getObjectFieldId(),
			cmsMigrationContext.getServiceContext());

		return fileEntry.getFileEntryId();
	}

	@Reference
	private AttachmentManager _attachmentManager;

	@Reference
	private DLAppLocalService _dlAppLocalService;

	@Reference
	private ImageLocalService _imageLocalService;

	@Reference
	private ObjectFieldLocalService _objectFieldLocalService;

}