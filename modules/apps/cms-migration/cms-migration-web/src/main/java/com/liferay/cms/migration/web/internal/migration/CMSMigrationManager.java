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
import com.liferay.cms.migration.web.internal.migration.structure.ObjectDefinitionMigrator;
import com.liferay.cms.migration.web.internal.migration.structure.StructureEditPageResetter;
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
import com.liferay.portal.kernel.util.StringUtil;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
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
		ObjectDefinitionMigrator objectDefinitionMigrator,
		ObjectEntryLocalService objectEntryLocalService,
		ObjectEntryScopeMigrator objectEntryScopeMigrator,
		ObjectFieldLocalService objectFieldLocalService,
		PortletReferenceRewriter portletReferenceRewriter,
		SiteNavigationMenuItemReferenceRewriter
			siteNavigationMenuItemReferenceRewriter,
		StructureEditPageResetter structureEditPageResetter,
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
		_objectDefinitionMigrator = objectDefinitionMigrator;
		_objectEntryLocalService = objectEntryLocalService;
		_objectEntryScopeMigrator = objectEntryScopeMigrator;
		_objectFieldLocalService = objectFieldLocalService;
		_portletReferenceRewriter = portletReferenceRewriter;
		_siteNavigationMenuItemReferenceRewriter =
			siteNavigationMenuItemReferenceRewriter;
		_structureEditPageResetter = structureEditPageResetter;
		_widgetTemplateMigrator = widgetTemplateMigrator;
	}

	public void migrate(CMSMigrationContext cmsMigrationContext)
		throws Exception {

		_migrate(cmsMigrationContext);

		_objectDefinitionMigrator.deleteUnusedLeftoverJSONObjectFields(
			cmsMigrationContext);
		_objectDefinitionMigrator.deleteUnusedLeftoverRepeatableGroups(
			cmsMigrationContext);

		_structureEditPageResetter.reset(cmsMigrationContext);
	}

	private CMSMigrationPlan _createCMSMigrationPlan(
			CMSMigrationContext cmsMigrationContext,
			Map<Long, Long> groupPagesCounts, List<Group> rewriteGroups)
		throws Exception {

		CMSMigrationPlan cmsMigrationPlan = new CMSMigrationPlan(
			_cmsMigrationBackgroundTaskStatusMessageSender);

		if (cmsMigrationContext.isMigrateFileEntries()) {
			long fileEntriesCount = _fileEntryMigrator.getCount(
				cmsMigrationContext);

			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FOLDERS,
				_folderMigrator.getDLFoldersCount(cmsMigrationContext));
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FILE_ENTRIES, fileEntriesCount);
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_CONTENT_LINKS, fileEntriesCount);
		}

		if (cmsMigrationContext.isMigrateJournalArticles()) {
			long journalArticlesCount = _journalArticleMigrator.getCount(
				cmsMigrationContext);

			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_JOURNAL_FOLDERS,
				_folderMigrator.getJournalFoldersCount(cmsMigrationContext));
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_JOURNAL_ARTICLES,
				journalArticlesCount);
			cmsMigrationPlan.addPendingPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_TEMPLATES);
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_CONTENT_LINKS,
				journalArticlesCount);
		}

		if (cmsMigrationContext.isMigrateBlogsEntries()) {
			if (!cmsMigrationContext.isMigrateJournalArticles()) {
				cmsMigrationPlan.addPhase(
					CMSMigrationConstants.BAR_BLOGS,
					CMSMigrationConstants.PHASE_JOURNAL_FOLDERS,
					_folderMigrator.getJournalFoldersCount(
						cmsMigrationContext));
			}

			long blogsEntriesCount = _blogsEntryMigrator.getCount(
				cmsMigrationContext);

			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_BLOGS,
				CMSMigrationConstants.PHASE_BLOGS_ENTRIES, blogsEntriesCount);
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_BLOGS,
				CMSMigrationConstants.PHASE_CONTENT_LINKS, blogsEntriesCount);
		}

		if (cmsMigrationContext.isMigrateObjectEntries()) {
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_OBJECTS,
				CMSMigrationConstants.PHASE_OBJECT_ENTRIES,
				_objectEntryScopeMigrator.getCount(cmsMigrationContext));
		}

		if (cmsMigrationContext.isMigrateRepeatableFields()) {
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_REPEATABLE_FIELDS,
				CMSMigrationConstants.PHASE_REPEATABLE_GROUPS,
				_objectDefinitionMigrator.getEstimatedRepeatableGroupsCount(
					cmsMigrationContext));
		}

		if (cmsMigrationContext.isRewritePages()) {
			long collectionsCount = 0;
			long displayPageTemplatesCount = 0;
			long pagesCount = 0;

			for (Group group : rewriteGroups) {
				long groupId = group.getGroupId();

				collectionsCount += _assetListEntryReferenceRewriter.getCount(
					groupId);

				long groupDisplayPageTemplatesCount =
					_displayPageTemplateMigrator.getCount(groupId);

				displayPageTemplatesCount += groupDisplayPageTemplatesCount;

				long groupPagesCount = _getPagesCount(
					groupDisplayPageTemplatesCount, groupId);

				groupPagesCounts.put(groupId, groupPagesCount);

				pagesCount += groupPagesCount;
			}

			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_UPDATE_PAGES,
				CMSMigrationConstants.PHASE_COLLECTIONS, collectionsCount);
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_UPDATE_PAGES,
				CMSMigrationConstants.PHASE_DISPLAY_PAGE_TEMPLATES,
				displayPageTemplatesCount);
			cmsMigrationPlan.addPhase(
				CMSMigrationConstants.BAR_UPDATE_PAGES,
				CMSMigrationConstants.PHASE_PAGES, pagesCount);
		}

		return cmsMigrationPlan;
	}

	private String _getContentLinksBar(List<String> bars, String key) {
		String bar = _bars.get(StringUtil.extractFirst(key, StringPool.POUND));

		if (bars.contains(bar)) {
			return bar;
		}

		return bars.getFirst();
	}

	private List<Layout> _getLayouts(long groupId) {
		DynamicQuery dynamicQuery = _layoutLocalService.dynamicQuery();

		dynamicQuery.add(RestrictionsFactoryUtil.eq("groupId", groupId));

		return _layoutLocalService.dynamicQuery(dynamicQuery);
	}

	private long _getLayoutsCount(long groupId) {
		return _layoutLocalService.getLayoutsCount(groupId);
	}

	private long _getPagesCount(long displayPageTemplatesCount, long groupId) {
		return _getLayoutsCount(groupId) + (2 * displayPageTemplatesCount);
	}

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

	private void _migrate(CMSMigrationContext cmsMigrationContext)
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

		Map<Long, Long> groupPagesCounts = new HashMap<>();

		CMSMigrationPlan cmsMigrationPlan = _createCMSMigrationPlan(
			cmsMigrationContext, groupPagesCounts, rewriteGroups);

		cmsMigrationPlan.send();

		if (cmsMigrationContext.isMigrateFileEntries()) {
			cmsMigrationPlan.startPhase(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FOLDERS);

			_folderMigrator.migrateDLFolders(cmsMigrationContext);

			cmsMigrationPlan.finishPhase(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FOLDERS);
		}
		else {
			_folderMigrator.registerDLRootFolder(cmsMigrationContext);
		}

		if (cmsMigrationContext.isMigrateBlogsEntries() ||
			cmsMigrationContext.isMigrateJournalArticles()) {

			List<String> bars = cmsMigrationPlan.getBars(
				CMSMigrationConstants.PHASE_JOURNAL_FOLDERS);

			String bar = bars.getFirst();

			cmsMigrationPlan.startPhase(
				bar, CMSMigrationConstants.PHASE_JOURNAL_FOLDERS);

			_folderMigrator.migrateJournalFolders(cmsMigrationContext);

			cmsMigrationPlan.finishPhase(
				bar, CMSMigrationConstants.PHASE_JOURNAL_FOLDERS);
		}

		if (cmsMigrationContext.isMigrateFileEntries()) {
			cmsMigrationPlan.startPhase(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FILE_ENTRIES);

			_fileEntryMigrator.migrateAll(cmsMigrationContext);

			cmsMigrationPlan.finishPhase(
				CMSMigrationConstants.BAR_DOCUMENTS_AND_MEDIA,
				CMSMigrationConstants.PHASE_FILE_ENTRIES);
		}

		if (cmsMigrationContext.isMigrateJournalArticles()) {
			cmsMigrationPlan.startPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_JOURNAL_ARTICLES);

			_journalArticleMigrator.migrateAll(cmsMigrationContext);

			cmsMigrationPlan.finishPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_JOURNAL_ARTICLES);

			cmsMigrationPlan.startPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_TEMPLATES,
				_journalTemplateMigrator.getCount(cmsMigrationContext));

			_journalTemplateMigrator.migrate(cmsMigrationContext);

			cmsMigrationPlan.finishPhase(
				CMSMigrationConstants.BAR_WEB_CONTENT,
				CMSMigrationConstants.PHASE_TEMPLATES);
		}

		if (cmsMigrationContext.isMigrateBlogsEntries()) {
			cmsMigrationPlan.startPhase(
				CMSMigrationConstants.BAR_BLOGS,
				CMSMigrationConstants.PHASE_BLOGS_ENTRIES);

			_blogsEntryMigrator.migrateAll(cmsMigrationContext);

			cmsMigrationPlan.finishPhase(
				CMSMigrationConstants.BAR_BLOGS,
				CMSMigrationConstants.PHASE_BLOGS_ENTRIES);
		}

		if (cmsMigrationContext.isMigrateObjectEntries()) {
			cmsMigrationPlan.startPhase(
				CMSMigrationConstants.BAR_OBJECTS,
				CMSMigrationConstants.PHASE_OBJECT_ENTRIES);

			_objectEntryScopeMigrator.migrateAll(cmsMigrationContext);

			cmsMigrationPlan.finishPhase(
				CMSMigrationConstants.BAR_OBJECTS,
				CMSMigrationConstants.PHASE_OBJECT_ENTRIES);
		}

		_rewriteContentLinks(cmsMigrationContext, cmsMigrationPlan);

		if (cmsMigrationContext.isRewritePages()) {
			_updatePages(
				cmsMigrationContext, cmsMigrationPlan, groupPagesCounts,
				rewriteGroups);
		}

		if (cmsMigrationContext.isMigrateRepeatableFields()) {
			cmsMigrationPlan.finishPhase(
				CMSMigrationConstants.BAR_REPEATABLE_FIELDS,
				CMSMigrationConstants.PHASE_REPEATABLE_GROUPS);
		}
	}

	private void _rewriteContentLinks(
		CMSMigrationContext cmsMigrationContext,
		CMSMigrationPlan cmsMigrationPlan) {

		List<String> bars = cmsMigrationPlan.getBars(
			CMSMigrationConstants.PHASE_CONTENT_LINKS);

		List<Map.Entry<String, ObjectEntry>> entries = new ArrayList<>(
			cmsMigrationContext.getObjectEntriesMap(
			).entrySet());

		if (!bars.isEmpty()) {
			Map<String, Long> barTotals = new HashMap<>();

			for (Map.Entry<String, ObjectEntry> entry : entries) {
				barTotals.merge(
					_getContentLinksBar(bars, entry.getKey()), 1L, Long::sum);
			}

			for (String bar : bars) {
				cmsMigrationPlan.startPhase(
					bar, CMSMigrationConstants.PHASE_CONTENT_LINKS,
					barTotals.getOrDefault(bar, 0L));
			}
		}

		for (Map.Entry<String, ObjectEntry> entry : entries) {
			ObjectEntry objectEntry = entry.getValue();

			try {
				if (bars.isEmpty()) {
					cmsMigrationContext.sendStatus(objectEntry.getTitleValue());
				}
				else {
					cmsMigrationContext.sendStatus(
						_getContentLinksBar(bars, entry.getKey()),
						objectEntry.getTitleValue(),
						CMSMigrationConstants.PHASE_CONTENT_LINKS);
				}

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

		for (String bar : bars) {
			cmsMigrationPlan.finishPhase(
				bar, CMSMigrationConstants.PHASE_CONTENT_LINKS);
		}
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

		updatedObjectEntry.setModifiedDate(objectEntry.getModifiedDate());

		updatedObjectEntry = _objectEntryLocalService.updateObjectEntry(
			updatedObjectEntry);

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

	private void _rewriteReferences(CMSMigrationContext cmsMigrationContext) {
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

	private void _updatePages(
			CMSMigrationContext cmsMigrationContext,
			CMSMigrationPlan cmsMigrationPlan, Map<Long, Long> groupPagesCounts,
			List<Group> rewriteGroups)
		throws Exception {

		long pagesCount = cmsMigrationPlan.getPhaseTotal(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_PAGES);

		Group sourceGroup = cmsMigrationContext.getSourceGroup();

		try {
			for (Group group : rewriteGroups) {
				cmsMigrationContext.setRewriteGroup(group);

				cmsMigrationPlan.startPhase(
					CMSMigrationConstants.BAR_UPDATE_PAGES,
					CMSMigrationConstants.PHASE_COLLECTIONS);

				_assetListEntryReferenceRewriter.rewrite(cmsMigrationContext);

				cmsMigrationPlan.startPhase(
					CMSMigrationConstants.BAR_UPDATE_PAGES,
					CMSMigrationConstants.PHASE_DISPLAY_PAGE_TEMPLATES);

				_displayPageTemplateMigrator.migrate(cmsMigrationContext);

				pagesCount -= groupPagesCounts.get(group.getGroupId());

				pagesCount += _getLayoutsCount(group.getGroupId());

				cmsMigrationPlan.startPhase(
					CMSMigrationConstants.BAR_UPDATE_PAGES,
					CMSMigrationConstants.PHASE_PAGES, pagesCount);

				_rewriteReferences(cmsMigrationContext);

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

		cmsMigrationPlan.finishPhase(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_COLLECTIONS);
		cmsMigrationPlan.finishPhase(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_DISPLAY_PAGE_TEMPLATES);
		cmsMigrationPlan.finishPhase(
			CMSMigrationConstants.BAR_UPDATE_PAGES,
			CMSMigrationConstants.PHASE_PAGES);

		_widgetTemplateMigrator.migrate(cmsMigrationContext);
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
	private final ObjectDefinitionMigrator _objectDefinitionMigrator;
	private final ObjectEntryLocalService _objectEntryLocalService;
	private final ObjectEntryScopeMigrator _objectEntryScopeMigrator;
	private final ObjectFieldLocalService _objectFieldLocalService;
	private final PortletReferenceRewriter _portletReferenceRewriter;
	private final SiteNavigationMenuItemReferenceRewriter
		_siteNavigationMenuItemReferenceRewriter;
	private final StructureEditPageResetter _structureEditPageResetter;
	private final WidgetTemplateMigrator _widgetTemplateMigrator;

}