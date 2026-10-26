/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton, {ClayButtonWithIcon} from '@clayui/button';
import ClayIcon from '@clayui/icon';
import ClaySticker from '@clayui/sticker';
import {dateUtils} from 'frontend-js-web';
import React, {useEffect, useState} from 'react';

import SpaceSticker from '../../common/components/SpaceSticker';
import StatusLabel from '../../common/components/StatusLabel';
import {StickerConfig} from '../../common/types/StickerConfig';
import {getAssetTitle} from '../../common/utils/getAssetTitle';
import {
	RecentlyViewedAsset,
	clearRecentlyViewedEntries,
	getRecentlyViewedAssets,
	removeRecentlyViewedEntry,
} from '../../common/utils/recentlyViewed';
import {getFileMimeTypeObjectDefinitionStickerValue} from '../props_transformer/utils/transformViewsItemProps';

import '../../../css/home/RecentlyViewed.scss';

export default function RecentlyViewed(stickerConfig: StickerConfig) {
	const [recentlyViewedAssets, setRecentlyViewedAssets] = useState<
		RecentlyViewedAsset[] | null
	>(null);

	useEffect(() => {
		getRecentlyViewedAssets().then(setRecentlyViewedAssets);
	}, []);

	if (!recentlyViewedAssets) {
		return null;
	}

	return (
		<div className="recently-viewed">
			<div className="align-items-center d-flex justify-content-between">
				<span
					aria-level={2}
					className="recently-viewed-title"
					role="heading"
				>
					<ClayIcon symbol="time" />

					{Liferay.Language.get('recently-viewed')}
				</span>

				{!!recentlyViewedAssets.length && (
					<ClayButton
						className="font-weight-semi-bold"
						displayType="link"
						onClick={() => {
							clearRecentlyViewedEntries();

							setRecentlyViewedAssets([]);
						}}
						size="sm"
					>
						{Liferay.Language.get('clear-history')}
					</ClayButton>
				)}
			</div>

			{recentlyViewedAssets.length ? (
				<ul className="list-unstyled recently-viewed-grid">
					{recentlyViewedAssets.map((recentlyViewedAsset) => (
						<RecentlyViewedTile
							key={recentlyViewedAsset.id}
							onRemove={() => {
								removeRecentlyViewedEntry(
									recentlyViewedAsset.id
								);

								setRecentlyViewedAssets(
									recentlyViewedAssets.filter(
										({id}) => id !== recentlyViewedAsset.id
									)
								);
							}}
							recentlyViewedAsset={recentlyViewedAsset}
							stickerConfig={stickerConfig}
						/>
					))}
				</ul>
			) : (
				<div className="recently-viewed-empty-state">
					<span className="font-weight-semi-bold">
						{Liferay.Language.get('nothing-viewed-yet')}
					</span>

					<span>
						{Liferay.Language.get(
							'the-assets-you-open-will-show-up-here'
						)}
					</span>
				</div>
			)}
		</div>
	);
}

function RecentlyViewedTile({
	onRemove,
	recentlyViewedAsset: {asset, href, space, viewedAt},
	stickerConfig: {
		fileMimeTypeCssClasses,
		fileMimeTypeIcons,
		objectDefinitionCssClasses,
		objectDefinitionIcons,
	},
}: {
	onRemove: () => void;
	recentlyViewedAsset: RecentlyViewedAsset;
	stickerConfig: StickerConfig;
}) {
	if (!asset) {
		return (
			<li className="recently-viewed-tile recently-viewed-tile-unavailable">
				<ClaySticker displayType="light">
					<ClayIcon symbol="trash" />
				</ClaySticker>

				<div className="recently-viewed-tile-meta">
					{Liferay.Language.get('no-longer-available')}
				</div>
			</li>
		);
	}

	return (
		<li className="recently-viewed-tile">
			<div className="align-items-center d-flex justify-content-between">
				<ClaySticker
					className={getFileMimeTypeObjectDefinitionStickerValue(
						fileMimeTypeCssClasses,
						objectDefinitionCssClasses,
						asset
					)}
				>
					<ClayIcon
						symbol={getFileMimeTypeObjectDefinitionStickerValue(
							fileMimeTypeIcons,
							objectDefinitionIcons,
							asset
						)}
					/>
				</ClaySticker>

				<StatusLabel
					expirationDate={asset.embedded.expirationDate ?? undefined}
					label={asset.embedded.status.label}
				/>
			</div>

			<a
				className="recently-viewed-tile-title stretched-link"
				href={href}
			>
				{getAssetTitle(asset)}
			</a>

			<div className="recently-viewed-tile-meta">
				{space && (
					<SpaceSticker
						displayType={space.settings?.logoColor}
						name={space.name}
						size="xs"
					/>
				)}

				<span>{`· ${dateUtils.fromNow(new Date(viewedAt))}`}</span>
			</div>

			<ClayButtonWithIcon
				aria-label={Liferay.Language.get('remove-from-history')}
				borderless
				className="recently-viewed-tile-remove"
				displayType="secondary"
				onClick={onRemove}
				size="sm"
				symbol="times"
				title={Liferay.Language.get('remove-from-history')}
			/>
		</li>
	);
}
