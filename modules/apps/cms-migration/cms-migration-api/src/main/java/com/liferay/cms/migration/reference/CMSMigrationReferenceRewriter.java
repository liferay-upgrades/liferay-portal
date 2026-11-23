/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.reference;

/**
 * Repoints references to classic content that live outside the source site,
 * such as company-wide search configurations, to the migrated object entries.
 * Every registered rewriter runs once per migration that rewrites pages, after
 * the content migrates and before the pages are rewritten.
 *
 * @author Lino Alves
 */
public interface CMSMigrationReferenceRewriter {

	public void rewrite(
			CMSMigrationReferenceContext cmsMigrationReferenceContext)
		throws Exception;

}