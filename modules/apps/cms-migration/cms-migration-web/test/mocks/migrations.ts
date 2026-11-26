/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {IMigration} from '../../src/main/resources/META-INF/resources/js/types/Migration';

export const COMPLETED_MIGRATION: IMigration = {
	backgroundTaskId: 1,
	completed: true,
	counts: [
		{label: 'Documents', value: 4},
		{label: 'Folders', value: 11},
	],
	createDate: '11/11/26, 10:58',
	errors: [],
	inProgress: false,
	name: 'Migration 1',
	progress: 0,
	statusDisplayType: 'success',
	statusLabel: 'Successful',
	warnings: [],
};

export const IN_PROGRESS_MIGRATION: IMigration = {
	backgroundTaskId: 2,
	completed: false,
	counts: [],
	createDate: '10/11/26, 18:49',
	errors: [],
	inProgress: true,
	itemTitle: 'Welcome',
	name: 'Migration 2',
	phaseLabel: 'Web Content',
	progress: 40,
	statusDisplayType: 'info',
	statusLabel: 'In Progress',
	warnings: [],
};

export const QUEUED_MIGRATION: IMigration = {
	backgroundTaskId: 3,
	completed: false,
	counts: [],
	createDate: '10/11/26, 18:49',
	errors: [],
	inProgress: false,
	name: 'Migration 3',
	progress: 0,
	statusDisplayType: 'info',
	statusLabel: 'Queued',
	warnings: [],
};

export const COMPLETED_MIGRATION_WITH_URL_MAPPINGS: IMigration = {
	...COMPLETED_MIGRATION,
	backgroundTaskId: 5,
	exportURLMappingsURL: 'http://localhost/export_url_mappings',
	urlMappings: [
		{
			actionRequired: true,
			newURL: '/documents/20/0/logo.png/uuid',
			oldURL: '/documents/d/guest/logo-png',
			status: 'update-manually',
			statusLabel: 'Update Manually',
			type: 'Documents',
		},
		{
			actionRequired: true,
			newURL: '/web/guest/web-content/space/welcome',
			oldURL: '/web/guest/w/welcome',
			status: 'update-manually',
			statusLabel: 'Update Manually',
			type: 'Web Content',
		},
		{
			actionRequired: false,
			newURL: '/web/guest/web-content/space/about',
			oldURL: '/web/guest/w/about',
			status: 'site-content',
			statusLabel: 'Not Needed',
			type: 'Web Content',
		},
	],
	urlMappingsActionRequiredCount: 2,
	urlMappingsCount: 3,
};
