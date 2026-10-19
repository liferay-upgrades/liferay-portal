/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayEmptyState from '@clayui/empty-state';
import ClayLink from '@clayui/link';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {sub} from 'frontend-js-web';
import React, {useEffect, useState} from 'react';

import '../../../css/home/MySpaces.scss';
import SpaceSticker from '../../common/components/SpaceSticker';
import SpaceService from '../../common/services/SpaceService';
import {Space} from '../../common/types/Space';
import dateFormat from '../../common/utils/dateFormat';

type SpaceTile = Space & {assetsCount?: number};

export default function MySpaces({
	allSpacesURL,
	baseSpaceURL,
}: {
	allSpacesURL: string;
	baseSpaceURL: string;
}) {
	const [loading, setLoading] = useState(true);
	const [spaces, setSpaces] = useState<SpaceTile[]>([]);

	useEffect(() => {
		const abortController = new AbortController();

		const loadSpaces = async () => {
			const {data} = await SpaceService.getRecentSpaces(
				abortController.signal
			);

			const items = data?.items ?? [];

			const assetsCounts = await Promise.all(
				items.map(async (space) => {
					const {data} = await SpaceService.getSpaceAssetsCount(
						space.id,
						abortController.signal
					);

					return data?.totalCount;
				})
			);

			if (abortController.signal.aborted) {
				return;
			}

			setLoading(false);
			setSpaces(
				items.map((space, index) => ({
					...space,
					assetsCount: assetsCounts[index],
				}))
			);
		};

		loadSpaces();

		return () => abortController.abort();
	}, []);

	return (
		<div className="container-fluid-max my-spaces p-2 p-sm-3">
			<div className="align-items-center d-flex justify-content-between mb-3">
				<span
					aria-level={2}
					className="font-weight-semi-bold text-4"
					role="heading"
				>
					{Liferay.Language.get('my-spaces')}
				</span>

				<ClayLink
					borderless
					className="font-weight-semi-bold text-3"
					data-canonical-name={sub(
						Liferay.Language.get('view-x'),
						Liferay.Language.get('my-spaces')
					)}
					href={allSpacesURL}
					small
				>
					{Liferay.Language.get('view-all')}
				</ClayLink>
			</div>

			{loading ? (
				<ClayLoadingIndicator displayType="secondary" size="sm" />
			) : spaces.length ? (
				<ul className="list-unstyled mb-0 row">
					{spaces.map((space) => (
						<li
							className="col-lg-3 col-sm-6 d-flex mb-3 mb-lg-0"
							key={space.id}
						>
							<a
								className="align-items-start d-flex flex-fill my-spaces-tile text-decoration-none"
								href={baseSpaceURL + space.id}
							>
								<SpaceSticker
									displayType={space.settings?.logoColor}
									hideName
									name={space.name}
								/>

								<div className="ml-2 text-truncate">
									<div className="font-weight-semi-bold text-3 text-dark text-truncate">
										{space.name}
									</div>

									{space.assetsCount !== undefined && (
										<div className="text-2 text-secondary">
											{sub(
												space.assetsCount === 1
													? Liferay.Language.get(
															'x-asset'
														)
													: Liferay.Language.get(
															'x-assets'
														),
												space.assetsCount.toLocaleString(
													Liferay.ThemeDisplay.getBCP47LanguageId()
												)
											)}
										</div>
									)}

									{space.dateModified && (
										<div className="text-2 text-secondary">
											{sub(
												Liferay.Language.get(
													'modified-x'
												),
												dateFormat(
													{
														day: 'numeric',
														month: 'short',
														year: 'numeric',
													},
													space.dateModified
												)
											)}
										</div>
									)}
								</div>
							</a>
						</li>
					))}
				</ul>
			) : (
				<ClayEmptyState
					className="my-4 text-center"
					description={Liferay.Language.get(
						'you-are-not-a-member-of-any-space.-ask-an-administrator-to-add-you-to-one'
					)}
					small
					title={Liferay.Language.get('no-spaces-yet')}
				/>
			)}
		</div>
	);
}
