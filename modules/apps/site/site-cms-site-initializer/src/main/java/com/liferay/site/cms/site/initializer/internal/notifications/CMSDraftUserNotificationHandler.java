/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.notifications;

import com.liferay.depot.constants.DepotPortletKeys;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.UserNotificationEvent;
import com.liferay.portal.kernel.notifications.BaseModelUserNotificationHandler;
import com.liferay.portal.kernel.notifications.UserNotificationHandler;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserNotificationEventLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.UnicodeProperties;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Lucas Ferruccio
 */
@Component(
	property = "jakarta.portlet.name=" + DepotPortletKeys.CMS_DRAFT_NOTIFICATION,
	service = UserNotificationHandler.class
)
public class CMSDraftUserNotificationHandler
	extends BaseModelUserNotificationHandler {

	public CMSDraftUserNotificationHandler() {
		setPortletId(DepotPortletKeys.CMS_DRAFT_NOTIFICATION);
	}

	@Override
	protected String getBody(
			UserNotificationEvent userNotificationEvent,
			ServiceContext serviceContext)
		throws Exception {

		JSONObject jsonObject = _jsonFactory.createJSONObject(
			userNotificationEvent.getPayload());

		ObjectEntry objectEntry = _objectEntryLocalService.fetchObjectEntry(
			jsonObject.getLong("classPK"));

		DepotEntry depotEntry = _depotEntryLocalService.fetchDepotEntry(
			jsonObject.getLong("depotEntryId"));

		if ((objectEntry == null) || (depotEntry == null) ||
			(depotEntry.getGroup() == null)) {

			_userNotificationEventLocalService.deleteUserNotificationEvent(
				userNotificationEvent.getUserNotificationEventId());

			return null;
		}

		return StringBundler.concat(
			"<div class=\"title\">",
			_getTitle(depotEntry, objectEntry, serviceContext), "</div>");
	}

	@Override
	protected String getLink(
			UserNotificationEvent userNotificationEvent,
			ServiceContext serviceContext)
		throws Exception {

		JSONObject jsonObject = _jsonFactory.createJSONObject(
			userNotificationEvent.getPayload());

		long classPK = jsonObject.getLong("classPK");

		ThemeDisplay themeDisplay = serviceContext.getThemeDisplay();

		ObjectEntry objectEntry = _objectEntryLocalService.getObjectEntry(
			classPK);

		return StringBundler.concat(
			serviceContext.getPortalURL(), themeDisplay.getPathMain(),
			GroupConstants.CMS_FRIENDLY_URL,
			"/edit_content_item?objectEntryId=", objectEntry.getObjectEntryId(),
			"&redirect=", themeDisplay.getURLCurrent());
	}

	@Override
	protected String getTitle(
			UserNotificationEvent userNotificationEvent,
			ServiceContext serviceContext)
		throws Exception {

		JSONObject jsonObject = _jsonFactory.createJSONObject(
			userNotificationEvent.getPayload());

		ObjectEntry objectEntry = _objectEntryLocalService.fetchObjectEntry(
			jsonObject.getLong("classPK"));

		DepotEntry depotEntry = _depotEntryLocalService.fetchDepotEntry(
			jsonObject.getLong("depotEntryId"));

		if ((objectEntry == null) || (depotEntry == null) ||
			(depotEntry.getGroup() == null)) {

			return null;
		}

		return _getTitle(depotEntry, objectEntry, serviceContext);
	}

	private String _getTitle(
			DepotEntry depotEntry, ObjectEntry objectEntry,
			ServiceContext serviceContext)
		throws Exception {

		Group group = depotEntry.getGroup();

		UnicodeProperties unicodeProperties =
			group.getTypeSettingsProperties();

		int draftNotificationMaxAgeMinutes = GetterUtil.getInteger(
			unicodeProperties.getProperty("draftNotificationMaxAge"),
			PropsValues.CMS_DRAFT_NOTIFICATION_ENTRIES_MAX_AGE);

		int draftNotificationMaxAgeDays = draftNotificationMaxAgeMinutes / 1440;

		return serviceContext.translate(
			"your-draft-x-in-the-x-space-has-not-been-modified-in-x-days",
			HtmlUtil.escape(
				objectEntry.getTitleValue(
					serviceContext.getLanguageId(), true)),
			HtmlUtil.escape(group.getName(serviceContext.getLocale())),
			draftNotificationMaxAgeDays);
	}

	@Reference
	private DepotEntryLocalService _depotEntryLocalService;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

	@Reference
	private UserNotificationEventLocalService
		_userNotificationEventLocalService;

}