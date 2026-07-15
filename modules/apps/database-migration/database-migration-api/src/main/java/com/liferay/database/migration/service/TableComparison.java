/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service;

import java.util.List;

/**
 * @author Albert Gomes Cabral
 */
public interface TableComparison {

	public List<ColumnComparison> getColumnComparisons();

	public long getSourceRowCount();

	public String getTableName();

	public long getTargetRowCount();

	public boolean isOnSource();

	public boolean isOnTarget();

}