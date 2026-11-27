/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.migration.url;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.cms.migration.web.internal.constants.CMSMigrationConstants;
import com.liferay.cms.migration.web.internal.migration.CMSMigrationContext;
import com.liferay.cms.migration.web.internal.migration.CMSMigrationURLMapping;
import com.liferay.cms.migration.web.internal.migration.reference.HTMLReferenceRewriter;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.friendly.url.model.FriendlyURLEntry;
import com.liferay.friendly.url.model.FriendlyURLEntryLocalization;
import com.liferay.friendly.url.service.FriendlyURLEntryLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.object.model.ObjectEntry;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the URL of every migrated web content article, blog entry, and document
 * to the URL of the object entry that replaces it, and records the mappings in
 * the migration report. The old URLs of web content articles and blog entries
 * include the URLs that the item had before. Liferay does not allow redirects
 * from these URLs, so the links to them must be updated manually.
 *
 * @author Lino Alves
 */
public class FriendlyURLMapper {

	public FriendlyURLMapper(
		DLAppLocalService dlAppLocalService,
		FriendlyURLEntryLocalService friendlyURLEntryLocalService,
		GroupLocalService groupLocalService,
		HTMLReferenceRewriter htmlReferenceRewriter, Portal portal) {

		_dlAppLocalService = dlAppLocalService;
		_friendlyURLEntryLocalService = friendlyURLEntryLocalService;
		_groupLocalService = groupLocalService;
		_htmlReferenceRewriter = htmlReferenceRewriter;
		_portal = portal;
	}

	public int getCount(CMSMigrationContext cmsMigrationContext) {
		int count = 0;

		for (String key :
				cmsMigrationContext.getObjectEntriesMap(
				).keySet()) {

			if (_isMapped(_getClassName(key))) {
				count++;
			}
		}

		return count;
	}

	public void mapAll(CMSMigrationContext cmsMigrationContext) {
		Map<Long, List<Group>> groupsMap = new HashMap<>();

		for (Map.Entry<String, ObjectEntry> entry :
				new ArrayList<>(
					cmsMigrationContext.getObjectEntriesMap(
					).entrySet())) {

			String className = _getClassName(entry.getKey());

			if (!_isMapped(className)) {
				continue;
			}

			ObjectEntry objectEntry = entry.getValue();

			cmsMigrationContext.sendStatus(
				objectEntry.getExternalReferenceCode());

			long classPK = GetterUtil.getLong(
				entry.getKey(
				).substring(
					className.length() + 1
				));

			try {
				if (className.equals(BlogsEntry.class.getName())) {
					_map(
						cmsMigrationContext, className, classPK,
						_FRIENDLY_URL_SEPARATOR_BLOGS_ENTRY, groupsMap,
						objectEntry, CMSMigrationConstants.PHASE_BLOGS_ENTRIES);
				}
				else if (className.equals(DLFileEntry.class.getName())) {
					_mapFileEntry(cmsMigrationContext, classPK, objectEntry);
				}
				else {
					_map(
						cmsMigrationContext, className, classPK,
						_FRIENDLY_URL_SEPARATOR_JOURNAL_ARTICLE, groupsMap,
						objectEntry,
						CMSMigrationConstants.PHASE_JOURNAL_ARTICLES);
				}
			}
			catch (Exception exception) {
				cmsMigrationContext.getCMSMigrationReport(
				).addWarning(
					StringBundler.concat(
						"Unable to map the URLs of ",
						objectEntry.getExternalReferenceCode(), ": ",
						exception.getMessage())
				);
			}
		}
	}

	private String _getClassName(String key) {
		return key.substring(0, key.lastIndexOf(CharPool.POUND));
	}

	/**
	 * Returns the groups where the friendly URLs of the group's content
	 * resolve. The content of the Global site resolves in every site.
	 */
	private List<Group> _getGroups(
			CMSMigrationContext cmsMigrationContext, long groupId)
		throws PortalException {

		Group group = _groupLocalService.getGroup(groupId);

		if (!group.isCompany()) {
			return List.of(group);
		}

		List<Group> groups = new ArrayList<>();

		for (Group siteGroup :
				_groupLocalService.getActiveGroups(
					cmsMigrationContext.getCompanyId(), true, true,
					QueryUtil.ALL_POS, QueryUtil.ALL_POS, null)) {

			if (siteGroup.isDepot() || siteGroup.isStagingGroup() ||
				!siteGroup.isSite()) {

				continue;
			}

			groups.add(siteGroup);
		}

		return groups;
	}

	/**
	 * Returns the language of every friendly URL that the item has or had,
	 * keyed by the URL title. The current friendly URLs come first.
	 */
	private Map<String, String> _getLanguageIds(
		FriendlyURLEntry mainFriendlyURLEntry) {

		List<FriendlyURLEntry> friendlyURLEntries = new ArrayList<>();

		friendlyURLEntries.add(mainFriendlyURLEntry);

		for (FriendlyURLEntry friendlyURLEntry :
				_friendlyURLEntryLocalService.getFriendlyURLEntries(
					mainFriendlyURLEntry.getGroupId(),
					mainFriendlyURLEntry.getClassNameId(),
					mainFriendlyURLEntry.getClassPK())) {

			if (friendlyURLEntry.getFriendlyURLEntryId() !=
					mainFriendlyURLEntry.getFriendlyURLEntryId()) {

				friendlyURLEntries.add(friendlyURLEntry);
			}
		}

		Map<String, String> languageIds = new LinkedHashMap<>();

		for (FriendlyURLEntry friendlyURLEntry : friendlyURLEntries) {
			for (FriendlyURLEntryLocalization friendlyURLEntryLocalization :
					_friendlyURLEntryLocalService.
						getFriendlyURLEntryLocalizations(
							friendlyURLEntry.getFriendlyURLEntryId())) {

				languageIds.putIfAbsent(
					friendlyURLEntryLocalization.getUrlTitle(),
					friendlyURLEntryLocalization.getLanguageId());
			}
		}

		return languageIds;
	}

