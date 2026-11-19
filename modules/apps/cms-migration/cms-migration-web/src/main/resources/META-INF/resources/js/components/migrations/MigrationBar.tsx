/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import ClayProgressBar from '@clayui/progress-bar';
import classNames from 'classnames';
import React from 'react';

import {IMigrationBar} from '../../types/Migration';

export default function MigrationBar({
	bar,
	numberFormat,
}: {
	bar: IMigrationBar;
	numberFormat: Intl.NumberFormat;
}) {
	const {completed, current, label, pending, progress, total} = bar;

	const complete = progress === 100;
	const highlighted = current && !complete;
	const notStarted = !complete && !current && !completed;

	return (
		<>
			<div
				className={classNames({
					'font-weight-semi-bold text-info': highlighted,
					'text-secondary': notStarted,
					'text-success': complete,
				})}
			>
				{complete && <ClayIcon className="mr-2" symbol="check" />}

				{label}
			</div>

			<div
				className="text-secondary"
				title={
					pending
						? Liferay.Language.get('cms-migration-total-pending')
						: undefined
				}
			>
				{`${numberFormat.format(completed)} / ${numberFormat.format(total)}${pending ? '+' : ''}`}
			</div>

			<ClayProgressBar
				aria-label={label}
				fillBarClassName="rounded-pill"
				value={progress}
			>
				{`${progress}%`}
			</ClayProgressBar>
		</>
	);
}
