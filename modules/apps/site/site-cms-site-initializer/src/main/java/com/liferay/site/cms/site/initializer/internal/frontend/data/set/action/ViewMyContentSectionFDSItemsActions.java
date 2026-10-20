/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.frontend.data.set.action;

import com.liferay.frontend.data.set.action.FDSItemsActions;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.site.cms.site.initializer.internal.constants.CMSSiteInitializerFDSNames;
import com.liferay.site.cms.site.initializer.internal.display.context.SectionDisplayContextUtil;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import org.osgi.service.component.annotations.Component;

/**
 * @author Samuel Lopes
 */
@Component(
	property = "frontend.data.set.name=" + CMSSiteInitializerFDSNames.MY_CONTENT_SECTION,
	service = FDSItemsActions.class
)
public class ViewMyContentSectionFDSItemsActions implements FDSItemsActions {

	@Override
	public List<FDSActionDropdownItem> getFDSActionDropdownItems(
		HttpServletRequest httpServletRequest) {

		return SectionDisplayContextUtil.getAllSectionFDSActionDropdownItems(
			httpServletRequest);
	}

}