	private boolean _isMapped(String className) {
		if (className.equals(BlogsEntry.class.getName()) ||
			className.equals(DLFileEntry.class.getName()) ||
			className.equals(JournalArticle.class.getName())) {

			return true;
		}

		return false;
	}

	private void _map(
			CMSMigrationContext cmsMigrationContext, String className,
			long classPK, String friendlyURLSeparator,
			Map<Long, List<Group>> groupsMap, ObjectEntry objectEntry,
			String type)
		throws PortalException {

		long classNameId = _portal.getClassNameId(className);

		FriendlyURLEntry mainFriendlyURLEntry =
			_friendlyURLEntryLocalService.fetchMainFriendlyURLEntry(
				classNameId, classPK);

		if (mainFriendlyURLEntry == null) {
			return;
		}

		List<Group> groups = groupsMap.get(mainFriendlyURLEntry.getGroupId());

		if (groups == null) {
			groups = _getGroups(
				cmsMigrationContext, mainFriendlyURLEntry.getGroupId());

			groupsMap.put(mainFriendlyURLEntry.getGroupId(), groups);
		}

		Map<String, String> languageIds = _getLanguageIds(mainFriendlyURLEntry);

		for (Group group : groups) {
			String groupURL =
				_portal.getPathFriendlyURLPublic() + group.getFriendlyURL();

			for (Map.Entry<String, String> entry : languageIds.entrySet()) {
				String urlTitle = entry.getKey();

				String objectEntryPath =
					_htmlReferenceRewriter.getObjectEntryPath(
						cmsMigrationContext, objectEntry, entry.getValue());

				String newURL = groupURL + objectEntryPath;

				String oldURL = StringBundler.concat(
					groupURL, StringPool.SLASH, friendlyURLSeparator,
					StringPool.SLASH, urlTitle);

				String status = CMSMigrationURLMapping.STATUS_UPDATE_MANUALLY;

				// A site's own content takes precedence over the content of
				// the Global site with the same friendly URL

				if (group.getGroupId() != mainFriendlyURLEntry.getGroupId()) {
					FriendlyURLEntry friendlyURLEntry =
						_friendlyURLEntryLocalService.fetchFriendlyURLEntry(
							group.getGroupId(), classNameId, urlTitle);

					if (friendlyURLEntry != null) {
						status = CMSMigrationURLMapping.STATUS_SITE_CONTENT;
					}
				}

				cmsMigrationContext.getCMSMigrationReport(
				).addCMSMigrationURLMapping(
					new CMSMigrationURLMapping(newURL, oldURL, status, type)
				);
			}
		}
	}

	private void _mapFileEntry(
			CMSMigrationContext cmsMigrationContext, long fileEntryId,
			ObjectEntry objectEntry)
		throws PortalException {

		Map<String, Serializable> values = objectEntry.getValues();

		long attachmentFileEntryId = GetterUtil.getLong(
			values.get(CMSMigrationConstants.OBJECT_FIELD_NAME_FILE));

		if (attachmentFileEntryId == 0) {
			return;
		}

		FriendlyURLEntry friendlyURLEntry =
			_friendlyURLEntryLocalService.fetchMainFriendlyURLEntry(
				_portal.getClassNameId(FileEntry.class.getName()), fileEntryId);

		if (friendlyURLEntry == null) {
			return;
		}

		Group group = _groupLocalService.getGroup(
			friendlyURLEntry.getGroupId());

		cmsMigrationContext.getCMSMigrationReport(
		).addCMSMigrationURLMapping(
			new CMSMigrationURLMapping(
				_htmlReferenceRewriter.getFileEntryURL(
					_dlAppLocalService.getFileEntry(attachmentFileEntryId)),
				StringBundler.concat(
					_portal.getPathContext(), "/documents/d",
					group.getFriendlyURL(), StringPool.SLASH,
					friendlyURLEntry.getUrlTitle()),
				CMSMigrationURLMapping.STATUS_UPDATE_MANUALLY,
				CMSMigrationConstants.PHASE_FILE_ENTRIES)
		);
	}

	private static final String _FRIENDLY_URL_SEPARATOR_BLOGS_ENTRY = "b";

	private static final String _FRIENDLY_URL_SEPARATOR_JOURNAL_ARTICLE = "w";

	private final DLAppLocalService _dlAppLocalService;
	private final FriendlyURLEntryLocalService _friendlyURLEntryLocalService;
	private final GroupLocalService _groupLocalService;
	private final HTMLReferenceRewriter _htmlReferenceRewriter;
	private final Portal _portal;

}