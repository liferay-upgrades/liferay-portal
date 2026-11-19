/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.migration;

import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.blogs.model.BlogsEntry;
import com.liferay.cms.migration.web.internal.background.task.CMSMigrationBackgroundTaskStatusMessageSender;
import com.liferay.cms.migration.web.internal.constants.CMSMigrationConstants;
import com.liferay.cms.migration.web.internal.migration.content.BlogsEntryMigrator;
import com.liferay.cms.migration.web.internal.migration.content.FileEntryMigrator;
import com.liferay.cms.migration.web.internal.migration.content.FolderMigrator;
import com.liferay.cms.migration.web.internal.migration.content.JournalArticleMigrator;
import com.liferay.cms.migration.web.internal.migration.content.ObjectEntryScopeMigrator;
import com.liferay.cms.migration.web.internal.migration.reference.AssetListEntryReferenceRewriter;
import com.liferay.cms.migration.web.internal.migration.reference.HTMLReferenceRewriter;
import com.liferay.cms.migration.web.internal.migration.reference.LayoutReferenceRewriter;
import com.liferay.cms.migration.web.internal.migration.reference.PortletReferenceRewriter;
import com.liferay.cms.migration.web.internal.migration.reference.SiteNavigationMenuItemReferenceRewriter;
import com.liferay.cms.migration.web.internal.migration.template.DisplayPageTemplateMigrator;
import com.liferay.cms.migration.web.internal.migration.template.JournalTemplateMigrator;
import com.liferay.cms.migration.web.internal.migration.template.WidgetTemplateMigrator;
import com.liferay.depot.service.DepotEntryGroupRelLocalService;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.journal.model.JournalArticle;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runs a migration from classic content to the CMS. Content migrates first;
 * then, when requested, every page reference to the classic content is
 * repointed to the migrated entries.
 *
 * @author Lino Alves
 */
public class CMSMigrationManager {

	public CMSMigrationManager(
		AssetEntryLocalService assetEntryLocalService,
		AssetListEntryReferenceRewriter assetListEntryReferenceRewriter,
		BlogsEntryMigrator blogsEntryMigrator,
		CMSMigrationBackgroundTaskStatusMessageSender
			cmsMigrationBackgroundTaskStatusMessageSender,
		DepotEntryGroupRelLocalService depotEntryGroupRelLocalService,
		DisplayPageTemplateMigrator displayPageTemplateMigrator,
		FileEntryMigrator fileEntryMigrator, FolderMigrator folderMigrator,
		GroupLocalService groupLocalService,
		HTMLReferenceRewriter htmlReferenceRewriter,
		JournalArticleMigrator journalArticleMigrator,
		JournalTemplateMigrator journalTemplateMigrator,
		LayoutLocalService layoutLocalService,
		LayoutReferenceRewriter layoutReferenceRewriter,
		ObjectDefinitionLocalService objectDefinitionLocalService,
		ObjectEntryLocalService objectEntryLocalService,
		ObjectEntryScopeMigrator objectEntryScopeMigrator,
		ObjectFieldLocalService objectFieldLocalService,
		PortletReferenceRewriter portletReferenceRewriter,
		SiteNavigationMenuItemReferenceRewriter
			siteNavigationMenuItemReferenceRewriter,
		WidgetTemplateMigrator widgetTemplateMigrator) {

		_assetEntryLocalService = assetEntryLocalService;
		_assetListEntryReferenceRewriter = assetListEntryReferenceRewriter;
		_blogsEntryMigrator = blogsEntryMigrator;
		_cmsMigrationBackgroundTaskStatusMessageSender =
			cmsMigrationBackgroundTaskStatusMessageSender;
		_depotEntryGroupRelLocalService = depotEntryGroupRelLocalService;
		_displayPageTemplateMigrator = displayPageTemplateMigrator;
		_fileEntryMigrator = fileEntryMigrator;
		_folderMigrator = folderMigrator;
		_groupLocalService = groupLocalService;
		_htmlReferenceRewriter = htmlReferenceRewriter;
		_journalArticleMigrator = journalArticleMigrator;
		_journalTemplateMigrator = journalTemplateMigrator;
		_layoutLocalService = layoutLocalService;
		_layoutReferenceRewriter = layoutReferenceRewriter;
		_objectDefinitionLocalService = objectDefinitionLocalService;
		_objectEntryLocalService = objectEntryLocalService;
		_objectEntryScopeMigrator = objectEntryScopeMigrator;
		_objectFieldLocalService = objectFieldLocalService;
		_portletReferenceRewriter = portletReferenceRewriter;
		_siteNavigationMenuItemReferenceRewriter =
			siteNavigationMenuItemReferenceRewriter;
		_widgetTemplateMigrator = widgetTemplateMigrator;
	}

