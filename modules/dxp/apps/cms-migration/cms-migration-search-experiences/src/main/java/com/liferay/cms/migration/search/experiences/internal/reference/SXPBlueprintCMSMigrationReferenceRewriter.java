/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.search.experiences.internal.reference;

import com.liferay.cms.migration.reference.CMSMigrationReferenceContext;
import com.liferay.cms.migration.reference.CMSMigrationReferenceRewriter;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.search.experiences.model.SXPBlueprint;
import com.liferay.search.experiences.service.SXPBlueprintLocalService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Repoints the search blueprints of the company to the migrated content.
 *
 * <p>
 * A blueprint that searches the source site selects the object definitions
 * next to the classic content types it selects, searches the space next to
 * the source site, and applies its clauses on classic types, structures, and
 * structure fields to the object definitions and object fields that replace
 * them.
 * </p>
 *
 * <p>
 * A blueprint that is a collection provider of a classic type can list only
 * one type, so it is copied instead. The copy lists the object definition
 * that replaces the type, and the pages that use the original collection
 * provider use the copy.
 * </p>
 *
 * @author Lino Alves
 */
@Component(service = CMSMigrationReferenceRewriter.class)
public class SXPBlueprintCMSMigrationReferenceRewriter
	implements CMSMigrationReferenceRewriter {

	@Override
	public void rewrite(
			CMSMigrationReferenceContext cmsMigrationReferenceContext)
		throws Exception {

		SXPBlueprintReferenceMapper sxpBlueprintReferenceMapper =
			new SXPBlueprintReferenceMapper(
				cmsMigrationReferenceContext, _ddmStructureLocalService,
				_dlFileEntryTypeLocalService, _groupLocalService,
				_objectFieldLocalService);

		if (!sxpBlueprintReferenceMapper.hasObjectDefinitions()) {
			return;
		}

		SXPBlueprintJSONRewriter sxpBlueprintJSONRewriter =
			new SXPBlueprintJSONRewriter(
				_jsonFactory, sxpBlueprintReferenceMapper);

		List<SXPBlueprint> sxpBlueprints =
			_sxpBlueprintLocalService.getSXPBlueprints(
				cmsMigrationReferenceContext.getCompanyId());

		cmsMigrationReferenceContext.sendPhase(
			"search-blueprints", sxpBlueprints.size());

		for (SXPBlueprint sxpBlueprint : sxpBlueprints) {
			String externalReferenceCode =
				sxpBlueprint.getExternalReferenceCode();

			if (externalReferenceCode.startsWith(_EXTERNAL_REFERENCE_CODE)) {
				continue;
			}

			String title = sxpBlueprint.getTitle(LocaleUtil.getSiteDefault());

			cmsMigrationReferenceContext.sendStatus(title);

			try {
				_rewrite(
					cmsMigrationReferenceContext, sxpBlueprint,
					sxpBlueprintJSONRewriter, sxpBlueprintReferenceMapper,
					title);
			}
			catch (Exception exception) {
				cmsMigrationReferenceContext.addError(
					StringBundler.concat(
						"Unable to rewrite the search blueprint ", title, ": ",
						exception.getMessage()));
			}
		}
	}

	private void _addProblems(
		CMSMigrationReferenceContext cmsMigrationReferenceContext,
		Set<String> problems, String title) {

		for (String problem : problems) {
			cmsMigrationReferenceContext.addError(
				StringBundler.concat(
					"The search blueprint ", title,
					" does not find the migrated content as it finds the ",
					"classic content: ", problem));
		}
	}

	private boolean _contains(JSONArray jsonArray, String value) {
		for (int i = 0; i < jsonArray.length(); i++) {
			if (Objects.equals(jsonArray.getString(i), value)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds or updates the copy of a blueprint that is a collection provider
	 * of a classic type, so the copy lists the object definition that
	 * replaces the type. Search Experiences derives the type of a collection
	 * provider from its searchable asset types, so the copy replaces them with
	 * the object definition.
	 */
	private void _copyCollectionProvider(
			CMSMigrationReferenceContext cmsMigrationReferenceContext,
			String collectionProviderType, JSONObject configurationJSONObject,
			JSONArray elementInstancesJSONArray, SXPBlueprint sxpBlueprint,
			SXPBlueprintJSONRewriter sxpBlueprintJSONRewriter,
			SXPBlueprintReferenceMapper sxpBlueprintReferenceMapper,
			String title)
		throws Exception {

		Set<String> classNames = new LinkedHashSet<>();
		long classTypeId = -1;

		JSONObject generalConfigurationJSONObject =
			configurationJSONObject.getJSONObject("generalConfiguration");

		JSONArray searchableAssetTypesJSONArray =
			generalConfigurationJSONObject.getJSONArray("searchableAssetTypes");

		if (searchableAssetTypesJSONArray != null) {
			for (int i = 0; i < searchableAssetTypesJSONArray.length(); i++) {
				String searchableAssetType =
					searchableAssetTypesJSONArray.getString(i);

				List<String> searchableAssetTypeClassNames =
					sxpBlueprintReferenceMapper.getClassNames(
						searchableAssetType);

				if (searchableAssetTypeClassNames.size() == 1) {
					classTypeId = sxpBlueprintReferenceMapper.getClassTypeId(
						searchableAssetType);
				}

				classNames.addAll(searchableAssetTypeClassNames);
			}
		}

		if ((classNames.size() != 1) || (classTypeId < 0)) {
			cmsMigrationReferenceContext.addError(
				StringBundler.concat(
					"The search blueprint ", title,
					" is a collection provider of ", collectionProviderType,
					", which several content structures replace, so no ",
					"collection provider lists the migrated content"));

			return;
		}

		Set<String> problems = new LinkedHashSet<>();

		sxpBlueprintJSONRewriter.rewriteConfiguration(
			configurationJSONObject, true, problems);
		sxpBlueprintJSONRewriter.rewriteElementInstances(
			elementInstancesJSONArray, problems);

		_addProblems(cmsMigrationReferenceContext, problems, title);

		String externalReferenceCode =
			_EXTERNAL_REFERENCE_CODE + sxpBlueprint.getSXPBlueprintId();

		SXPBlueprint newSXPBlueprint =
			_sxpBlueprintLocalService.fetchSXPBlueprintByExternalReferenceCode(
				externalReferenceCode, sxpBlueprint.getCompanyId());

		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		if (serviceContext == null) {
			serviceContext = new ServiceContext();

			serviceContext.setCompanyId(sxpBlueprint.getCompanyId());
			serviceContext.setUserId(cmsMigrationReferenceContext.getUserId());
		}

		if (newSXPBlueprint == null) {
			newSXPBlueprint = _sxpBlueprintLocalService.addSXPBlueprint(
				externalReferenceCode, cmsMigrationReferenceContext.getUserId(),
				configurationJSONObject.toString(),
				sxpBlueprint.getDescriptionMap(),
				elementInstancesJSONArray.toString(),
				sxpBlueprint.getSchemaVersion(), sxpBlueprint.getTitleMap(),
				serviceContext);
		}
		else {
			newSXPBlueprint = _sxpBlueprintLocalService.updateSXPBlueprint(
				externalReferenceCode, cmsMigrationReferenceContext.getUserId(),
				newSXPBlueprint.getSXPBlueprintId(),
				configurationJSONObject.toString(),
				sxpBlueprint.getDescriptionMap(),
				elementInstancesJSONArray.toString(),
				sxpBlueprint.getSchemaVersion(), sxpBlueprint.getTitleMap(),
				serviceContext);
		}

		cmsMigrationReferenceContext.addCollectionProviderKey(
			_getCollectionProviderKey(sxpBlueprint),
			_getCollectionProviderKey(newSXPBlueprint),
			StringUtil.extractFirst(collectionProviderType, "&&"), classTypeId);
		cmsMigrationReferenceContext.incrementCount("search-blueprints");
	}

	private String _getCollectionProviderKey(SXPBlueprint sxpBlueprint) {
		return StringBundler.concat(
			SXPBlueprint.class.getName(), StringPool.UNDERLINE,
			sxpBlueprint.getCompanyId(), StringPool.UNDERLINE,
			sxpBlueprint.getExternalReferenceCode());
	}

	/**
	 * Returns whether the blueprint searches the content that migrated: it
	 * searches the source site and selects at least one migrated type.
	 */
	private boolean _isSearchingMigratedContent(
		JSONObject generalConfigurationJSONObject,
		SXPBlueprintReferenceMapper sxpBlueprintReferenceMapper) {

		if (generalConfigurationJSONObject == null) {
			return true;
		}

		JSONArray scopeJSONArray = generalConfigurationJSONObject.getJSONArray(
			"scope");

		if ((scopeJSONArray != null) && (scopeJSONArray.length() > 0) &&
			!_contains(
				scopeJSONArray,
				sxpBlueprintReferenceMapper.
					getSourceGroupExternalReferenceCode())) {

			return false;
		}

		JSONArray searchableAssetTypesJSONArray =
			generalConfigurationJSONObject.getJSONArray("searchableAssetTypes");

		if ((searchableAssetTypesJSONArray == null) ||
			(searchableAssetTypesJSONArray.length() == 0)) {

			return true;
		}

		for (int i = 0; i < searchableAssetTypesJSONArray.length(); i++) {
			List<String> classNames = sxpBlueprintReferenceMapper.getClassNames(
				searchableAssetTypesJSONArray.getString(i));

			if (!classNames.isEmpty()) {
				return true;
			}
		}

		return false;
	}

	private void _rewrite(
			CMSMigrationReferenceContext cmsMigrationReferenceContext,
			SXPBlueprint sxpBlueprint,
			SXPBlueprintJSONRewriter sxpBlueprintJSONRewriter,
			SXPBlueprintReferenceMapper sxpBlueprintReferenceMapper,
			String title)
		throws Exception {

		JSONObject configurationJSONObject = _toJSONObject(
			sxpBlueprint.getConfigurationJSON());

		JSONObject generalConfigurationJSONObject =
			configurationJSONObject.getJSONObject("generalConfiguration");

		if (!_isSearchingMigratedContent(
				generalConfigurationJSONObject, sxpBlueprintReferenceMapper)) {

			return;
		}

		JSONArray elementInstancesJSONArray = _toJSONArray(
			sxpBlueprint.getElementInstancesJSON());

		if ((generalConfigurationJSONObject != null) &&
			generalConfigurationJSONObject.getBoolean("collectionProvider")) {

			String collectionProviderType =
				generalConfigurationJSONObject.getString(
					"collectionProviderType");

			if (Validator.isNotNull(collectionProviderType) &&
				!sxpBlueprintReferenceMapper.getClassNames(
					collectionProviderType
				).isEmpty()) {

				_copyCollectionProvider(
					cmsMigrationReferenceContext, collectionProviderType,
					configurationJSONObject, elementInstancesJSONArray,
					sxpBlueprint, sxpBlueprintJSONRewriter,
					sxpBlueprintReferenceMapper, title);

				return;
			}
		}

		String configurationJSON = configurationJSONObject.toString();
		String elementInstancesJSON = elementInstancesJSONArray.toString();

		Set<String> problems = new LinkedHashSet<>();

		sxpBlueprintJSONRewriter.rewriteConfiguration(
			configurationJSONObject, false, problems);
		sxpBlueprintJSONRewriter.rewriteElementInstances(
			elementInstancesJSONArray, problems);

		_addProblems(cmsMigrationReferenceContext, problems, title);

		boolean changed = false;

		if (!configurationJSON.equals(configurationJSONObject.toString())) {
			sxpBlueprint.setConfigurationJSON(
				configurationJSONObject.toString());

			changed = true;
		}

		if (!elementInstancesJSON.equals(
				elementInstancesJSONArray.toString())) {

			sxpBlueprint.setElementInstancesJSON(
				elementInstancesJSONArray.toString());

			changed = true;
		}

		if (!changed) {
			return;
		}

		_sxpBlueprintLocalService.updateSXPBlueprint(sxpBlueprint);

		cmsMigrationReferenceContext.incrementCount("search-blueprints");
	}

	private JSONArray _toJSONArray(String json) throws Exception {
		if (Validator.isNull(json)) {
			return _jsonFactory.createJSONArray();
		}

		return _jsonFactory.createJSONArray(json);
	}

	private JSONObject _toJSONObject(String json) throws Exception {
		if (Validator.isNull(json)) {
			return _jsonFactory.createJSONObject();
		}

		return _jsonFactory.createJSONObject(json);
	}

	private static final String _EXTERNAL_REFERENCE_CODE =
		"CMS_MIGRATION_SXP_BLUEPRINT_";

	@Reference
	private DDMStructureLocalService _ddmStructureLocalService;

	@Reference
	private DLFileEntryTypeLocalService _dlFileEntryTypeLocalService;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private ObjectFieldLocalService _objectFieldLocalService;

	@Reference
	private SXPBlueprintLocalService _sxpBlueprintLocalService;

}