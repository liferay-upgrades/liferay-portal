/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayButton from '@clayui/button';
import ClayForm, {ClayCheckbox, ClayInput, ClaySelect} from '@clayui/form';
import ClayLayout from '@clayui/layout';
import React, {useState} from 'react';

import {IOption} from '../../types/Form';
import {CONTENT_TYPES, CREATE_NEW_SPACE} from '../../utils/constants';
import FieldWrapper from '../forms/FieldWrapper';
import HelpTooltipIcon from '../forms/HelpTooltipIcon';

interface IProps {
	groups: IOption[];
	migrationIncomplete: boolean;
	portletNamespace: string;
	spaces: IOption[];
	startCMSMigrationURL: string;
	title: string;
}

const UncontrolledClayCheckbox = ClayCheckbox as unknown as React.ComponentType<
	Omit<React.ComponentProps<typeof ClayCheckbox>, 'checked'>
>;

export default function Configuration({
	groups,
	migrationIncomplete,
	portletNamespace,
	spaces,
	startCMSMigrationURL,
	title,
}: IProps) {
	const [depotEntryId, setDepotEntryId] = useState(
		spaces.length ? spaces[0].value : CREATE_NEW_SPACE
	);
	const [submitting, setSubmitting] = useState(false);

	return (
		<ClayLayout.Sheet>
			<form
				action={startCMSMigrationURL}
				method="post"
				onSubmit={() => setSubmitting(true)}
			>
				<ClayLayout.SheetHeader>
					<h2 className="sheet-title">{title}</h2>

					<div className="sheet-text">
						{Liferay.Language.get('cms-migration-description')}
					</div>
				</ClayLayout.SheetHeader>

				<ClayLayout.SheetSection>
					<FieldWrapper
						fieldId={`${portletNamespace}sourceGroupId`}
						label={Liferay.Language.get(
							'cms-migration-source-site'
						)}
						required
					>
						<ClaySelect
							id={`${portletNamespace}sourceGroupId`}
							name={`${portletNamespace}sourceGroupId`}
							required
						>
							{groups.map(({label, value}) => (
								<ClaySelect.Option
									key={value}
									label={label}
									value={value}
								/>
							))}
						</ClaySelect>
					</FieldWrapper>

					<FieldWrapper
						fieldId={`${portletNamespace}depotEntryId`}
						label={Liferay.Language.get(
							'cms-migration-target-space'
						)}
					>
						<ClaySelect
							id={`${portletNamespace}depotEntryId`}
							name={`${portletNamespace}depotEntryId`}
							onChange={(event) =>
								setDepotEntryId(event.target.value)
							}
							value={depotEntryId}
						>
							<ClaySelect.Option
								label={Liferay.Language.get(
									'cms-migration-create-new-space'
								)}
								value={CREATE_NEW_SPACE}
							/>

							{spaces.map(({label, value}) => (
								<ClaySelect.Option
									key={value}
									label={label}
									value={value}
								/>
							))}
						</ClaySelect>
					</FieldWrapper>

					{depotEntryId === CREATE_NEW_SPACE && (
						<FieldWrapper
							fieldId={`${portletNamespace}spaceName`}
							label={Liferay.Language.get(
								'cms-migration-new-space-name'
							)}
						>
							<ClayInput
								id={`${portletNamespace}spaceName`}
								name={`${portletNamespace}spaceName`}
								type="text"
							/>
						</FieldWrapper>
					)}
				</ClayLayout.SheetSection>

				<ClayLayout.SheetSection>
					<h3 className="sheet-subtitle">
						{Liferay.Language.get('cms-migration-content-types')}
					</h3>

					{CONTENT_TYPES.map(
						({defaultChecked, help, label, name}) => (
							<ClayForm.Group
								className="align-items-start d-flex"
								key={name}
							>
								<UncontrolledClayCheckbox
									containerProps={{
										className:
											'custom-control-primary mb-0',
									}}
									defaultChecked={defaultChecked ?? true}
									label={label}
									name={`${portletNamespace}${name}`}
									value="true"
								/>

								{help && <HelpTooltipIcon message={help} />}
							</ClayForm.Group>
						)
					)}

					<FieldWrapper
						fieldId={`${portletNamespace}titleObjectFieldNames`}
						label={Liferay.Language.get(
							'cms-migration-title-object-field-names'
						)}
					>
						<ClayInput
							id={`${portletNamespace}titleObjectFieldNames`}
							name={`${portletNamespace}titleObjectFieldNames`}
							type="text"
						/>

						<ClayForm.FeedbackGroup>
							<ClayForm.Text>
								{Liferay.Language.get(
									'cms-migration-title-object-field-names-help'
								)}
							</ClayForm.Text>
						</ClayForm.FeedbackGroup>
					</FieldWrapper>
				</ClayLayout.SheetSection>

				{migrationIncomplete && (
					<ClayAlert
						displayType="warning"
						title={Liferay.Language.get('warning')}
					>
						{Liferay.Language.get(
							'cms-migration-only-one-migration-can-run-at-a-time'
						)}
					</ClayAlert>
				)}

				<ClayLayout.SheetFooter>
					<ClayButton
						disabled={migrationIncomplete || submitting}
						type="submit"
					>
						{Liferay.Language.get('cms-migration-start')}
					</ClayButton>
				</ClayLayout.SheetFooter>
			</form>
		</ClayLayout.Sheet>
	);
}
