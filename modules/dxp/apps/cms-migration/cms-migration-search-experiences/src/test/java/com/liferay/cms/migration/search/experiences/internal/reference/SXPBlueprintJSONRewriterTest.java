/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.search.experiences.internal.reference;

import com.liferay.journal.model.JournalArticle;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Lino Alves
 */
public class SXPBlueprintJSONRewriterTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_sxpBlueprintJSONRewriter = new SXPBlueprintJSONRewriter(
			_jsonFactory, _sxpBlueprintReferenceMapper);

		Mockito.when(
			_sxpBlueprintReferenceMapper.getClassNames(
				JournalArticle.class.getName())
		).thenReturn(
			Collections.singletonList(_OBJECT_DEFINITION_CLASS_NAME)
		);

		Mockito.when(
			_sxpBlueprintReferenceMapper.getDepotGroupExternalReferenceCode()
		).thenReturn(
			"DEPOT"
		);

		Mockito.when(
			_sxpBlueprintReferenceMapper.getObjectDefinitionIds("KEY")
		).thenReturn(
			Collections.singletonList(200L)
		);

		Mockito.when(
			_sxpBlueprintReferenceMapper.getObjectFieldName(100, "Text")
		).thenReturn(
			"text"
		);

		Mockito.when(
			_sxpBlueprintReferenceMapper.getSourceGroupExternalReferenceCode()
		).thenReturn(
			"SOURCE"
		);

		Mockito.when(
			_sxpBlueprintReferenceMapper.getValueFieldName(100, "text", "en_US")
		).thenReturn(
			"value_keyword"
		);

		Mockito.when(
			_sxpBlueprintReferenceMapper.isMigratedDDMStructure(100)
		).thenReturn(
			true
		);
	}

	@Test
	public void testRewriteConfiguration() throws Exception {
		JSONObject configurationJSONObject = _jsonFactory.createJSONObject(
			_read("testRewriteConfiguration.json"));

		for (int i = 0; i < 2; i++) {
			Set<String> problems = new LinkedHashSet<>();

			_sxpBlueprintJSONRewriter.rewriteConfiguration(
				configurationJSONObject, false, problems);

			JSONAssert.assertEquals(
				_read("testRewriteConfiguration.expected.json"),
				configurationJSONObject.toString(), true);
			Assert.assertEquals(
				new LinkedHashSet<>(
					Arrays.asList(
						StringBundler.concat(
							"A clause limits results to the scope ",
							"${context.scope_group_id}, which does not ",
							"include the space"),
						StringBundler.concat(
							"The field ddm__keyword__100__Other_en_US belongs ",
							"to a migrated structure, but the clause that ",
							"uses it could not be rewritten"))),
				problems);
		}

		configurationJSONObject = _jsonFactory.createJSONObject(
			_read("testRewriteConfiguration.json"));

		_sxpBlueprintJSONRewriter.rewriteConfiguration(
			configurationJSONObject, true, new LinkedHashSet<>());

		JSONObject generalConfigurationJSONObject =
			configurationJSONObject.getJSONObject("generalConfiguration");

		Assert.assertEquals(
			_jsonFactory.createJSONArray(
				new String[] {
					_OBJECT_DEFINITION_CLASS_NAME,
					"com.liferay.portal.kernel.model.Layout"
				}
			).toString(),
			generalConfigurationJSONObject.getJSONArray(
				"searchableAssetTypes"
			).toString());
	}

	@Test
	public void testRewriteElementInstances() throws Exception {
		JSONArray elementInstancesJSONArray = _jsonFactory.createJSONArray(
			_read("testRewriteElementInstances.json"));

		for (int i = 0; i < 2; i++) {
			Set<String> problems = new LinkedHashSet<>();

			_sxpBlueprintJSONRewriter.rewriteElementInstances(
				elementInstancesJSONArray, problems);

			JSONAssert.assertEquals(
				_read("testRewriteElementInstances.expected.json"),
				elementInstancesJSONArray.toString(), true);
			Assert.assertEquals(
				Collections.singleton(
					StringBundler.concat(
						"The element FILTER_ASSET_TYPE selects the asset type ",
						JournalArticle.class.getName(),
						", but it filters results, so it cannot select the ",
						"migrated content too")),
				problems);
		}
	}

	private String _read(String fileName) {
		return StringUtil.read(
			getClass(),
			StringBundler.concat(
				"dependencies/", getClass().getSimpleName(), ".", fileName));
	}

	private static final String _OBJECT_DEFINITION_CLASS_NAME =
		"com.liferay.object.model.ObjectDefinition#200";

	private final JSONFactory _jsonFactory = new JSONFactoryImpl();
	private SXPBlueprintJSONRewriter _sxpBlueprintJSONRewriter;
	private final SXPBlueprintReferenceMapper _sxpBlueprintReferenceMapper =
		Mockito.mock(SXPBlueprintReferenceMapper.class);

}