/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.search.experiences.internal.reference;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.journal.model.JournalArticle;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites the configuration and the element instances of a blueprint, as
 * JSON, so the blueprint also selects the object entries that replace classic
 * content. Every rewrite adds the migrated content next to the classic
 * content, because a blueprint serves every site of the company and the
 * classic content stays in place.
 *
 * <p>
 * Exclusions are left as they are: a classic type in a <code>must_not</code>
 * clause often guards a condition that only classic content meets, such as
 * the head version of Web Content, so adding the migrated content to it would
 * hide the migrated content.
 * </p>
 *
 * <p>
 * Queries that combine a classic clause with its translation are marked with
 * the <code>CMS_MIGRATION</code> query name, so a later migration does not
 * translate them twice.
 * </p>
 *
 * @author Lino Alves
 */
public class SXPBlueprintJSONRewriter {

	public SXPBlueprintJSONRewriter(
		JSONFactory jsonFactory,
		SXPBlueprintReferenceMapper sxpBlueprintReferenceMapper) {

		_jsonFactory = jsonFactory;
		_sxpBlueprintReferenceMapper = sxpBlueprintReferenceMapper;
	}

	/**
	 * Rewrites the configuration in place.
	 *
	 * @param replaceSearchableAssetTypes whether the object definitions
	 *        replace the classic searchable asset types instead of joining
	 *        them, as a collection provider that lists object entries
	 *        requires
	 * @param problems the descriptions of the references that could not be
	 *        rewritten
	 */
	public void rewriteConfiguration(
			JSONObject configurationJSONObject,
			boolean replaceSearchableAssetTypes, Set<String> problems)
		throws JSONException {

		if (configurationJSONObject == null) {
			return;
		}

		JSONObject generalConfigurationJSONObject =
			configurationJSONObject.getJSONObject("generalConfiguration");

		if (generalConfigurationJSONObject != null) {
			_rewriteClauseContributors(
				generalConfigurationJSONObject, "clauseContributorsExcludes");
			_rewriteClauseContributors(
				generalConfigurationJSONObject, "clauseContributorsIncludes");
			_rewriteScope(generalConfigurationJSONObject);
			_rewriteSearchableAssetTypes(
				generalConfigurationJSONObject, replaceSearchableAssetTypes);
		}

		for (String key : configurationJSONObject.keySet()) {
			if (!key.equals("generalConfiguration")) {
				_rewriteQueries(configurationJSONObject.opt(key), problems);
			}
		}
	}

	/**
	 * Rewrites the element instances in place. An element instance whose
	 * configuration values select a classic asset type is duplicated for
	 * each object definition that replaces the type, when the element only
	 * boosts results.
	 *
	 * @param problems the descriptions of the references that could not be
	 *        rewritten
	 */
	public void rewriteElementInstances(
			JSONArray elementInstancesJSONArray, Set<String> problems)
		throws JSONException {

		if (elementInstancesJSONArray == null) {
			return;
		}

		List<JSONObject> elementInstanceJSONObjects = new ArrayList<>();

		for (int i = 0; i < elementInstancesJSONArray.length(); i++) {
			JSONObject elementInstanceJSONObject =
				elementInstancesJSONArray.getJSONObject(i);

			if (elementInstanceJSONObject != null) {
				elementInstanceJSONObjects.add(elementInstanceJSONObject);
			}
		}

		Set<String> elementInstanceJSONs = new LinkedHashSet<>();

		for (JSONObject elementInstanceJSONObject :
				elementInstanceJSONObjects) {

			elementInstanceJSONs.add(elementInstanceJSONObject.toString());
		}

		for (JSONObject elementInstanceJSONObject :
				elementInstanceJSONObjects) {

			JSONObject configurationEntryJSONObject =
				elementInstanceJSONObject.getJSONObject("configurationEntry");

			rewriteConfiguration(configurationEntryJSONObject, false, problems);

			JSONObject elementDefinitionConfigurationJSONObject =
				_getElementDefinitionConfigurationJSONObject(
					elementInstanceJSONObject);

			rewriteConfiguration(
				elementDefinitionConfigurationJSONObject, false, problems);

			JSONObject uiConfigurationValuesJSONObject =
				elementInstanceJSONObject.getJSONObject(
					"uiConfigurationValues");

			if (uiConfigurationValuesJSONObject == null) {
				continue;
			}

			_rewriteQueries(uiConfigurationValuesJSONObject, problems);

			if (configurationEntryJSONObject != null) {
				continue;
			}

			for (String key : uiConfigurationValuesJSONObject.keySet()) {
				Object value = uiConfigurationValuesJSONObject.opt(key);

				if (!(value instanceof String)) {
					continue;
				}

				List<String> classNames =
					_sxpBlueprintReferenceMapper.getClassNames((String)value);

				if (classNames.isEmpty()) {
					continue;
				}

				if (!_isBoostOnly(elementDefinitionConfigurationJSONObject)) {
					problems.add(
						StringBundler.concat(
							"The element ",
							_getElementTitle(elementInstanceJSONObject),
							" selects the asset type ", value,
							", but it filters results, so it cannot select ",
							"the migrated content too"));

					continue;
				}

				for (String className : classNames) {
					JSONObject newElementInstanceJSONObject =
						_jsonFactory.createJSONObject(
							elementInstanceJSONObject.toString());

					JSONObject newUIConfigurationValuesJSONObject =
						newElementInstanceJSONObject.getJSONObject(
							"uiConfigurationValues");

					newUIConfigurationValuesJSONObject.put(key, className);

					if (elementInstanceJSONs.add(
							newElementInstanceJSONObject.toString())) {

						elementInstancesJSONArray.put(
							newElementInstanceJSONObject);
					}
				}
			}
		}
	}

