/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.reference;

import com.liferay.object.model.ObjectDefinition;
import com.liferay.portal.kernel.model.Group;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Exposes the state of a running migration to a {@link
 * CMSMigrationReferenceRewriter}: the source site, the target space, and the
 * object definitions that replace the classic content types.
 *
 * @author Lino Alves
 */
@ProviderType
public interface CMSMigrationReferenceContext {

	/**
	 * Registers a collection provider that replaces another one, so the pages
	 * that list the items of the original provider list the items of the new
	 * one instead.
	 *
	 * @param key the key of the original collection provider
	 * @param newKey the key of the collection provider that replaces it
	 * @param className the classic class name that the original collection
	 *        provider lists
	 * @param classTypeId the class type ID of the object definition that the
	 *        new collection provider lists, as {@link
	 *        #getObjectDefinitions(String)} keys it
	 */
	public void addCollectionProviderKey(
		String key, String newKey, String className, long classTypeId);

	public void addError(String error);

	public void addWarning(String warning);

	public long getCompanyId();

	public Group getDepotGroup();

	/**
	 * Returns the object definitions that replace a classic class name in this
	 * migration, keyed by the class type ID of the classic content they
	 * replace: the structure ID for Web Content, the file entry type ID for
	 * Documents and Media, and <code>0</code> for Blogs.
	 */
	public Map<Long, ObjectDefinition> getObjectDefinitions(String className);

	/**
	 * Returns the name of the object field that replaces a Web Content
	 * structure field, or <code>null</code> if the structure did not migrate
	 * in this migration or the field has no counterpart.
	 */
	public String getObjectFieldName(
		long ddmStructureId, String fieldReference);

	public Group getSourceGroup();

	public long getUserId();

	public void incrementCount(String key);

	public void sendPhase(String phase, long total);

	public void sendStatus(String itemTitle);

}