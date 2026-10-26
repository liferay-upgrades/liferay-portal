/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {dataApiHelpersTest} from '../../../fixtures/dataApiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {clickAndExpectToBeVisible} from '../../../utils/clickAndExpectToBeVisible';
import getRandomString from '../../../utils/getRandomString';
import {cmsPagesTest} from './fixtures/cmsPagesTest';

const test = mergeTests(cmsPagesTest, dataApiHelpersTest, loginTest());

test(
	'Recently created page lists the new assets of the spaces, newest first',
	{tag: '@ECU-20759'},
	async ({apiHelpers, assetsPage, page}) => {
		const spaceName = `Space ${getRandomString()}`;

		await apiHelpers.headlessAssetLibrary.createAssetLibrary({
			name: spaceName,
			type: 'Space',
		});

		// Create two contents, one after the other

		const olderTitle = `Content ${getRandomString()}`;

		await apiHelpers.objectEntry.postObjectEntry(
			{
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				title: olderTitle,
			},
			'cms/basic-web-contents',
			spaceName
		);

		const newerTitle = `Content ${getRandomString()}`;

		await apiHelpers.objectEntry.postObjectEntry(
			{
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				title: newerTitle,
			},
			'cms/basic-web-contents',
			spaceName
		);

		// Open the page from the left navigation

		await assetsPage.gotoAll();

		await clickAndExpectToBeVisible({
			target: page.getByRole('heading', {name: 'Recently Created'}),
			trigger: page
				.getByRole('menuitem', {exact: true, name: 'Recently Created'})
				.first(),
		});

		await expect(
			page.getByText('Assets created in the last 7 days, newest first.')
		).toBeVisible();

		// Check the newest content comes first

		await expect(assetsPage.getItem(newerTitle)).toBeVisible();
		await expect(assetsPage.getItem(olderTitle)).toBeVisible();

		const rowTitles = await assetsPage.table.bodyRows.allInnerTexts();

		expect(
			rowTitles.findIndex((rowTitle) => rowTitle.includes(newerTitle))
		).toBeLessThan(
			rowTitles.findIndex((rowTitle) => rowTitle.includes(olderTitle))
		);
	}
);