	private void _checkFieldReference(String value, Set<String> problems) {
		Matcher matcher = _ddmFieldNamePattern.matcher(value);

		while (matcher.find()) {
			if (_sxpBlueprintReferenceMapper.isMigratedDDMStructure(
					GetterUtil.getLong(matcher.group(2)))) {

				problems.add(
					StringBundler.concat(
						"The field ", value,
						" belongs to a migrated structure, but the clause ",
						"that uses it could not be rewritten"));
			}
		}
	}

	private void _checkScopeGroupReference(
		String key, Object value, Set<String> problems) {

		if (!key.equals("scopeGroupId")) {
			return;
		}

		if (value instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject)value;

			value = jsonObject.opt("value");
		}

		if (!(value instanceof String)) {
			return;
		}

		String string = (String)value;

		if (string.startsWith(StringPool.DOLLAR_AND_OPEN_CURLY_BRACE)) {
			problems.add(
				"A clause limits results to the scope " + value +
					", which does not include the space");
		}
	}

	private DDMFieldName _getDDMFieldName(String value) {
		Matcher matcher = _ddmFieldNamePattern.matcher(value);

		if (!matcher.matches()) {
			return null;
		}

		String fieldReference = matcher.group(3);
		String languageId = null;

		Matcher languageIdMatcher = _languageIdPattern.matcher(fieldReference);

		if (languageIdMatcher.find()) {
			fieldReference = fieldReference.substring(
				0, languageIdMatcher.start());
			languageId = languageIdMatcher.group(1);
		}

		return new DDMFieldName(
			GetterUtil.getLong(matcher.group(2)), fieldReference, languageId);
	}

	private void _getDDMFieldNames(
		Object value, List<DDMFieldName> ddmFieldNames) {

		if (value instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)value;

			for (int i = 0; i < jsonArray.length(); i++) {
				_getDDMFieldNames(jsonArray.get(i), ddmFieldNames);
			}
		}
		else if (value instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject)value;

			for (String key : jsonObject.keySet()) {
				_getDDMFieldNames(jsonObject.opt(key), ddmFieldNames);
			}
		}
		else if (value instanceof String) {
			DDMFieldName ddmFieldName = _getDDMFieldName((String)value);

			if (ddmFieldName != null) {
				ddmFieldNames.add(ddmFieldName);
			}
		}
	}

	private JSONObject _getElementDefinitionConfigurationJSONObject(
		JSONObject elementInstanceJSONObject) {

		JSONObject sxpElementJSONObject =
			elementInstanceJSONObject.getJSONObject("sxpElement");

		if (sxpElementJSONObject == null) {
			return null;
		}

		JSONObject elementDefinitionJSONObject =
			sxpElementJSONObject.getJSONObject("elementDefinition");

		if (elementDefinitionJSONObject == null) {
			return null;
		}

		return elementDefinitionJSONObject.getJSONObject("configuration");
	}

	private String _getElementTitle(JSONObject elementInstanceJSONObject) {
		JSONObject sxpElementJSONObject =
			elementInstanceJSONObject.getJSONObject("sxpElement");

		if (sxpElementJSONObject == null) {
			return StringPool.BLANK;
		}

		return sxpElementJSONObject.getString(
			"externalReferenceCode", sxpElementJSONObject.getString("title"));
	}

	private void _getOccurs(Object value, Set<String> occurs) {
		if (value instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)value;

			for (int i = 0; i < jsonArray.length(); i++) {
				_getOccurs(jsonArray.get(i), occurs);
			}
		}
		else if (value instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject)value;

			for (String key : jsonObject.keySet()) {
				Object childValue = jsonObject.opt(key);

				if (key.equals("occur")) {
					occurs.add(String.valueOf(childValue));
				}
				else {
					_getOccurs(childValue, occurs);
				}
			}
		}
	}

	private List<String> _getStrings(Object value) {
		List<String> strings = new ArrayList<>();

		if (value instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject)value;

			value = jsonObject.opt("value");
		}

		if (value instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)value;

			for (int i = 0; i < jsonArray.length(); i++) {
				strings.add(jsonArray.getString(i));
			}
		}
		else if (value instanceof String) {
			strings.add((String)value);
		}

		return strings;
	}

	private boolean _isBoostOnly(JSONObject configurationJSONObject) {
		if (configurationJSONObject == null) {
			return false;
		}

		Set<String> occurs = new LinkedHashSet<>();

		_getOccurs(configurationJSONObject, occurs);

		if (occurs.isEmpty()) {
			return false;
		}

		for (String occur : occurs) {
			if (!occur.equals("should")) {
				return false;
			}
		}

		return true;
	}

	private boolean _isMigrationQuery(JSONObject jsonObject) {
		JSONObject boolJSONObject = jsonObject.getJSONObject("bool");

		if ((boolJSONObject != null) &&
			Objects.equals(boolJSONObject.getString("_name"), _QUERY_NAME)) {

			return true;
		}

		return false;
	}

	private void _rewriteClauseContributors(
		JSONObject generalConfigurationJSONObject, String key) {

		JSONArray jsonArray = generalConfigurationJSONObject.getJSONArray(key);

		if (jsonArray == null) {
			return;
		}

		Set<String> clauseContributors = new LinkedHashSet<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			String clauseContributor = jsonArray.getString(i);

			clauseContributors.add(clauseContributor);

			String simpleClassName = clauseContributor.substring(
				clauseContributor.lastIndexOf(StringPool.PERIOD) + 1);

			for (Map.Entry<String, String> entry :
					_clauseContributorClassNames.entrySet()) {

				String prefix = entry.getKey();

				if (!simpleClassName.startsWith(prefix)) {
					continue;
				}

				String suffix = simpleClassName.substring(prefix.length());

				if ((suffix.equals("KeywordQueryContributor") ||
					 suffix.equals("ModelPreFilterContributor")) &&
					!_sxpBlueprintReferenceMapper.getClassNames(
						entry.getValue()
					).isEmpty()) {

					clauseContributors.add(
						_OBJECT_ENTRY_CLAUSE_CONTRIBUTOR_PREFIX + suffix);
				}
			}
		}

		if (clauseContributors.size() != jsonArray.length()) {
			generalConfigurationJSONObject.put(
				key, _jsonFactory.createJSONArray(clauseContributors));
		}
	}

	/**
	 * Combines a nested query on the structure fields of Web Content with the
	 * same query on the object fields that replace them.
	 *
	 * @return whether the query is a nested query on structure fields
	 */
	private boolean _rewriteDDMFieldArrayQuery(
			JSONObject queryJSONObject, Set<String> problems)
		throws JSONException {

		JSONObject nestedJSONObject = queryJSONObject.getJSONObject("nested");

		if ((nestedJSONObject == null) ||
			!Objects.equals(
				nestedJSONObject.getString("path"), "ddmFieldArray")) {

			return false;
		}

		List<DDMFieldName> ddmFieldNames = new ArrayList<>();

		_getDDMFieldNames(nestedJSONObject, ddmFieldNames);

		Set<String> fieldNames = new LinkedHashSet<>();
		boolean migrated = false;

		for (DDMFieldName ddmFieldName : ddmFieldNames) {
			fieldNames.add(ddmFieldName.toString());

			if (_sxpBlueprintReferenceMapper.isMigratedDDMStructure(
					ddmFieldName._ddmStructureId)) {

				migrated = true;
			}
		}

		if (!migrated) {
			return true;
		}

		if (fieldNames.size() > 1) {
			problems.add(
				"A clause queries the structure fields " + fieldNames +
					" together, which cannot be rewritten");

			return true;
		}

		DDMFieldName ddmFieldName = ddmFieldNames.get(0);

		String objectFieldName =
			_sxpBlueprintReferenceMapper.getObjectFieldName(
				ddmFieldName._ddmStructureId, ddmFieldName._fieldReference);

		String valueFieldName = null;

		if (objectFieldName != null) {
			valueFieldName = _sxpBlueprintReferenceMapper.getValueFieldName(
				ddmFieldName._ddmStructureId, objectFieldName,
				ddmFieldName._languageId);
		}

		if (valueFieldName == null) {
			problems.add(
				StringBundler.concat(
					"The structure field ", ddmFieldName._fieldReference,
					" has no indexed object field counterpart, so the clause ",
					"that queries it could not be rewritten"));

			return true;
		}

		JSONObject newNestedJSONObject = _jsonFactory.createJSONObject(
			nestedJSONObject.toString());

		if (!_translateDDMFieldArrayQuery(
				newNestedJSONObject, null, objectFieldName, valueFieldName,
				problems)) {

			return true;
		}

		newNestedJSONObject.put("path", "nestedFieldArray");

		_wrap(queryJSONObject, JSONUtil.put("nested", newNestedJSONObject));

		return true;
	}

	/**
	 * Combines a term query on the structure key of Web Content with a query
	 * on the object definitions that replace the structures.
	 *
	 * @return whether the query is a term query on structure keys
	 */
	private boolean _rewriteDDMStructureKeyQuery(
			JSONObject queryJSONObject, Set<String> problems)
		throws JSONException {

		for (String type : new String[] {"match", "term", "terms"}) {
			JSONObject typeJSONObject = queryJSONObject.getJSONObject(type);

			if ((typeJSONObject == null) ||
				!typeJSONObject.has("ddmStructureKey")) {

				continue;
			}

			Set<Long> objectDefinitionIds = new LinkedHashSet<>();

			for (String ddmStructureKey :
					_getStrings(typeJSONObject.opt("ddmStructureKey"))) {

				objectDefinitionIds.addAll(
					_sxpBlueprintReferenceMapper.getObjectDefinitionIds(
						ddmStructureKey));
			}

			if (objectDefinitionIds.isEmpty()) {
				return true;
			}

			if (type.equals("match")) {
				problems.add(
					"A match query on the structure key of migrated " +
						"structures could not be rewritten");

				return true;
			}

			_wrap(
				queryJSONObject,
				JSONUtil.put(
					"terms",
					JSONUtil.put(
						"objectDefinitionId",
						_jsonFactory.createJSONArray(objectDefinitionIds))));

			return true;
		}

		return false;
	}

	private void _rewriteEntryClassNameQuery(JSONObject queryJSONObject) {
		JSONObject termJSONObject = queryJSONObject.getJSONObject("term");

		if ((termJSONObject != null) && termJSONObject.has("entryClassName")) {
			Object value = termJSONObject.opt("entryClassName");

			Object boost = null;

			if (value instanceof JSONObject) {
				JSONObject valueJSONObject = (JSONObject)value;

				boost = valueJSONObject.opt("boost");
				value = valueJSONObject.opt("value");
			}

			if (!(value instanceof String)) {
				return;
			}

			List<String> classNames =
				_sxpBlueprintReferenceMapper.getClassNames((String)value);

			if (classNames.isEmpty()) {
				return;
			}

			Set<String> newClassNames = new LinkedHashSet<>();

			newClassNames.add((String)value);
			newClassNames.addAll(classNames);

			JSONObject termsJSONObject = JSONUtil.put(
				"entryClassName", _jsonFactory.createJSONArray(newClassNames));

			if (boost != null) {
				termsJSONObject.put("boost", boost);
			}

			queryJSONObject.remove("term");

			queryJSONObject.put("terms", termsJSONObject);

			return;
		}

		JSONObject termsJSONObject = queryJSONObject.getJSONObject("terms");

		if (termsJSONObject == null) {
			return;
		}

		JSONArray jsonArray = termsJSONObject.getJSONArray("entryClassName");

		if (jsonArray == null) {
			return;
		}

		Set<String> classNames = new LinkedHashSet<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			String className = jsonArray.getString(i);

			classNames.add(className);
			classNames.addAll(
				_sxpBlueprintReferenceMapper.getClassNames(className));
		}

		if (classNames.size() != jsonArray.length()) {
			termsJSONObject.put(
				"entryClassName", _jsonFactory.createJSONArray(classNames));
		}
	}

	private void _rewriteQueries(Object value, Set<String> problems)
		throws JSONException {

		if (value instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)value;

			for (int i = 0; i < jsonArray.length(); i++) {
				_rewriteQueries(jsonArray.get(i), problems);
			}

			return;
		}

		if (value instanceof String) {
			_checkFieldReference((String)value, problems);

			return;
		}

		if (!(value instanceof JSONObject)) {
			return;
		}

		JSONObject jsonObject = (JSONObject)value;

		if (_isMigrationQuery(jsonObject) ||
			_rewriteDDMFieldArrayQuery(jsonObject, problems) ||
			_rewriteDDMStructureKeyQuery(jsonObject, problems)) {

			return;
		}

		_rewriteEntryClassNameQuery(jsonObject);

		for (String key : new ArrayList<>(jsonObject.keySet())) {
			if (key.equals("must_not") ||
				(key.equals("query") &&
				 Objects.equals(jsonObject.getString("occur"), "must_not"))) {

				continue;
			}

			Object childValue = jsonObject.opt(key);

			_checkFieldReference(key, problems);
			_checkScopeGroupReference(key, childValue, problems);

			_rewriteQueries(childValue, problems);
		}
	}

	private void _rewriteScope(JSONObject generalConfigurationJSONObject) {
		JSONArray jsonArray = generalConfigurationJSONObject.getJSONArray(
			"scope");

		if (jsonArray == null) {
			return;
		}

		Set<String> scope = new LinkedHashSet<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			scope.add(jsonArray.getString(i));
		}

		if (scope.contains(
				_sxpBlueprintReferenceMapper.
					getSourceGroupExternalReferenceCode()) &&
			scope.add(
				_sxpBlueprintReferenceMapper.
					getDepotGroupExternalReferenceCode())) {

			generalConfigurationJSONObject.put(
				"scope", _jsonFactory.createJSONArray(scope));
		}
	}

	private void _rewriteSearchableAssetTypes(
		JSONObject generalConfigurationJSONObject,
		boolean replaceSearchableAssetTypes) {

		JSONArray jsonArray = generalConfigurationJSONObject.getJSONArray(
			"searchableAssetTypes");

		if (jsonArray == null) {
			return;
		}

		Set<String> searchableAssetTypes = new LinkedHashSet<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			String searchableAssetType = jsonArray.getString(i);

			List<String> classNames =
				_sxpBlueprintReferenceMapper.getClassNames(searchableAssetType);

			if (!replaceSearchableAssetTypes || classNames.isEmpty()) {
				searchableAssetTypes.add(searchableAssetType);
			}

			searchableAssetTypes.addAll(classNames);
		}

		generalConfigurationJSONObject.put(
			"searchableAssetTypes",
			_jsonFactory.createJSONArray(searchableAssetTypes));
	}

	private Object _toLowerCase(Object value) {
		if (value instanceof String) {
			return StringUtil.toLowerCase((String)value);
		}

		if (value instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)value;

			JSONArray newJSONArray = _jsonFactory.createJSONArray();

			for (int i = 0; i < jsonArray.length(); i++) {
				newJSONArray.put(_toLowerCase(jsonArray.get(i)));
			}

			return newJSONArray;
		}

		if (value instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject)value;

			Object childValue = jsonObject.opt("value");

			if (childValue instanceof String) {
				jsonObject.put(
					"value", StringUtil.toLowerCase((String)childValue));
			}
		}

		return value;
	}

	/**
	 * Translates a copy of a nested query on a structure field into a nested
	 * query on the object field that replaces it.
	 *
	 * @return whether every structure field reference was translated
	 */
	private boolean _translateDDMFieldArrayQuery(
		JSONObject jsonObject, String parentKey, String objectFieldName,
		String valueFieldName, Set<String> problems) {

		for (String key : new ArrayList<>(jsonObject.keySet())) {
			Object value = jsonObject.opt(key);

			String newKey = key;

			if (key.startsWith("ddmFieldArray.")) {
				String fieldName = key.substring("ddmFieldArray.".length());

				if (fieldName.equals("ddmFieldName")) {
					newKey = "nestedFieldArray.fieldName";
				}
				else if (fieldName.equals("ddmValueFieldName")) {
					newKey = "nestedFieldArray.valueFieldName";
				}
				else if (fieldName.startsWith("ddmFieldValue")) {
					newKey = "nestedFieldArray." + valueFieldName;
				}
				else {
					problems.add(
						StringBundler.concat(
							"The structure field property ", key,
							" has no object field counterpart, so the clause ",
							"that queries it could not be rewritten"));

					return false;
				}
			}

			Object newValue = _translateDDMFieldArrayValue(
				newKey, value, objectFieldName, valueFieldName, problems);

			if (newValue == null) {
				return false;
			}

			if (newKey.equals("nestedFieldArray.value_keyword") &&
				(Objects.equals(parentKey, "term") ||
				 Objects.equals(parentKey, "terms"))) {

				newValue = _toLowerCase(newValue);
			}

			if (!newKey.equals(key)) {
				jsonObject.remove(key);
			}

			jsonObject.put(newKey, newValue);
		}

		return true;
	}

	private Object _translateDDMFieldArrayValue(
		String key, Object value, String objectFieldName, String valueFieldName,
		Set<String> problems) {

		if (value instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)value;

			JSONArray newJSONArray = _jsonFactory.createJSONArray();

			for (int i = 0; i < jsonArray.length(); i++) {
				Object newValue = _translateDDMFieldArrayValue(
					key, jsonArray.get(i), objectFieldName, valueFieldName,
					problems);

				if (newValue == null) {
					return null;
				}

				newJSONArray.put(newValue);
			}

			return newJSONArray;
		}

		if (value instanceof JSONObject) {
			if (!_translateDDMFieldArrayQuery(
					(JSONObject)value, key, objectFieldName, valueFieldName,
					problems)) {

				return null;
			}

			return value;
		}

		if (!(value instanceof String)) {
			return value;
		}

		String string = (String)value;

		if (_getDDMFieldName(string) != null) {
			return objectFieldName;
		}

		if (string.startsWith("ddmFieldValue")) {
			return valueFieldName;
		}

		return string;
	}

	/**
	 * Replaces the query with a query that matches either the original query
	 * or the alternative.
	 */
	private void _wrap(JSONObject queryJSONObject, JSONObject jsonObject)
		throws JSONException {

		JSONObject originalQueryJSONObject = _jsonFactory.createJSONObject(
			queryJSONObject.toString());

		for (String key : new ArrayList<>(queryJSONObject.keySet())) {
			queryJSONObject.remove(key);
		}

		queryJSONObject.put(
			"bool",
			JSONUtil.put(
				"_name", _QUERY_NAME
			).put(
				"minimum_should_match", 1
			).put(
				"should", JSONUtil.putAll(originalQueryJSONObject, jsonObject)
			));
	}

	private static final String _OBJECT_ENTRY_CLAUSE_CONTRIBUTOR_PREFIX =
		"com.liferay.object.internal.search.spi.model.query.contributor." +
			"ObjectEntry";

	private static final String _QUERY_NAME = "CMS_MIGRATION";

	private static final Map<String, String> _clauseContributorClassNames =
		HashMapBuilder.put(
			"BlogsEntry", BlogsEntry.class.getName()
		).put(
			"DLFileEntry", DLFileEntry.class.getName()
		).put(
			"JournalArticle", JournalArticle.class.getName()
		).build();
	private static final Pattern _ddmFieldNamePattern = Pattern.compile(
		"ddm__([a-zA-Z]+)__(\\d+)__([^\\s\"]+)");
	private static final Pattern _languageIdPattern = Pattern.compile(
		"_([a-z]{2,3}_[A-Z]{2})$");

	private final JSONFactory _jsonFactory;
	private final SXPBlueprintReferenceMapper _sxpBlueprintReferenceMapper;

	private static class DDMFieldName {

		@Override
		public String toString() {
			return _ddmStructureId + StringPool.POUND + _fieldReference;
		}

		private DDMFieldName(
			long ddmStructureId, String fieldReference, String languageId) {

			_ddmStructureId = ddmStructureId;
			_fieldReference = fieldReference;
			_languageId = languageId;
		}

		private final long _ddmStructureId;
		private final String _fieldReference;
		private final String _languageId;

	}

}