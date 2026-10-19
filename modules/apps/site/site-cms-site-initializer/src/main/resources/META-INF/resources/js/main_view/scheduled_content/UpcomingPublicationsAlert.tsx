/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import {sub} from 'frontend-js-web';
import React, {useCallback, useEffect, useState} from 'react';

import {
	CMSSiteInitializerFDSNames,
	FDS_EVENT_DISPLAY_UPDATED,
} from '../../common/utils/constants';
import ScheduledContentService from './ScheduledContentService';
import {MILLISECONDS_PER_HOUR} from './constants';

export default function UpcomingPublicationsAlert({apiURL}: {apiURL: string}) {
	const [count, setCount] = useState(0);

	const fetchCount = useCallback(
		async (signal?: AbortSignal) => {
			const {count, error} =
				await ScheduledContentService.getScheduledContentCount(
					apiURL,
					{to: new Date(Date.now() + MILLISECONDS_PER_HOUR)},
					signal
				);

			if (error !== null) {
				console.error(
					'Failed to fetch the upcoming publications count',
					error
				);

				return;
			}

			if (!signal?.aborted) {
				setCount(count);
			}
		},
		[apiURL]
	);

	useEffect(() => {
		const controller = new AbortController();

		fetchCount(controller.signal);

		const handleDisplayUpdated = (event?: {id?: string}) => {
			if (
				event?.id?.endsWith(
					CMSSiteInitializerFDSNames.SCHEDULED_CONTENT_SECTION
				)
			) {
				fetchCount(controller.signal);
			}
		};

		Liferay.on(FDS_EVENT_DISPLAY_UPDATED, handleDisplayUpdated);

		return () => {
			controller.abort();

			Liferay.detach(FDS_EVENT_DISPLAY_UPDATED, handleDisplayUpdated);
		};
	}, [fetchCount]);

	if (!count) {
		return null;
	}

	return (
		<ClayAlert
			className="c-mb-0 c-mx-4"
			displayType="info"
			role="status"
			title={Liferay.Language.get('upcoming')}
		>
			{count === 1
				? Liferay.Language.get(
						'one-asset-will-be-published-in-the-next-hour'
					)
				: sub(
						Liferay.Language.get(
							'x-assets-will-be-published-in-the-next-hour'
						),
						String(count)
					)}
		</ClayAlert>
	);
}
