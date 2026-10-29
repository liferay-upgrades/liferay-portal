/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.scheduler.helper;

import com.liferay.depot.constants.DepotConstants;
import com.liferay.depot.constants.DepotPortletKeys;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.object.constants.ObjectFolderConstants;
import com.liferay.object.model.ObjectDefinitionTable;
import com.liferay.object.model.ObjectEntryTable;
import com.liferay.object.model.ObjectEntryVersion;
import com.liferay.object.model.ObjectEntryVersionTable;
import com.liferay.object.model.ObjectFolderTable;
import com.liferay.object.service.ObjectEntryVersionLocalService;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.UserNotificationDeliveryConstants;
import com.liferay.portal.kernel.service.UserNotificationEventLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.Date;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Lucas Ferruccio
 */
@Component(service = CMSDraftNotificationCheckerHelper.class)
public class CMSDraftNotificationCheckerHelper {

	public void checkDraftObjectEntries(Long companyId) {
		for (DepotEntry depotEntry :
				_depotEntryLocalService.getDepotEntries(
					companyId, DepotConstants.TYPE_SPACE)) {

			try {
				Group group = depotEntry.getGroup();

				UnicodeProperties unicodeProperties =
					group.getTypeSettingsProperties();

				if (!GetterUtil.getBoolean(
						unicodeProperties.getProperty(
							"draftNotificationEnabled"))) {

					continue;
				}

				int draftNotificationMaxAge = GetterUtil.getInteger(
					unicodeProperties.getProperty("draftNotificationMaxAge"));

				_checkSpaceDraftObjectEntries(
					depotEntry, group.getGroupId(), draftNotificationMaxAge);
			}
			catch (PortalException portalException) {
				if (_log.isDebugEnabled()) {
					_log.debug(portalException);
				}
			}
		}
	}

	private void _checkSpaceDraftObjectEntries(
		DepotEntry depotEntry, long groupId, int draftNotificationMaxAge) {

		Date endDate = new Date(
			System.currentTimeMillis() -
				(draftNotificationMaxAge * Time.MINUTE));

		Date startDate = new Date(endDate.getTime() - Time.DAY);

		for (ObjectEntryVersion objectEntryVersion :
				_getObjectEntryVersions(groupId, startDate, endDate)) {

			try {
				_sendUserDraftNotificationEvent(depotEntry, objectEntryVersion);
			}
			catch (PortalException portalException) {
				_log.error("Unable to send user notification", portalException);
			}
		}
	}

	private List<ObjectEntryVersion> _getObjectEntryVersions(
		long groupId, Date startDate, Date endDate) {

		return _objectEntryVersionLocalService.dslQuery(
			DSLQueryFactoryUtil.select(
				ObjectEntryVersionTable.INSTANCE
			).from(
				ObjectEntryVersionTable.INSTANCE
			).innerJoinON(
				ObjectEntryTable.INSTANCE,
				ObjectEntryTable.INSTANCE.objectEntryId.eq(
					ObjectEntryVersionTable.INSTANCE.objectEntryId)
			).innerJoinON(
				ObjectDefinitionTable.INSTANCE,
				ObjectDefinitionTable.INSTANCE.objectDefinitionId.eq(
					ObjectEntryTable.INSTANCE.objectDefinitionId)
			).innerJoinON(
				ObjectFolderTable.INSTANCE,
				ObjectFolderTable.INSTANCE.objectFolderId.eq(
					ObjectDefinitionTable.INSTANCE.objectFolderId)
			).where(
				ObjectEntryTable.INSTANCE.groupId.eq(
					groupId
				).and(
					ObjectFolderTable.INSTANCE.externalReferenceCode.eq(
						ObjectFolderConstants.
							EXTERNAL_REFERENCE_CODE_CONTENT_STRUCTURES)
				).and(
					ObjectEntryVersionTable.INSTANCE.status.eq(
						WorkflowConstants.STATUS_DRAFT)
				).and(
					ObjectEntryVersionTable.INSTANCE.modifiedDate.gt(startDate)
				).and(
					ObjectEntryVersionTable.INSTANCE.modifiedDate.lt(endDate)
				)
			));
	}

	private void _sendUserDraftNotificationEvent(
			DepotEntry depotEntry, ObjectEntryVersion objectEntryVersion)
		throws PortalException {

		_userNotificationEventLocalService.sendUserNotificationEvents(
			objectEntryVersion.getUserId(),
			DepotPortletKeys.CMS_DRAFT_NOTIFICATION,
			UserNotificationDeliveryConstants.TYPE_WEBSITE,
			JSONUtil.put(
				"classPK", objectEntryVersion.getObjectEntryId()
			).put(
				"depotEntryId", depotEntry.getDepotEntryId()
			));
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CMSDraftNotificationCheckerHelper.class);

	@Reference
	private DepotEntryLocalService _depotEntryLocalService;

	@Reference
	private ObjectEntryVersionLocalService _objectEntryVersionLocalService;

	@Reference
	private UserNotificationEventLocalService
		_userNotificationEventLocalService;

}