/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.project.templates.external.api.mock;

import com.liferay.maven.executor.MavenExecutor;
import com.liferay.project.templates.BaseProjectTemplatesTestCase;
import com.liferay.project.templates.extensions.util.Validator;
import com.liferay.project.templates.util.FileTestUtil;

import java.io.File;

import java.net.URI;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Albert Gomes Cabral
 */
@RunWith(Parameterized.class)
public class ProjectTemplatesExternalAPIMockTest
	implements BaseProjectTemplatesTestCase {

	@ClassRule
	public static final MavenExecutor mavenExecutor = new MavenExecutor();

	@Parameterized.Parameters(name = "Testcase-{index}: testing {1} {0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(
			new Object[][] {
				{"portal", "7.4.3.56"}, {"dxp", "2024.q1.1"},
				{"dxp", "2025.q3.1"}
			});
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		String gradleDistribution = System.getProperty("gradle.distribution");

		if (Validator.isNull(gradleDistribution)) {
			Properties properties = FileTestUtil.readProperties(
				"gradle-wrapper/gradle/wrapper/gradle-wrapper.properties");

			gradleDistribution = properties.getProperty("distributionUrl");
		}

		Assert.assertTrue(gradleDistribution.contains(GRADLE_WRAPPER_VERSION));

		_gradleDistribution = URI.create(gradleDistribution);
	}

	public ProjectTemplatesExternalAPIMockTest(
		String liferayProduct, String liferayVersion) {

		_liferayProduct = liferayProduct;
		_liferayVersion = liferayVersion;
	}

	@Test
	public void testBuildTemplateExternalAPIMock() throws Exception {
		String author = "Test Author";
		String className = "Foo";
		String name = "foo";
		String template = "external-api-mock";

		File gradleWorkspaceDir = buildWorkspace(
			temporaryFolder, "gradle", "gradleWS", _liferayVersion,
			mavenExecutor);

		String liferayWorkspaceProduct = getLiferayWorkspaceProduct(
			_liferayVersion);

		if (liferayWorkspaceProduct != null) {
			writeGradlePropertiesInWorkspace(
				gradleWorkspaceDir,
				"liferay.workspace.product=" + liferayWorkspaceProduct);
		}

		File gradleWorkspaceModulesDir = new File(
			gradleWorkspaceDir, "modules");

		File gradleProjectDir = buildTemplateWithGradle(
			gradleWorkspaceModulesDir, template, name, "--author", author,
			"--class-name", className, "--liferay-product", _liferayProduct,
			"--liferay-version", _liferayVersion);

		testContains(gradleProjectDir, "bnd.bnd", "Export-Package: foo");

		testExists(gradleProjectDir, "README.md");
		testExists(gradleProjectDir, "src/main/resources/foo/packageinfo");
		testExists(
			gradleProjectDir,
			"src/main/resources/mock-responses/customer.json");
		testExists(
			gradleProjectDir,
			"src/main/resources/mock-responses/customers.json");

		testGradlePortalReleaseDependency(gradleProjectDir, _liferayVersion);

		testContains(
			gradleProjectDir, "src/main/java/foo/FooAPIClient.java",
			"@author " + author, "public interface FooAPIClient");
		testContains(
			gradleProjectDir,
			"src/main/java/foo/configuration/FooAPIConfiguration.java",
			"id = \"foo.configuration.FooAPIConfiguration\"",
			"public boolean mockEnabled();",
			"public interface FooAPIConfiguration");
		testContains(
			gradleProjectDir,
			"src/main/java/foo/internal/FooAPIClientImpl.java",
			"configurationPid = \"foo.configuration.FooAPIConfiguration\"",
			"public class FooAPIClientImpl implements FooAPIClient");
		testContains(
			gradleProjectDir,
			"src/main/java/foo/internal/FooAPIMockResponseUtil.java",
			"\"/mock-responses\" + path + \".json\"",
			"public class FooAPIMockResponseUtil");

		String packagePrefix = getJavaxOrJakartaPackagePrefix(_liferayVersion);

		testContains(
			gradleProjectDir,
			"src/main/java/foo/internal/servlet/FooAPIMockServlet.java",
			"import " + packagePrefix + ".servlet.Servlet;",
			"osgi.http.whiteboard.context.path=/foo",
			"public class FooAPIMockServlet extends HttpServlet");

		testNotContains(gradleProjectDir, "build.gradle", "version: \"[0-9].*");

		File mavenWorkspaceDir = buildWorkspace(
			temporaryFolder, "maven", "mavenWS", _liferayVersion,
			mavenExecutor);

		File mavenModulesDir = new File(mavenWorkspaceDir, "modules");

		File mavenProjectDir = buildTemplateWithMaven(
			mavenModulesDir, mavenModulesDir, template, name, "com.test",
			mavenExecutor, "-DclassName=" + className,
			"-DliferayProduct=" + _liferayProduct,
			"-DliferayVersion=" + _liferayVersion, "-Dpackage=" + name);

		testContains(mavenProjectDir, "bnd.bnd", "-metatype: *");

		testContains(
			mavenProjectDir,
			"src/main/java/foo/internal/servlet/FooAPIMockServlet.java",
			"import " + packagePrefix + ".servlet.Servlet;");

		if (isBuildProjects()) {
			File gradleOutputDir = new File(gradleProjectDir, "build/libs");
			File mavenOutputDir = new File(mavenProjectDir, "target");

			buildProjects(
				_gradleDistribution, mavenExecutor, gradleWorkspaceDir,
				mavenProjectDir, gradleOutputDir, mavenOutputDir,
				":modules:" + name + GRADLE_TASK_PATH_BUILD);
		}
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private static URI _gradleDistribution;

	private final String _liferayProduct;
	private final String _liferayVersion;

}