/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Albert Gomes Cabral
 */
public class MigrationStatusImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetPhaseLabel() {
		Assert.assertEquals(
			"Completed", _getPhaseLabel(MigrationStatus.PHASE_COMPLETED));
		Assert.assertEquals(
			"Copying Data", _getPhaseLabel(MigrationStatus.PHASE_DATA_LOAD));
		Assert.assertEquals(
			"Discovery", _getPhaseLabel(MigrationStatus.PHASE_DISCOVERY));
		Assert.assertEquals(
			"Error", _getPhaseLabel(MigrationStatus.PHASE_ERROR));
		Assert.assertEquals("Idle", _getPhaseLabel(MigrationStatus.PHASE_IDLE));
		Assert.assertEquals(
			"Creating Indexes",
			_getPhaseLabel(MigrationStatus.PHASE_INDEX_CREATION));
		Assert.assertEquals(
			"Creating Schema",
			_getPhaseLabel(MigrationStatus.PHASE_SCHEMA_CREATION));
		Assert.assertEquals(
			"Validating Schema",
			_getPhaseLabel(MigrationStatus.PHASE_SCHEMA_VALIDATION));
	}

	@Test
	public void testGetPhaseLabelAfterSetPhase() {
		MigrationStatusImpl migrationStatusImpl = new MigrationStatusImpl(
			MigrationStatus.PHASE_INDEX_CREATION, "Creating indexes");

		migrationStatusImpl.setPhase(MigrationStatus.PHASE_SCHEMA_VALIDATION);

		Assert.assertEquals(
			MigrationStatus.PHASE_SCHEMA_VALIDATION,
			migrationStatusImpl.getPhase());
		Assert.assertEquals(
			"Validating Schema", migrationStatusImpl.getPhaseLabel());
	}

	private String _getPhaseLabel(int phase) {
		MigrationStatusImpl migrationStatusImpl = new MigrationStatusImpl(
			phase, "Migrating");

		return migrationStatusImpl.getPhaseLabel();
	}

}