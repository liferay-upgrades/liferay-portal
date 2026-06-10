/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.source.formatter.util.GradleBuildFile;

/**
 * @author Regisson Aguiar
 */
public class UpgradeGradlePetraModelAdapterCheck extends BaseFileCheck {

	@Override
	protected String doProcess(
		String fileName, String absolutePath, String content) {

		if (!absolutePath.endsWith("/build.gradle")) {
			return content;
		}

		GradleBuildFile gradleBuildFile = new GradleBuildFile(content);

		gradleBuildFile.deleteGradleDependency(
			"com.liferay", "com.liferay.petra.model.adapter");

		String source = gradleBuildFile.getSource();

		if (content.equals(source)) {
			return content;
		}

		return source;
	}

}