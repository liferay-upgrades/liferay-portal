/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.internal.manager.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.cms.migration.manager.CMSMigrationManager;
import com.liferay.cms.migration.report.CMSMigrationReport;
import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.depot.constants.DepotConstants;
import com.liferay.depot.exception.DepotEntryNameException;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.model.DepotEntryGroupRel;
import com.liferay.depot.service.DepotEntryGroupRelLocalService;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.background.task.service.BackgroundTaskLocalService;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskExecutor;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManager;
import com.liferay.portal.kernel.backgroundtask.constants.BackgroundTaskConstants;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Mateus Xavier
 */
@RunWith(Arquillian.class)
public class CMSMigrationManagerImplTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			SynchronousDestinationTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
	}

	@After
	public void tearDown() throws Exception {
		for (long backgroundTaskId : _backgroundTaskIds) {
			_backgroundTaskLocalService.deleteBackgroundTask(backgroundTaskId);
		}

		for (DepotEntry depotEntry : _depotEntries) {
			for (DepotEntryGroupRel depotEntryGroupRel :
					_depotEntryGroupRelLocalService.getDepotEntryGroupRels(
						depotEntry)) {

				_depotEntryGroupRelLocalService.deleteDepotEntryGroupRel(
					depotEntryGroupRel);
			}

			_depotEntryLocalService.deleteDepotEntry(depotEntry);
		}
	}

	@Test
	public void testAddCMSMigrationBackgroundTask() throws Exception {
		try {
			_addCMSMigrationBackgroundTask(0, StringPool.BLANK);

			Assert.fail();
		}
		catch (DepotEntryNameException depotEntryNameException) {
		}

		String depotEntryName = RandomTestUtil.randomString();

		DepotEntry depotEntry = _getDepotEntry(
			_addCMSMigrationBackgroundTask(0, depotEntryName));

		Assert.assertEquals(DepotConstants.TYPE_SPACE, depotEntry.getType());

		Group depotGroup = depotEntry.getGroup();

		Assert.assertEquals(
			depotEntryName, depotGroup.getName(LocaleUtil.getDefault()));

		Assert.assertEquals(
			depotEntry.getDepotEntryId(),
			_getDepotEntry(
				_addCMSMigrationBackgroundTask(
					depotEntry.getDepotEntryId(), RandomTestUtil.randomString())
			).getDepotEntryId());
	}

	@Test
	public void testDeleteCMSMigrationBackgroundTask() throws Exception {
		long backgroundTaskId = _addBackgroundTask(
			_group.getCompanyId(), StringPool.BLANK,
			RandomTestUtil.randomString());

		try {
			_cmsMigrationManager.deleteCMSMigrationBackgroundTask(
				backgroundTaskId);

			Assert.fail();
		}
		catch (PrincipalException principalException) {
		}

		backgroundTaskId = _addBackgroundTask(
			RandomTestUtil.nextLong(), StringPool.BLANK,
			_getTaskExecutorClassName());

		try {
			_cmsMigrationManager.deleteCMSMigrationBackgroundTask(
				backgroundTaskId);

			Assert.fail();
		}
		catch (PrincipalException principalException) {
		}

		backgroundTaskId = _addBackgroundTask(
			_group.getCompanyId(), StringPool.BLANK,
			_getTaskExecutorClassName());

		_backgroundTaskIds.remove(Long.valueOf(backgroundTaskId));

		Assert.assertNotNull(
			_backgroundTaskManager.fetchBackgroundTask(backgroundTaskId));

		_cmsMigrationManager.deleteCMSMigrationBackgroundTask(backgroundTaskId);

		Assert.assertNull(
			_backgroundTaskManager.fetchBackgroundTask(backgroundTaskId));
	}

	@Test
	public void testGetCMSMigrationBackgroundTasks() throws Exception {
		long backgroundTaskId1 = _addBackgroundTask(
			_group.getCompanyId(), StringPool.BLANK,
			_getTaskExecutorClassName());

		_addBackgroundTask(
			_group.getCompanyId(), StringPool.BLANK,
			RandomTestUtil.randomString());

		long backgroundTaskId2 = _addBackgroundTask(
			_group.getCompanyId(), StringPool.BLANK,
			_getTaskExecutorClassName());

		Assert.assertEquals(
			Arrays.asList(backgroundTaskId2, backgroundTaskId1),
			TransformUtil.transform(
				_cmsMigrationManager.getCMSMigrationBackgroundTasks(
					_group.getCompanyId()),
				backgroundTask -> {
					if (_backgroundTaskIds.contains(
							backgroundTask.getBackgroundTaskId())) {

						return backgroundTask.getBackgroundTaskId();
					}

					return null;
				}));
	}

	@Test
	public void testGetCMSMigrationReport() throws Exception {
		String error = RandomTestUtil.randomString();
		String key = RandomTestUtil.randomString();
		String warning = RandomTestUtil.randomString();

		CMSMigrationReport cmsMigrationReport =
			_cmsMigrationManager.getCMSMigrationReport(
				_backgroundTaskManager.getBackgroundTask(
					_addBackgroundTask(
						_group.getCompanyId(),
						JSONUtil.put(
							"counts", JSONUtil.put(key, 2)
						).put(
							"errors", JSONUtil.put(error)
						).put(
							"warnings", JSONUtil.put(warning)
						).toString(),
						_getTaskExecutorClassName())));

		Assert.assertEquals(
			HashMapBuilder.put(
				key, 2L
			).build(),
			cmsMigrationReport.getCounts());
		Assert.assertEquals(
			Arrays.asList(error), cmsMigrationReport.getErrors());
		Assert.assertEquals(
			Arrays.asList(warning), cmsMigrationReport.getWarnings());

		String statusMessage = RandomTestUtil.randomString();

		cmsMigrationReport = _cmsMigrationManager.getCMSMigrationReport(
			_backgroundTaskManager.getBackgroundTask(
				_addBackgroundTask(
					_group.getCompanyId(), statusMessage,
					_getTaskExecutorClassName())));

		Assert.assertEquals(
			Arrays.asList(statusMessage), cmsMigrationReport.getErrors());
	}

	private long _addBackgroundTask(
		long companyId, String statusMessage, String taskExecutorClassName) {

		com.liferay.portal.background.task.model.BackgroundTask backgroundTask =
			_backgroundTaskLocalService.createBackgroundTask(
				_counterLocalService.increment());

		Group companyGroup = _groupLocalService.fetchCompanyGroup(
			_group.getCompanyId());

		backgroundTask.setGroupId(companyGroup.getGroupId());

		backgroundTask.setCompanyId(companyId);
		backgroundTask.setName(RandomTestUtil.randomString());
		backgroundTask.setTaskExecutorClassName(taskExecutorClassName);
		backgroundTask.setTaskContextMap(new HashMap<>());
		backgroundTask.setCompleted(true);
		backgroundTask.setStatus(BackgroundTaskConstants.STATUS_SUCCESSFUL);
		backgroundTask.setStatusMessage(statusMessage);

		backgroundTask = _backgroundTaskLocalService.updateBackgroundTask(
			backgroundTask);

		_backgroundTaskIds.add(backgroundTask.getBackgroundTaskId());

		return backgroundTask.getBackgroundTaskId();
	}

	private BackgroundTask _addCMSMigrationBackgroundTask(
			long depotEntryId, String depotEntryName)
		throws Exception {

		BackgroundTask backgroundTask =
			_cmsMigrationManager.addCMSMigrationBackgroundTask(
				TestPropsValues.getUserId(), depotEntryId, depotEntryName,
				false, false, false, false, _group.getGroupId(),
				ServiceContextTestUtil.getServiceContext(_group.getGroupId()));

		_backgroundTaskIds.add(backgroundTask.getBackgroundTaskId());

		backgroundTask = _backgroundTaskManager.getBackgroundTask(
			backgroundTask.getBackgroundTaskId());

		Assert.assertEquals(
			backgroundTask.getStatusMessage(),
			BackgroundTaskConstants.STATUS_SUCCESSFUL,
			backgroundTask.getStatus());

		return backgroundTask;
	}

	private DepotEntry _getDepotEntry(BackgroundTask backgroundTask)
		throws Exception {

		Map<String, ?> taskContextMap = backgroundTask.getTaskContextMap();

		DepotEntry depotEntry = _depotEntryLocalService.getDepotEntry(
			GetterUtil.getLong(taskContextMap.get("depotEntryId")));

		if (!_depotEntries.contains(depotEntry)) {
			_depotEntries.add(depotEntry);
		}

		return depotEntry;
	}

	private String _getTaskExecutorClassName() {
		Class<?> clazz = _backgroundTaskExecutor.getClass();

		return clazz.getName();
	}

	@Inject(
		filter = "background.task.executor.class.name=com.liferay.cms.migration.internal.background.task.CMSMigrationBackgroundTaskExecutor"
	)
	private BackgroundTaskExecutor _backgroundTaskExecutor;

	private final List<Long> _backgroundTaskIds = new ArrayList<>();

	@Inject
	private BackgroundTaskLocalService _backgroundTaskLocalService;

	@Inject
	private BackgroundTaskManager _backgroundTaskManager;

	@Inject
	private CMSMigrationManager _cmsMigrationManager;

	@Inject
	private CounterLocalService _counterLocalService;

	private final List<DepotEntry> _depotEntries = new ArrayList<>();

	@Inject
	private DepotEntryGroupRelLocalService _depotEntryGroupRelLocalService;

	@Inject
	private DepotEntryLocalService _depotEntryLocalService;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private GroupLocalService _groupLocalService;

}