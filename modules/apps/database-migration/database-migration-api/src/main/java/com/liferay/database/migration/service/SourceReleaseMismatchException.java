/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service;

import java.util.List;

/**
 * @author Albert Gomes Cabral
 */
public class SourceReleaseMismatchException extends RuntimeException {

	public SourceReleaseMismatchException(List<String> mismatches) {
		super(
			"The source database schema versions do not match this Liferay " +
				"installation: " + String.join(", ", mismatches));

		_mismatches = List.copyOf(mismatches);
	}

	public List<String> getMismatches() {
		return _mismatches;
	}

	private final List<String> _mismatches;

}