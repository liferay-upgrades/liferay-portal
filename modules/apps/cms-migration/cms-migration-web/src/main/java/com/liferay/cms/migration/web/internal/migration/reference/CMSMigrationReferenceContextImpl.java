/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.migration.reference;

import com.liferay.cms.migration.reference.CMSMigrationReferenceContext;
import com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskStatusMessageSender;
import com.liferay.cms.migration.web.internal.migration.CMSMigrationContext;
import com.liferay.cms.migration.web.internal.migration.structure.CMSMigrationDDMFormFieldUtil;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Exposes a migration context to the reference rewriters that other modules
 * register.
 *
 * @author Lino Alves
 */
public class CMSMigrationReferenceContextImpl
	implements CMSMigrationReferenceContext {

	public CMSMigrationReferenceContextImpl(
		CMSMigrationBackgroundTaskStatusMessageSender
			cmsMigrationBackgroundTaskStatusMessageSender,
		CMSMigrationContext cmsMigrationContext,
		DDMStructureLocalService ddmStructureLocalService,
		ObjectFieldLocalService objectFieldLocalService) {

		_cmsMigrationBackgroundTaskStatusMessageSender =
			cmsMigrationBackgroundTaskStatusMessageSender;
		_cmsMigrationContext = cmsMigrationContext;
		_ddmStructureLocalService = ddmStructureLocalService;
		_objectFieldLocalService = objectFieldLocalService;
	}

	@Override
	public void addCollectionProviderKey(
		String key, String newKey, String className, long classTypeId) {

		DDMStructure ddmStructure = null;

		if (Objects.equals(className, JournalArticle.class.getName())) {
			ddmStructure = _ddmStructureLocalService.fetchDDMStructure(
				classTypeId);
		}

		_cmsMigrationContext.addCollectionProviderKey(
			key, newKey,
			new ReferenceScope(
				ddmStructure,
				_cmsMigrationContext.getObjectDefinition(
					className, classTypeId),
				className));
	}

	@Override
	public void addError(String error) {
		_cmsMigrationContext.getCMSMigrationReport(
		).addError(
			error
		);
	}

	@Override
	public void addWarning(String warning) {
		_cmsMigrationContext.getCMSMigrationReport(
		).addWarning(
			warning
		);
	}

	@Override
	public long getCompanyId() {
		return _cmsMigrationContext.getCompanyId();
	}

	@Override
	public Group getDepotGroup() {
		return _cmsMigrationContext.getDepotGroup();
	}

	@Override
	public Map<Long, ObjectDefinition> getObjectDefinitions(String className) {
		Map<Long, ObjectDefinition> objectDefinitions = new TreeMap<>();

		String prefix = className + StringPool.POUND;

		for (Map.Entry<String, ObjectDefinition> entry :
				_cmsMigrationContext.getObjectDefinitions(
				).entrySet()) {

			String key = entry.getKey();

			if (key.startsWith(prefix)) {
				objectDefinitions.put(
					GetterUtil.getLong(key.substring(prefix.length())),
					entry.getValue());
			}
		}

		return objectDefinitions;
	}

	@Override
	public String getObjectFieldName(
		long ddmStructureId, String fieldReference) {

		ObjectDefinition objectDefinition =
			_cmsMigrationContext.getObjectDefinition(
				JournalArticle.class.getName(), ddmStructureId);

		if (objectDefinition == null) {
			return null;
		}

		DDMStructure ddmStructure = _ddmStructureLocalService.fetchDDMStructure(
			ddmStructureId);

		if (ddmStructure == null) {
			return null;
		}

		DDMFormField ddmFormField = null;

		try {
			ddmFormField = ddmStructure.getDDMFormFieldByFieldReference(
				fieldReference);
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}

			return null;
		}

		if (ddmFormField == null) {
			return null;
		}

		String objectFieldName =
			CMSMigrationDDMFormFieldUtil.getObjectFieldName(ddmFormField);

		ObjectField objectField = _objectFieldLocalService.fetchObjectField(
			objectDefinition.getObjectDefinitionId(), objectFieldName);

		if (objectField == null) {
			return null;
		}

		return objectFieldName;
	}

	@Override
	public Group getSourceGroup() {
		return _cmsMigrationContext.getSourceGroup();
	}

	@Override
	public long getUserId() {
		return _cmsMigrationContext.getUserId();
	}

	@Override
	public void incrementCount(String key) {
		_cmsMigrationContext.getCMSMigrationReport(
		).increment(
			key
		);
	}

	@Override
	public void sendPhase(String phase, long total) {
		_cmsMigrationBackgroundTaskStatusMessageSender.sendPhaseStatusMessage(
			phase, total);
	}

	@Override
	public void sendStatus(String itemTitle) {
		_cmsMigrationContext.sendStatus(itemTitle);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CMSMigrationReferenceContextImpl.class);

	private final CMSMigrationBackgroundTaskStatusMessageSender
		_cmsMigrationBackgroundTaskStatusMessageSender;
	private final CMSMigrationContext _cmsMigrationContext;
	private final DDMStructureLocalService _ddmStructureLocalService;
	private final ObjectFieldLocalService _objectFieldLocalService;

}