	public void migrate(CMSMigrationContext cmsMigrationContext)
		throws Exception {

		_depotEntryGroupRelLocalService.addDepotEntryGroupRel(
			cmsMigrationContext.getDepotEntry(
			).getDepotEntryId(),
			cmsMigrationContext.getSourceGroupId());

		cmsMigrationContext.setFileEntryResolver(
			fileEntry -> {
				if (!cmsMigrationContext.isRewriteSourceGroup() &&
					(fileEntry.getGroupId() !=
						cmsMigrationContext.getSourceGroupId())) {

					return cmsMigrationContext.getObjectEntry(
						DLFileEntry.class.getName(),
						fileEntry.getFileEntryId());
				}

				return _fileEntryMigrator.migrate(
					cmsMigrationContext, fileEntry);
			});

		List<Group> rewriteGroups = _getRewriteGroups(cmsMigrationContext);

		Map<String, Map<String, Long>> plan = _getPlan(
			cmsMigrationContext, rewriteGroups);

		_sendPlan(plan);

		if (cmsMigrationContext.isMigrateFileEntries()) {
			_sendPhase(
				plan, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FOLDERS);

			_folderMigrator.migrateDLFolders(cmsMigrationContext);
		}
		else {
			_folderMigrator.registerDLRootFolder(cmsMigrationContext);
		}

		if (cmsMigrationContext.isMigrateBlogsEntries() ||
			cmsMigrationContext.isMigrateJournalArticles()) {

			String bar = CMSMigrationConstants.BAR_BLOGS;

			if (cmsMigrationContext.isMigrateJournalArticles()) {
				bar = CMSMigrationConstants.BAR_WEB_CONTENT;
			}

			_sendPhase(plan, bar, CMSMigrationConstants.PHASE_JOURNAL_FOLDERS);

			_folderMigrator.migrateJournalFolders(cmsMigrationContext);
		}

		if (cmsMigrationContext.isMigrateFileEntries()) {
			_sendPhase(
				plan, CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FILE_ENTRIES);

			_fileEntryMigrator.migrateAll(cmsMigrationContext);
		}

		if (cmsMigrationContext.isMigrateJournalArticles()) {
			_sendPhase(
				plan, CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_JOURNAL_ARTICLES);

			_journalArticleMigrator.migrateAll(cmsMigrationContext);

			_sendPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_TEMPLATES,
				_journalTemplateMigrator.getCount(cmsMigrationContext));

			_journalTemplateMigrator.migrate(cmsMigrationContext);
		}

		if (cmsMigrationContext.isMigrateBlogsEntries()) {
			_sendPhase(
				plan, CMSMigrationConstants.BAR_BLOGS,
				CMSMigrationConstants.PHASE_BLOGS_ENTRIES);

			_blogsEntryMigrator.migrateAll(cmsMigrationContext);
		}

		if (cmsMigrationContext.isMigrateObjectEntries()) {
			_sendPhase(
				plan, CMSMigrationConstants.BAR_OBJECTS,
				CMSMigrationConstants.PHASE_OBJECT_ENTRIES);

			_objectEntryScopeMigrator.migrateAll(cmsMigrationContext);
		}

