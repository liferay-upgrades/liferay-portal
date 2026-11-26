/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.migration;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;

/**
 * Pairs the URL of a classic content item with the URL of the object entry
 * that replaces it, and tells whether the links to the old URL must be updated
 * manually.
 *
 * @author Lino Alves
 */
public class CMSMigrationURLMapping {

	public static final String STATUS_SITE_CONTENT = "site-content";

	public static final String STATUS_UPDATE_MANUALLY = "update-manually";

	public static CMSMigrationURLMapping of(JSONObject jsonObject) {
		return new CMSMigrationURLMapping(
			jsonObject.getString("newURL"), jsonObject.getString("oldURL"),
			jsonObject.getString("status"), jsonObject.getString("type"));
	}

	public CMSMigrationURLMapping(
		String newURL, String oldURL, String status, String type) {

		_newURL = newURL;
		_oldURL = oldURL;
		_status = status;
		_type = type;
	}

	public String getNewURL() {
		return _newURL;
	}

	public String getOldURL() {
		return _oldURL;
	}

	/**
	 * Returns the status of the old URL, such as
	 * <code>update-manually</code>.
	 */
	public String getStatus() {
		return _status;
	}

	/**
	 * Returns the phase key of the migrated content, such as
	 * <code>journal-articles</code>.
	 */
	public String getType() {
		return _type;
	}

	/**
	 * Returns <code>true</code> if the links to the old URL must be updated
	 * manually.
	 */
	public boolean isActionRequired() {
		return _status.equals(STATUS_UPDATE_MANUALLY);
	}

	public JSONObject toJSONObject() {
		return JSONUtil.put(
			"newURL", _newURL
		).put(
			"oldURL", _oldURL
		).put(
			"status", _status
		).put(
			"type", _type
		);
	}

	private final String _newURL;
	private final String _oldURL;
	private final String _status;
	private final String _type;

}