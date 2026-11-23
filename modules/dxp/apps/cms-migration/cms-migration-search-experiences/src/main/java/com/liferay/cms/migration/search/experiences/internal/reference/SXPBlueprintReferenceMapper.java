/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.search.experiences.internal.reference;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.cms.migration.reference.CMSMigrationReferenceContext;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates the references that a blueprint stores to classic content types,
 * structures, and structure fields into references to the object definitions
 * and object fields that replace them in a migration.
 *
 * @author Lino Alves
 */
public class SXPBlueprintReferenceMapper {

	public SXPBlueprintReferenceMapper(
		CMSMigrationReferenceContext cmsMigrationReferenceContext,
		DDMStructureLocalService ddmStructureLocalService,
		DLFileEntryTypeLocalService dlFileEntryTypeLocalService,
		GroupLocalService groupLocalService,
		ObjectFieldLocalService objectFieldLocalService) {

		_cmsMigrationReferenceContext = cmsMigrationReferenceContext;
		_groupLocalService = groupLocalService;
		_objectFieldLocalService = objectFieldLocalService;

		for (String className : _CLASS_NAMES) {
			_objectDefinitionsMap.put(
				className,
				cmsMigrationReferenceContext.getObjectDefinitions(className));
		}

		for (Map.Entry<Long, ObjectDefinition> entry :
				_getObjectDefinitions(
					JournalArticle.class.getName()
				).entrySet()) {

			DDMStructure ddmStructure =
				ddmStructureLocalService.fetchDDMStructure(entry.getKey());

			if (ddmStructure == null) {
				continue;
			}

			_addSubtype(
				JournalArticle.class.getName(), entry.getKey(),
				ddmStructure.getGroupId(),
				ddmStructure.getExternalReferenceCode());

			List<ObjectDefinition> objectDefinitions =
				_ddmStructureKeyObjectDefinitions.computeIfAbsent(
					ddmStructure.getStructureKey(), key -> new ArrayList<>());

			objectDefinitions.add(entry.getValue());
		}

		for (long fileEntryTypeId :
				_getObjectDefinitions(
					DLFileEntry.class.getName()
				).keySet()) {

			DLFileEntryType dlFileEntryType =
				dlFileEntryTypeLocalService.fetchDLFileEntryType(
					fileEntryTypeId);

			if (dlFileEntryType != null) {
				_addSubtype(
					DLFileEntry.class.getName(), fileEntryTypeId,
					dlFileEntryType.getGroupId(),
					dlFileEntryType.getExternalReferenceCode());
			}
		}
	}

	/**
	 * Returns the class names of the object definitions that replace a
	 * searchable asset type, which is a class name, optionally followed by
	 * the external reference codes of a group and a subtype, separated by
	 * <code>&amp;&amp;</code>.
	 */
	public List<String> getClassNames(String searchableAssetType) {
		if (Validator.isNull(searchableAssetType)) {
			return Collections.emptyList();
		}

		List<String> classNames = new ArrayList<>();

		if (searchableAssetType.contains(_SEPARATOR)) {
			Long classTypeId = _subtypeClassTypeIds.get(searchableAssetType);

			if (classTypeId == null) {
				return classNames;
			}

			ObjectDefinition objectDefinition = _getObjectDefinitions(
				StringUtil.extractFirst(searchableAssetType, _SEPARATOR)
			).get(
				classTypeId
			);

			classNames.add(objectDefinition.getClassName());

			return classNames;
		}

		for (ObjectDefinition objectDefinition :
				_getObjectDefinitions(
					searchableAssetType
				).values()) {

			classNames.add(objectDefinition.getClassName());
		}

		return classNames;
	}

	/**
	 * Returns the class type ID of the only object definition that replaces a
	 * searchable asset type, or <code>-1</code> if no object definition or
	 * several replace it.
	 */
	public long getClassTypeId(String searchableAssetType) {
		Long classTypeId = _subtypeClassTypeIds.get(searchableAssetType);

		if (classTypeId != null) {
			return classTypeId;
		}

		Map<Long, ObjectDefinition> objectDefinitions = _getObjectDefinitions(
			searchableAssetType);

		if (objectDefinitions.size() != 1) {
			return -1;
		}

		Set<Long> classTypeIds = objectDefinitions.keySet();

		Iterator<Long> iterator = classTypeIds.iterator();

		return iterator.next();
	}

	public String getDepotGroupExternalReferenceCode() {
		Group group = _cmsMigrationReferenceContext.getDepotGroup();

		return group.getExternalReferenceCode();
	}