		_rewriteContentLinks(cmsMigrationContext, plan);

		if (!cmsMigrationContext.isRewritePages()) {
			return;
		}

		Group sourceGroup = cmsMigrationContext.getSourceGroup();

		try {
			_sendPhase(
				plan, CMSMigrationConstants.BAR_UPDATE_PAGES,
				CMSMigrationConstants.PHASE_DISPLAY_PAGE_TEMPLATES);

			for (Group group : rewriteGroups) {
				cmsMigrationContext.setRewriteGroup(group);

				_assetListEntryReferenceRewriter.rewrite(cmsMigrationContext);

				_displayPageTemplateMigrator.migrate(cmsMigrationContext);
			}

			long pagesCount = 0;

			for (Group group : rewriteGroups) {
				pagesCount += _layoutLocalService.getLayoutsCount(
					group.getGroupId());
			}

			_sendPhase(
				CMSMigrationConstants.BAR_UPDATE_PAGES,
				CMSMigrationConstants.PHASE_PAGES, pagesCount);

			for (Group group : rewriteGroups) {
				cmsMigrationContext.setRewriteGroup(group);

				_rewritePages(cmsMigrationContext);

				if (group.getGroupId() == sourceGroup.getGroupId()) {
					continue;
				}

				Group liveGroup = group;

				if (group.isStagingGroup()) {
					liveGroup = group.getLiveGroup();
				}

				if (liveGroup.isSite()) {
					_depotEntryGroupRelLocalService.addDepotEntryGroupRel(
						cmsMigrationContext.getDepotEntry(
						).getDepotEntryId(),
						liveGroup.getGroupId());
				}
			}
		}
		finally {
			cmsMigrationContext.setRewriteGroup(sourceGroup);
		}

