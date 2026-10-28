/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLink from '@clayui/link';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {dateUtils, sub} from 'frontend-js-web';
import React, {useEffect, useState} from 'react';

import {FDS_FILTER_ID, WORKFLOW_STATUS} from '../../common/utils/constants';
import {DraftAssets, getDrafts} from '../../common/utils/myDrafts';
import getAllSectionHref from '../dashboard/governance/getAllSectionHref';

import '../../../css/home/MyDrafts.scss';

type Props = {
	allSectionFDSName: string;
	editContentItemURL: string;
	maxItems: number;
	redirect: string;
	userId: number;
};

function getEditURL(editContentItemURL: string, id: number, redirect: string) {
	const url = new URL(editContentItemURL);

	url.searchParams.set('objectEntryId', String(id));
	url.searchParams.set('redirect', redirect);

	return url.toString();
}

export default function MyDrafts({
	allSectionFDSName,
	editContentItemURL,
	maxItems,
	redirect,
	userId,
}: Props) {
	const [drafts, setDrafts] = useState<DraftAssets[] | null>(null);

	useEffect(() => {
		getDrafts(userId, maxItems).then(setDrafts);
	}, [maxItems, userId]);

	return (
		<div className="container-fluid-max my-drafts p-2 p-sm-3">
			<div className="align-items-center d-flex justify-content-between mb-3">
				<span
					aria-level={2}
					className="font-weight-semi-bold text-4"
					role="heading"
				>
					{Liferay.Language.get('my-drafts')}
				</span>

				<ClayLink
					aria-label={sub(
						Liferay.Language.get('view-all-x'),
						Liferay.Language.get('my-drafts')
					)}
					borderless
					className="font-weight-semi-bold text-3"
					href={getAllSectionHref(allSectionFDSName, {
						filters: [
							{
								id: FDS_FILTER_ID.STATUS,
								selectedData: {
									exclude: false,
									selectedItems: [
										{value: WORKFLOW_STATUS.DRAFT},
									],
								},
							},
						],
					})}
					small
				>
					{Liferay.Language.get('view-all')}
				</ClayLink>
			</div>

			{!drafts ? (
				<ClayLoadingIndicator displayType="secondary" size="sm" />
			) : drafts.length ? (
				<ul className="list-unstyled my-drafts-list">
					{drafts.map((draft) => (
						<li className="my-drafts-item" key={draft.id}>
							<a
								className="my-drafts-item-title stretched-link"
								href={getEditURL(
									editContentItemURL,
									draft.id,
									redirect
								)}
								title={draft.title}
							>
								{draft.title}
							</a>

							<div className="my-drafts-item-meta">
								<span className="text-truncate">
									{draft.scopeKey}
								</span>

								<span className="flex-shrink-0">
									{`· ${dateUtils.fromNow(
										new Date(draft.dateModified)
									)}`}
								</span>
							</div>
						</li>
					))}
				</ul>
			) : (
				<div className="my-drafts-empty-state">
					<span className="font-weight-semi-bold">
						{Liferay.Language.get('no-drafts')}
					</span>

					<span>
						{Liferay.Language.get(
							'the-drafts-you-create-will-show-up-here'
						)}
					</span>
				</div>
			)}
		</div>
	);
}
