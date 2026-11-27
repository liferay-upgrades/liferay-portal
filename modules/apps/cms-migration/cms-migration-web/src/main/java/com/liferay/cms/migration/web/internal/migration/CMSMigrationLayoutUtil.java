/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.migration;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;

/**
 * @author Lucas Emanuel
 */
public class CMSMigrationLayoutUtil {

	public static String getDescription(Layout layout) {
		String descriptiveName = String.valueOf(layout.getGroupId());

		try {
			Group group = layout.getGroup();

			descriptiveName = group.getDescriptiveName();
		}
		catch (PortalException portalException) {
			if (_log.isDebugEnabled()) {
				_log.debug(portalException);
			}
		}

		return StringBundler.concat(
			layout.getFriendlyURL(), " (site ", descriptiveName, ", PLID ",
			layout.getPlid(), ")");
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CMSMigrationLayoutUtil.class);

}