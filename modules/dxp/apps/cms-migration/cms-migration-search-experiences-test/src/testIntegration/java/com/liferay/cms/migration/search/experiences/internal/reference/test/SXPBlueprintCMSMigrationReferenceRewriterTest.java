/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.search.experiences.internal.reference.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.depot.constants.DepotConstants;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.model.DepotEntryGroupRel;
import com.liferay.depot.service.DepotEntryGroupRelLocalService;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.journal.constants.JournalFolderConstants;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.test.util.JournalTestUtil;
import com.liferay.layout.provider.LayoutStructureProvider;
import com.liferay.layout.test.util.ContentLayoutTestUtil;
import com.liferay.layout.test.util.LayoutTestUtil;
import com.liferay.layout.util.structure.CollectionStyledLayoutStructureItem;
import com.liferay.layout.util.structure.LayoutStructure;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskConstants;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskExecutor;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskResult;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.search.experiences.model.SXPBlueprint;
import com.liferay.search.experiences.service.SXPBlueprintLocalService;
import com.liferay.segments.service.SegmentsExperienceLocalService;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Lino Alves
 */
@RunWith(Arquillian.class)
public class SXPBlueprintCMSMigrationReferenceRewriterTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_depotEntry = _depotEntryLocalService.addDepotEntry(
			HashMapBuilder.put(
				LocaleUtil.getDefault(), RandomTestUtil.randomString()
			).build(),
			HashMapBuilder.put(
				LocaleUtil.getDefault(), RandomTestUtil.randomString()
			).build(),
			DepotConstants.TYPE_SPACE,
			ServiceContextTestUtil.getServiceContext(_group.getGroupId()));

		_originalObjectDefinitionIds = _getObjectDefinitionIds();
	}

	@After
	public void tearDown() throws Exception {
		for (SXPBlueprint sxpBlueprint : _sxpBlueprints) {
			SXPBlueprint copySXPBlueprint =
				_sxpBlueprintLocalService.
					fetchSXPBlueprintByExternalReferenceCode(
						"CMS_MIGRATION_SXP_BLUEPRINT_" +
							sxpBlueprint.getSXPBlueprintId(),
						sxpBlueprint.getCompanyId());

			if (copySXPBlueprint != null) {
				_sxpBlueprintLocalService.deleteSXPBlueprint(copySXPBlueprint);
			}

			_sxpBlueprintLocalService.deleteSXPBlueprint(
				sxpBlueprint.getSXPBlueprintId());
		}

		for (DepotEntryGroupRel depotEntryGroupRel :
				_depotEntryGroupRelLocalService.getDepotEntryGroupRels(
					_depotEntry)) {

			_depotEntryGroupRelLocalService.deleteDepotEntryGroupRel(
				depotEntryGroupRel);
		}

		_depotEntryLocalService.deleteDepotEntry(_depotEntry);

		for (long objectDefinitionId : _getObjectDefinitionIds()) {
			if (!_originalObjectDefinitionIds.contains(objectDefinitionId)) {
				_objectDefinitionLocalService.deleteObjectDefinition(
					objectDefinitionId);
			}
		}
	}

	@Test
	public void testRewrite() throws Exception {
		JournalArticle journalArticle = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		DDMStructure ddmStructure = journalArticle.getDDMStructure();

		Group ddmStructureGroup = _groupLocalService.getGroup(
			ddmStructure.getGroupId());

		SXPBlueprint collectionProviderSXPBlueprint = _addSXPBlueprint(
			JSONUtil.put(
				"collectionProvider", true
			).put(
				"searchableAssetTypes",
				JSONUtil.putAll(
					StringBundler.concat(
						JournalArticle.class.getName(), "&&",
						ddmStructureGroup.getExternalReferenceCode(), "&&",
						ddmStructure.getExternalReferenceCode()))
			));

		SXPBlueprint otherSiteSXPBlueprint = _addSXPBlueprint(
			JSONUtil.put(
				"scope", JSONUtil.putAll(RandomTestUtil.randomString())
			).put(
				"searchableAssetTypes",
				JSONUtil.putAll(JournalArticle.class.getName())
			));
		SXPBlueprint sxpBlueprint = _addSXPBlueprint(
			JSONUtil.put(
				"scope", JSONUtil.putAll(_group.getExternalReferenceCode())
			).put(
				"searchableAssetTypes",
				JSONUtil.putAll(JournalArticle.class.getName())
			));

		Layout layout = LayoutTestUtil.addTypeContentLayout(_group);

		long segmentsExperienceId =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperienceId(
				layout.getPlid());

		String itemId = ContentLayoutTestUtil.addCollectionDisplayToLayout(
			JSONUtil.put(
				"itemType", JournalArticle.class.getName()
			).put(
				"key", _getCollectionProviderKey(collectionProviderSXPBlueprint)
			).put(
				"type", _TYPE_INFO_LIST_PROVIDER_ITEM_SELECTOR_RETURN_TYPE
			),
			layout, _layoutStructureProvider, null, null, 0,
			segmentsExperienceId);

		for (int i = 0; i < 2; i++) {
			BackgroundTaskResult backgroundTaskResult = _execute();

			Assert.assertEquals(
				backgroundTaskResult.getStatusMessage(),
				BackgroundTaskConstants.STATUS_SUCCESSFUL,
				backgroundTaskResult.getStatus());

			ObjectDefinition objectDefinition =
				_objectDefinitionLocalService.
					getObjectDefinitionByExternalReferenceCode(
						"CMS_MIGRATION_DDM_STRUCTURE_" +
							ddmStructure.getStructureId(),
						_group.getCompanyId());

			SXPBlueprint copySXPBlueprint =
				_sxpBlueprintLocalService.
					getSXPBlueprintByExternalReferenceCode(
						"CMS_MIGRATION_SXP_BLUEPRINT_" +
							collectionProviderSXPBlueprint.getSXPBlueprintId(),
						_group.getCompanyId());

			JSONObject generalConfigurationJSONObject =
				_getGeneralConfigurationJSONObject(copySXPBlueprint);

			Assert.assertEquals(
				objectDefinition.getClassName(),
				generalConfigurationJSONObject.getString(
					"collectionProviderType"));
			Assert.assertEquals(
				JSONUtil.putAll(
					objectDefinition.getClassName()
				).toString(),
				generalConfigurationJSONObject.getJSONArray(
					"searchableAssetTypes"
				).toString());

			Assert.assertEquals(
				collectionProviderSXPBlueprint.getConfigurationJSON(),
				_sxpBlueprintLocalService.getSXPBlueprint(
					collectionProviderSXPBlueprint.getSXPBlueprintId()
				).getConfigurationJSON());

			Assert.assertEquals(
				otherSiteSXPBlueprint.getConfigurationJSON(),
				_sxpBlueprintLocalService.getSXPBlueprint(
					otherSiteSXPBlueprint.getSXPBlueprintId()
				).getConfigurationJSON());

			generalConfigurationJSONObject = _getGeneralConfigurationJSONObject(
				_sxpBlueprintLocalService.getSXPBlueprint(
					sxpBlueprint.getSXPBlueprintId()));

			Group depotGroup = _depotEntry.getGroup();

			Assert.assertEquals(
				JSONUtil.putAll(
					_group.getExternalReferenceCode(),
					depotGroup.getExternalReferenceCode()
				).toString(),
				generalConfigurationJSONObject.getJSONArray(
					"scope"
				).toString());

			Assert.assertEquals(
				JSONUtil.putAll(
					JournalArticle.class.getName(),
					objectDefinition.getClassName()
				).toString(),
				generalConfigurationJSONObject.getJSONArray(
					"searchableAssetTypes"
				).toString());

			LayoutStructure layoutStructure =
				_layoutStructureProvider.getLayoutStructure(
					layout.getPlid(), segmentsExperienceId);

			CollectionStyledLayoutStructureItem
				collectionStyledLayoutStructureItem =
					(CollectionStyledLayoutStructureItem)
						layoutStructure.getLayoutStructureItem(itemId);

			JSONObject collectionJSONObject =
				collectionStyledLayoutStructureItem.getCollectionJSONObject();

			Assert.assertEquals(
				objectDefinition.getClassName(),
				collectionJSONObject.getString("itemType"));
			Assert.assertEquals(
				_getCollectionProviderKey(copySXPBlueprint),
				collectionJSONObject.getString("key"));
		}
	}

	private SXPBlueprint _addSXPBlueprint(
			JSONObject generalConfigurationJSONObject)
		throws Exception {

		SXPBlueprint sxpBlueprint = _sxpBlueprintLocalService.addSXPBlueprint(
			RandomTestUtil.randomString(), TestPropsValues.getUserId(),
			JSONUtil.put(
				"generalConfiguration", generalConfigurationJSONObject
			).toString(),
			null, "[]", null,
			HashMapBuilder.put(
				LocaleUtil.getSiteDefault(), RandomTestUtil.randomString()
			).build(),
			ServiceContextTestUtil.getServiceContext(_group.getGroupId()));

		_sxpBlueprints.add(sxpBlueprint);

		return sxpBlueprint;
	}

	private BackgroundTaskResult _execute() throws Exception {
		Map<String, Serializable> taskContextMap =
			HashMapBuilder.<String, Serializable>put(
				"depotEntryId", _depotEntry.getDepotEntryId()
			).put(
				"migrateJournalArticles", true
			).put(
				"rewritePages", true
			).put(
				"sourceGroupId", _group.getGroupId()
			).build();

		long userId = TestPropsValues.getUserId();

		BackgroundTask backgroundTask =
			(BackgroundTask)ProxyUtil.newProxyInstance(
				BackgroundTask.class.getClassLoader(),
				new Class<?>[] {BackgroundTask.class},
				(proxy, method, arguments) -> {
					String methodName = method.getName();

					if (methodName.equals("getTaskContextMap")) {
						return taskContextMap;
					}
					else if (methodName.equals("getUserId")) {
						return userId;
					}

					return null;
				});

		return _backgroundTaskExecutor.execute(backgroundTask);
	}

	private String _getCollectionProviderKey(SXPBlueprint sxpBlueprint) {
		return StringBundler.concat(
			SXPBlueprint.class.getName(), StringPool.UNDERLINE,
			sxpBlueprint.getCompanyId(), StringPool.UNDERLINE,
			sxpBlueprint.getExternalReferenceCode());
	}

	private JSONObject _getGeneralConfigurationJSONObject(
			SXPBlueprint sxpBlueprint)
		throws Exception {

		JSONObject configurationJSONObject = JSONFactoryUtil.createJSONObject(
			sxpBlueprint.getConfigurationJSON());

		return configurationJSONObject.getJSONObject("generalConfiguration");
	}

	private Set<Long> _getObjectDefinitionIds() {
		DynamicQuery dynamicQuery =
			_objectDefinitionLocalService.dynamicQuery();

		dynamicQuery.add(
			RestrictionsFactoryUtil.eq("companyId", _group.getCompanyId()));
		dynamicQuery.add(
			RestrictionsFactoryUtil.like(
				"externalReferenceCode", "CMS_MIGRATION_%"));
		dynamicQuery.setProjection(
			ProjectionFactoryUtil.property("objectDefinitionId"));

		return new TreeSet<>(
			_objectDefinitionLocalService.dynamicQuery(dynamicQuery));
	}

	private static final String
		_TYPE_INFO_LIST_PROVIDER_ITEM_SELECTOR_RETURN_TYPE =
			"com.liferay.item.selector.criteria." +
				"InfoListProviderItemSelectorReturnType";

	@Inject(
		filter = "background.task.executor.class.name=com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskExecutor"
	)
	private BackgroundTaskExecutor _backgroundTaskExecutor;

	private DepotEntry _depotEntry;

	@Inject
	private DepotEntryGroupRelLocalService _depotEntryGroupRelLocalService;

	@Inject
	private DepotEntryLocalService _depotEntryLocalService;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private GroupLocalService _groupLocalService;

	@Inject
	private LayoutStructureProvider _layoutStructureProvider;

	@Inject
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	private Set<Long> _originalObjectDefinitionIds;

	@Inject
	private SegmentsExperienceLocalService _segmentsExperienceLocalService;

	@Inject
	private SXPBlueprintLocalService _sxpBlueprintLocalService;

	private final List<SXPBlueprint> _sxpBlueprints = new ArrayList<>();

}