	public List<Long> getObjectDefinitionIds(String ddmStructureKey) {
		List<Long> objectDefinitionIds = new ArrayList<>();

		for (ObjectDefinition objectDefinition :
				_ddmStructureKeyObjectDefinitions.getOrDefault(
					ddmStructureKey, Collections.emptyList())) {

			objectDefinitionIds.add(objectDefinition.getObjectDefinitionId());
		}

		return objectDefinitionIds;
	}

	public String getObjectFieldName(
		long ddmStructureId, String fieldReference) {

		return _cmsMigrationReferenceContext.getObjectFieldName(
			ddmStructureId, fieldReference);
	}

	public String getSourceGroupExternalReferenceCode() {
		Group group = _cmsMigrationReferenceContext.getSourceGroup();

		return group.getExternalReferenceCode();
	}

	/**
	 * Returns the name of the field of the nested field array that holds the
	 * values of the object field in the search index, or <code>null</code> if
	 * the object field is not indexed.
	 *
	 * @param languageId the language of the value, or <code>null</code> if
	 *        the value is not localized
	 */
	public String getValueFieldName(
		long ddmStructureId, String objectFieldName, String languageId) {

		ObjectDefinition objectDefinition = _getObjectDefinitions(
			JournalArticle.class.getName()
		).get(
			ddmStructureId
		);

		if (objectDefinition == null) {
			return null;
		}

		ObjectField objectField = _objectFieldLocalService.fetchObjectField(
			objectDefinition.getObjectDefinitionId(), objectFieldName);

		if ((objectField == null) || !objectField.isIndexed()) {
			return null;
		}

		if (objectField.isIndexedAsKeyword()) {
			return "value_keyword";
		}

		String dbType = objectField.getDBType();

		if (dbType.equals(ObjectFieldConstants.DB_TYPE_BIG_DECIMAL) ||
			dbType.equals(ObjectFieldConstants.DB_TYPE_DOUBLE)) {

			return "value_double";
		}

		if (dbType.equals(ObjectFieldConstants.DB_TYPE_BOOLEAN)) {
			return "value_boolean";
		}

		if (dbType.equals(ObjectFieldConstants.DB_TYPE_DATE) ||
			dbType.equals(ObjectFieldConstants.DB_TYPE_DATE_TIME)) {

			return "value_date";
		}

		if (dbType.equals(ObjectFieldConstants.DB_TYPE_INTEGER)) {
			return "value_integer";
		}

		if (dbType.equals(ObjectFieldConstants.DB_TYPE_LONG)) {
			return "value_long";
		}

		if (!dbType.equals(ObjectFieldConstants.DB_TYPE_CLOB) &&
			!dbType.equals(ObjectFieldConstants.DB_TYPE_STRING)) {

			return null;
		}

		if (Validator.isBlank(objectField.getIndexedLanguageId())) {
			return "value_text";
		}

		if (objectField.isLocalized() && Validator.isNotNull(languageId)) {
			return "value_" + languageId;
		}

		return "value_" + objectField.getIndexedLanguageId();
	}

	public boolean hasObjectDefinitions() {
		for (Map<Long, ObjectDefinition> objectDefinitions :
				_objectDefinitionsMap.values()) {

			if (!objectDefinitions.isEmpty()) {
				return true;
			}
		}

		return false;
	}

	public boolean isMigratedDDMStructure(long ddmStructureId) {
		return _getObjectDefinitions(
			JournalArticle.class.getName()
		).containsKey(
			ddmStructureId
		);
	}

	private void _addSubtype(
		String className, long classTypeId, long groupId,
		String subtypeExternalReferenceCode) {

		Group group = _groupLocalService.fetchGroup(groupId);

		if (group == null) {
			return;
		}

		_subtypeClassTypeIds.put(
			StringBundler.concat(
				className, _SEPARATOR, group.getExternalReferenceCode(),
				_SEPARATOR, subtypeExternalReferenceCode),
			classTypeId);
	}

	private Map<Long, ObjectDefinition> _getObjectDefinitions(
		String className) {

		return _objectDefinitionsMap.getOrDefault(
			className, Collections.emptyMap());
	}

	private static final String[] _CLASS_NAMES = {
		BlogsEntry.class.getName(), DLFileEntry.class.getName(),
		JournalArticle.class.getName()
	};

	private static final String _SEPARATOR = "&&";

	private final CMSMigrationReferenceContext _cmsMigrationReferenceContext;
	private final Map<String, List<ObjectDefinition>>
		_ddmStructureKeyObjectDefinitions = new HashMap<>();
	private final GroupLocalService _groupLocalService;
	private final Map<String, Map<Long, ObjectDefinition>>
		_objectDefinitionsMap = new HashMap<>();
	private final ObjectFieldLocalService _objectFieldLocalService;
	private final Map<String, Long> _subtypeClassTypeIds = new HashMap<>();

}