		_widgetTemplateMigrator.migrate(cmsMigrationContext);
	}

	private LinkedHashMap<String, Long> _addPendingPhases(
		LinkedHashMap<String, Long> phaseTotals, String... phases) {

		for (String phase : phases) {
			phaseTotals.put(phase, null);
		}

		return phaseTotals;
	}

	private List<Layout> _getLayouts(long groupId) {
		DynamicQuery dynamicQuery = _layoutLocalService.dynamicQuery();

		dynamicQuery.add(RestrictionsFactoryUtil.eq("groupId", groupId));

		return _layoutLocalService.dynamicQuery(dynamicQuery);
	}

	private Map<String, Map<String, Long>> _getPlan(
			CMSMigrationContext cmsMigrationContext, List<Group> rewriteGroups)
		throws Exception {

		Map<String, Map<String, Long>> plan = new LinkedHashMap<>();

		if (cmsMigrationContext.isMigrateFileEntries()) {
			plan.put(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				_addPendingPhases(
					LinkedHashMapBuilder.put(
						CMSMigrationConstants.PHASE_FOLDERS,
						_folderMigrator.getDLFoldersCount(cmsMigrationContext)
					).put(
						CMSMigrationConstants.PHASE_FILE_ENTRIES,
						(long)_fileEntryMigrator.getCount(cmsMigrationContext)
					).build(),
					CMSMigrationConstants.PHASE_CONTENT_LINKS));
		}

		if (cmsMigrationContext.isMigrateJournalArticles()) {
			plan.put(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				_addPendingPhases(
					LinkedHashMapBuilder.put(
						CMSMigrationConstants.PHASE_JOURNAL_FOLDERS,
						_folderMigrator.getJournalFoldersCount(
							cmsMigrationContext)
					).put(
						CMSMigrationConstants.PHASE_JOURNAL_ARTICLES,
						(long)
							_journalArticleMigrator.getJournalArticleResources(
								cmsMigrationContext
							).size()
					).build(),
					CMSMigrationConstants.PHASE_TEMPLATES,
					CMSMigrationConstants.PHASE_CONTENT_LINKS));
		}

		if (cmsMigrationContext.isMigrateBlogsEntries()) {
			plan.put(
				CMSMigrationConstants.BAR_BLOGS,
				_addPendingPhases(
					LinkedHashMapBuilder.put(
						CMSMigrationConstants.PHASE_JOURNAL_FOLDERS,
						() -> {
							if (cmsMigrationContext.
									isMigrateJournalArticles()) {

								return null;
							}

							return _folderMigrator.getJournalFoldersCount(
								cmsMigrationContext);
						}
					).put(
						CMSMigrationConstants.PHASE_BLOGS_ENTRIES,
						(long)_blogsEntryMigrator.getBlogsEntries(
							cmsMigrationContext
						).size()
					).build(),
					CMSMigrationConstants.PHASE_CONTENT_LINKS));
		}

		if (cmsMigrationContext.isMigrateObjectEntries()) {
			plan.put(
				CMSMigrationConstants.BAR_OBJECTS,
				LinkedHashMapBuilder.put(
					CMSMigrationConstants.PHASE_OBJECT_ENTRIES,
					_objectEntryScopeMigrator.getCount(cmsMigrationContext)
				).build());
		}

		if (cmsMigrationContext.isRewritePages()) {
			long displayPageTemplatesCount = 0;

			for (Group group : rewriteGroups) {
				displayPageTemplatesCount +=
					_assetListEntryReferenceRewriter.getCount(
						group.getGroupId());
				displayPageTemplatesCount +=
					_displayPageTemplateMigrator.getCount(group.getGroupId());
			}

			plan.put(
				CMSMigrationConstants.BAR_UPDATE_PAGES,
				_addPendingPhases(
					LinkedHashMapBuilder.put(
						CMSMigrationConstants.PHASE_DISPLAY_PAGE_TEMPLATES,
						displayPageTemplatesCount
					).build(),
					CMSMigrationConstants.PHASE_PAGES));
		}

		return plan;
	}

	/**
	 * Returns every group, other than the source group and the target space,
	 * that has pages. Their pages may reference the content of the Global
	 * site.
	 */
	private List<Group> _getReferencingGroups(
		CMSMigrationContext cmsMigrationContext) {

		DynamicQuery dynamicQuery = _layoutLocalService.dynamicQuery();

		dynamicQuery.add(
			RestrictionsFactoryUtil.eq(
				"companyId", cmsMigrationContext.getCompanyId()));
		dynamicQuery.add(
			RestrictionsFactoryUtil.ne(
				"groupId", cmsMigrationContext.getSourceGroupId()));
		dynamicQuery.add(
			RestrictionsFactoryUtil.ne(
				"groupId", cmsMigrationContext.getDepotGroupId()));
		dynamicQuery.addOrder(OrderFactoryUtil.asc("groupId"));
		dynamicQuery.setProjection(
			ProjectionFactoryUtil.distinct(
				ProjectionFactoryUtil.property("groupId")));

		List<Group> groups = new ArrayList<>();

		for (Object groupId : _layoutLocalService.dynamicQuery(dynamicQuery)) {
			Group group = _groupLocalService.fetchGroup((Long)groupId);

			if ((group != null) && !group.isDepot()) {
				groups.add(group);
			}
		}

		return groups;
	}

	/**
	 * Returns the groups whose pages are rewritten: the source group and, when
	 * the source group is the Global site, every group that references it.
	 */
	private List<Group> _getRewriteGroups(
		CMSMigrationContext cmsMigrationContext) {

		List<Group> rewriteGroups = new ArrayList<>();

		if (!cmsMigrationContext.isRewritePages()) {
			return rewriteGroups;
		}

		Group sourceGroup = cmsMigrationContext.getSourceGroup();

		rewriteGroups.add(sourceGroup);

		if (sourceGroup.isCompany()) {
			rewriteGroups.addAll(_getReferencingGroups(cmsMigrationContext));
		}

		return rewriteGroups;
	}

	private void _rewriteContentLinks(
		CMSMigrationContext cmsMigrationContext,
		List<ObjectEntry> objectEntries) {

		for (ObjectEntry objectEntry : objectEntries) {
			try {
				cmsMigrationContext.sendStatus(objectEntry.getTitleValue());

				_rewriteContentLinks(cmsMigrationContext, objectEntry);
			}
			catch (Exception exception) {
				cmsMigrationContext.getCMSMigrationReport(
				).addWarning(
					StringBundler.concat(
						"Unable to rewrite the links of ",
						objectEntry.getExternalReferenceCode(), ": ",
						exception.getMessage())
				);
			}
		}
	}

	private void _rewriteContentLinks(
		CMSMigrationContext cmsMigrationContext,
		Map<String, Map<String, Long>> plan) {

		Map<String, List<ObjectEntry>> barObjectEntries = new HashMap<>();
		List<ObjectEntry> otherObjectEntries = new ArrayList<>();

		for (Map.Entry<String, ObjectEntry> entry :
				cmsMigrationContext.getObjectEntriesMap(
				).entrySet()) {

			String bar = _bars.get(
				StringUtil.extractFirst(entry.getKey(), StringPool.POUND));

			if ((bar == null) || !plan.containsKey(bar)) {
				otherObjectEntries.add(entry.getValue());

				continue;
			}

			List<ObjectEntry> objectEntries = barObjectEntries.computeIfAbsent(
				bar, key -> new ArrayList<>());

			objectEntries.add(entry.getValue());
		}

		for (Map.Entry<String, Map<String, Long>> entry : plan.entrySet()) {
			Map<String, Long> phaseTotals = entry.getValue();

			if (!phaseTotals.containsKey(
					CMSMigrationConstants.PHASE_CONTENT_LINKS)) {

				continue;
			}

			List<ObjectEntry> objectEntries = barObjectEntries.getOrDefault(
				entry.getKey(), Collections.emptyList());

			_sendPhase(
				entry.getKey(), CMSMigrationConstants.PHASE_CONTENT_LINKS,
				objectEntries.size());

			_rewriteContentLinks(cmsMigrationContext, objectEntries);
		}

		_rewriteContentLinks(cmsMigrationContext, otherObjectEntries);
	}

	private void _rewriteContentLinks(
			CMSMigrationContext cmsMigrationContext, ObjectEntry objectEntry)
		throws Exception {

		Map<String, Serializable> values = new HashMap<>(
			objectEntry.getValues());

		boolean changed = false;

		for (ObjectField objectField :
				_objectFieldLocalService.getObjectFields(
					objectEntry.getObjectDefinitionId())) {

			if (!objectField.compareBusinessType(
					ObjectFieldConstants.BUSINESS_TYPE_RICH_TEXT)) {

				continue;
			}

			Object value = values.get(objectField.getI18nObjectFieldName());

			if (!(value instanceof Map)) {
				continue;
			}

			HashMap<String, Serializable> localizedValues = new HashMap<>(
				(Map<String, Serializable>)value);

			for (Map.Entry<String, Serializable> entry :
					localizedValues.entrySet()) {

				String html = String.valueOf(entry.getValue());

				String newHTML = _htmlReferenceRewriter.rewriteContentLinks(
					cmsMigrationContext, html);

				if (!Objects.equals(html, newHTML)) {
					entry.setValue(newHTML);

					changed = true;
				}
			}

			values.put(objectField.getI18nObjectFieldName(), localizedValues);
		}

		if (!changed) {
			return;
		}

		ServiceContext serviceContext =
			(ServiceContext)cmsMigrationContext.getServiceContext(
			).clone();

		ObjectEntryScopeMigrator.populateServiceContext(
			_assetEntryLocalService,
			_objectDefinitionLocalService.getObjectDefinition(
				objectEntry.getObjectDefinitionId()),
			objectEntry, serviceContext);

		ObjectEntry updatedObjectEntry =
			_objectEntryLocalService.updateObjectEntry(
				cmsMigrationContext.getUserId(), objectEntry.getObjectEntryId(),
				objectEntry.getObjectEntryFolderId(), values, serviceContext);

		for (Map.Entry<String, ObjectEntry> entry :
				cmsMigrationContext.getObjectEntriesMap(
				).entrySet()) {

			ObjectEntry entryObjectEntry = entry.getValue();

			if (entryObjectEntry.getObjectEntryId() ==
					updatedObjectEntry.getObjectEntryId()) {

				entry.setValue(updatedObjectEntry);
			}
		}
	}

	private void _rewritePages(CMSMigrationContext cmsMigrationContext) {
		for (Layout layout :
				_getLayouts(cmsMigrationContext.getRewriteGroupId())) {

			cmsMigrationContext.sendStatus(layout.getFriendlyURL());

			try {
				_layoutReferenceRewriter.rewrite(
					cmsMigrationContext, layout, null);
				_portletReferenceRewriter.rewrite(cmsMigrationContext, layout);
			}
			catch (Exception exception) {
				cmsMigrationContext.getCMSMigrationReport(
				).addError(
					StringBundler.concat(
						"Unable to rewrite the page ", layout.getFriendlyURL(),
						": ", exception.getMessage())
				);
			}
		}

		_siteNavigationMenuItemReferenceRewriter.rewrite(cmsMigrationContext);
	}

	private void _sendPhase(
		Map<String, Map<String, Long>> plan, String bar, String phase) {

		Map<String, Long> phaseTotals = plan.get(bar);

		_sendPhase(bar, phase, phaseTotals.get(phase));
	}

	private void _sendPhase(String bar, String phase, long total) {
		_cmsMigrationBackgroundTaskStatusMessageSender.sendPhaseStatusMessage(
			bar, phase, total);
	}

	private void _sendPlan(Map<String, Map<String, Long>> plan) {
		_cmsMigrationBackgroundTaskStatusMessageSender.sendPlanStatusMessage(
			plan);
	}

	private static final Map<String, String> _bars = HashMapBuilder.put(
		BlogsEntry.class.getName(), CMSMigrationConstants.BAR_BLOGS
	).put(
		DLFileEntry.class.getName(),
		CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA
	).put(
		JournalArticle.class.getName(), CMSMigrationConstants.BAR_WEB_CONTENT
	).build();

	private final AssetEntryLocalService _assetEntryLocalService;
	private final AssetListEntryReferenceRewriter
		_assetListEntryReferenceRewriter;
	private final BlogsEntryMigrator _blogsEntryMigrator;
	private final CMSMigrationBackgroundTaskStatusMessageSender
		_cmsMigrationBackgroundTaskStatusMessageSender;
	private final DepotEntryGroupRelLocalService
		_depotEntryGroupRelLocalService;
	private final DisplayPageTemplateMigrator _displayPageTemplateMigrator;
	private final FileEntryMigrator _fileEntryMigrator;
	private final FolderMigrator _folderMigrator;
	private final GroupLocalService _groupLocalService;
	private final HTMLReferenceRewriter _htmlReferenceRewriter;
	private final JournalArticleMigrator _journalArticleMigrator;
	private final JournalTemplateMigrator _journalTemplateMigrator;
	private final LayoutLocalService _layoutLocalService;
	private final LayoutReferenceRewriter _layoutReferenceRewriter;
	private final ObjectDefinitionLocalService _objectDefinitionLocalService;
	private final ObjectEntryLocalService _objectEntryLocalService;
	private final ObjectEntryScopeMigrator _objectEntryScopeMigrator;
	private final ObjectFieldLocalService _objectFieldLocalService;
	private final PortletReferenceRewriter _portletReferenceRewriter;
	private final SiteNavigationMenuItemReferenceRewriter
		_siteNavigationMenuItemReferenceRewriter;
	private final WidgetTemplateMigrator _widgetTemplateMigrator;

}