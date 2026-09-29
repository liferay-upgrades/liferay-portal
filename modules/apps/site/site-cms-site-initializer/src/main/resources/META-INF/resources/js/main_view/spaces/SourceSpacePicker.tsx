/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Option, Picker} from '@clayui/core';
import {useId} from 'frontend-js-components-web';
import React from 'react';

import SpaceSticker from '../../common/components/SpaceSticker';
import {Space} from '../../common/types/Space';

const START_FROM_SCRATCH_KEY = 'start-from-scratch';

type Item = {
	key: string;
	name: string;
	space?: Space;
};

function Label({space}: {space?: Space}) {
	if (!space) {
		return <>{Liferay.Language.get('start-from-scratch')}</>;
	}

	return (
		<SpaceSticker
			displayType={space.settings?.logoColor}
			name={space.name}
			size="sm"
		/>
	);
}

const Trigger = React.forwardRef(
	(
		{
			id,
			space,
			...otherProps
		}: {
			id: string;
			space?: Space;
		},
		ref: React.Ref<HTMLButtonElement>
	) => {
		return (
			<button
				{...otherProps}
				className="d-flex form-control form-control-select"
				id={id}
				ref={ref}
				type="button"
			>
				<Label space={space} />
			</button>
		);
	}
);

export default function SourceSpacePicker({
	onChangeValue,
	spaces,
	value,
}: {
	onChangeValue: (space?: Space) => void;
	spaces: Space[];
	value: string;
}) {
	const id = useId();

	const items: Item[] = [
		{
			key: START_FROM_SCRATCH_KEY,
			name: Liferay.Language.get('start-from-scratch'),
		},
		...spaces.map((space) => ({
			key: space.externalReferenceCode,
			name: space.name,
			space,
		})),
	];

	return (
		<div className="form-group">
			<label htmlFor={id}>{Liferay.Language.get('copy-from')}</label>

			<Picker
				as={Trigger}
				id={id}
				items={items}
				messages={{
					itemDescribedby: Liferay.Language.get(
						'you-are-currently-on-a-text-element,-inside-of-a-list-box'
					),
					itemSelected: Liferay.Language.get('x-selected'),
					scrollToBottomAriaLabel:
						Liferay.Language.get('scroll-to-bottom'),
					scrollToTopAriaLabel: Liferay.Language.get('scroll-to-top'),
				}}
				onSelectionChange={(selectedKey: React.Key) => {
					const item = items.find(({key}) => key === selectedKey);

					onChangeValue(item?.space);
				}}
				selectedKey={value || START_FROM_SCRATCH_KEY}
				space={spaces.find(
					({externalReferenceCode}) => externalReferenceCode === value
				)}
			>
				{({key, name, space}: Item) => (
					<Option key={key} textValue={name}>
						<Label space={space} />
					</Option>
				)}
			</Picker>
		</div>
	);
}
