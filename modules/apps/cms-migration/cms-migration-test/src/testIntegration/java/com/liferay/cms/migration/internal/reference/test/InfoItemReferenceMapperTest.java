/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.internal.reference.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.blogs.model.BlogsEntry;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Mateus Xavier
 */
@RunWith(Arquillian.class)
public class InfoItemReferenceMapperTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testGetSourceClassName() {
		Assert.assertEquals(
			BlogsEntry.class.getName(),
			_getSourceClassName(BlogsEntry.class.getName()));
		Assert.assertEquals(
			DLFileEntry.class.getName(),
			_getSourceClassName(DLFileEntry.class.getName()));
		Assert.assertEquals(
			DLFileEntry.class.getName(),
			_getSourceClassName(FileEntry.class.getName()));
		Assert.assertEquals(
			JournalArticle.class.getName(),
			_getSourceClassName(JournalArticle.class.getName()));
		Assert.assertNull(_getSourceClassName(RandomTestUtil.randomString()));

		Assert.assertEquals(
			JournalArticle.class.getName(),
			_getSourceClassName(_portal.getClassNameId(JournalArticle.class)));
		Assert.assertNull(_getSourceClassName(0));
	}

	private String _getSourceClassName(long classNameId) {
		return ReflectionTestUtil.invoke(
			_infoItemReferenceMapper, "getSourceClassName",
			new Class<?>[] {long.class}, classNameId);
	}

	private String _getSourceClassName(String className) {
		return ReflectionTestUtil.invoke(
			_infoItemReferenceMapper, "getSourceClassName",
			new Class<?>[] {String.class}, className);
	}

	@Inject(
		filter = "component.name=com.liferay.cms.migration.internal.reference.InfoItemReferenceMapper",
		type = Inject.NoType.class
	)
	private Object _infoItemReferenceMapper;

	@Inject
	private Portal _portal;

}