/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayButton from '@clayui/button';
import ClayForm, {ClayInput} from '@clayui/form';
import {fetch} from 'frontend-js-web';
import React, {useState} from 'react';

interface MigrationFormProps {
	namespace: string;
	onStarted: () => void;
	startMigrationURL: string;
	testConnectionURL: string;
}

type Side = 'source' | 'target';

interface TestState {
	message?: string;
	status: 'error' | 'success' | 'testing';
}

function getErrorMessage(errorCode: string): string {
	if (errorCode === 'connectionInformationRequired') {
		return Liferay.Language.get(
			'both-source-and-target-connection-details-are-required'
		);
	}

	if (errorCode === 'migrationAlreadyRunning') {
		return Liferay.Language.get('a-database-migration-is-already-running');
	}

	if (errorCode === 'targetDatabaseMustBePostgreSQL') {
		return Liferay.Language.get(
			'the-target-database-url-must-be-postgresql'
		);
	}

	return Liferay.Language.get('an-unexpected-error-occurred');
}

const MigrationForm: React.FC<MigrationFormProps> = ({
	namespace,
	onStarted,
	startMigrationURL,
	testConnectionURL,
}) => {
	const [values, setValues] = useState({
		migrationName: '',
		sourceJDBCURL: '',
		sourcePassword: '',
		sourceUserName: '',
		targetJDBCURL: '',
		targetPassword: '',
		targetUserName: '',
	});
	const [errorCode, setErrorCode] = useState<string | null>(null);
	const [submitting, setSubmitting] = useState(false);
	const [testStates, setTestStates] = useState<{
		source?: TestState;
		target?: TestState;
	}>({});

	const setValue = (name: string, value: string) =>
		setValues((previous) => ({...previous, [name]: value}));

	const handleSubmit = (event: React.FormEvent) => {
		event.preventDefault();

		setErrorCode(null);
		setSubmitting(true);

		const body = new URLSearchParams();

		Object.entries(values).forEach(([name, value]) =>
			body.append(namespace + name, value)
		);

		fetch(startMigrationURL, {body, method: 'POST'})
			.then((response) => response.json())
			.then((json) => {
				setSubmitting(false);

				if (json.started) {
					onStarted();
				}
				else {
					setErrorCode(json.error || 'unexpected');
				}
			})
			.catch(() => {
				setSubmitting(false);
				setErrorCode('unexpected');
			});
	};

	const handleTest = (side: Side) => {
		setTestStates((previous) => ({
			...previous,
			[side]: {status: 'testing'},
		}));

		const body = new URLSearchParams();

		body.append(namespace + 'jdbcURL', values[`${side}JDBCURL`]);
		body.append(namespace + 'userName', values[`${side}UserName`]);
		body.append(namespace + 'password', values[`${side}Password`]);

		fetch(testConnectionURL, {body, method: 'POST'})
			.then((response) => response.json())
			.then((json) =>
				setTestStates((previous) => ({
					...previous,
					[side]: json.valid
						? {status: 'success'}
						: {message: json.message, status: 'error'},
				}))
			)
			.catch(() =>
				setTestStates((previous) => ({
					...previous,
					[side]: {status: 'error'},
				}))
			);
	};

	const renderField = (
		name: keyof typeof values,
		label: string,
		type: string,
		placeholder?: string
	) => (
		<ClayForm.Group>
			<label htmlFor={name}>{label}</label>

			<ClayInput
				id={name}
				onChange={(event) => setValue(name, event.target.value)}
				placeholder={placeholder}
				type={type}
				value={values[name]}
			/>
		</ClayForm.Group>
	);

	const renderTest = (side: Side) => {
		const testState = testStates[side];

		return (
			<>
				<ClayButton
					disabled={
						testState?.status === 'testing' ||
						!values[`${side}JDBCURL`] ||
						!values[`${side}UserName`]
					}
					displayType="secondary"
					onClick={() => handleTest(side)}
					type="button"
				>
					{Liferay.Language.get('test-database-connection')}
				</ClayButton>

				{testState?.status === 'success' && (
					<ClayAlert className="mt-3" displayType="success">
						{Liferay.Language.get('the-connection-was-successful')}
					</ClayAlert>
				)}

				{testState?.status === 'error' && (
					<ClayAlert className="mt-3" displayType="danger">
						{Liferay.Language.get(
							'the-connection-could-not-be-established'
						)}

						{testState.message ? ` ${testState.message}` : ''}
					</ClayAlert>
				)}
			</>
		);
	};

	return (
		<ClayForm className="sheet" onSubmit={handleSubmit}>
			{errorCode && (
				<ClayAlert
					displayType="danger"
					title={Liferay.Language.get('error')}
				>
					{getErrorMessage(errorCode)}
				</ClayAlert>
			)}

			{renderField(
				'migrationName',
				Liferay.Language.get('migration-name'),
				'text',
				Liferay.Language.get(
					'optional-a-name-is-generated-if-left-blank'
				)
			)}

			<div className="row">
				<div className="col-md-6">
					<h4 className="sheet-subtitle">
						{Liferay.Language.get('source-database')}
					</h4>

					{renderField(
						'sourceJDBCURL',
						Liferay.Language.get('jdbc-url'),
						'text',
						'jdbc:mysql://host:3306/lportal'
					)}

					{renderField(
						'sourceUserName',
						Liferay.Language.get('user-name'),
						'text'
					)}

					{renderField(
						'sourcePassword',
						Liferay.Language.get('password'),
						'password'
					)}

					{renderTest('source')}
				</div>

				<div className="col-md-6">
					<h4 className="sheet-subtitle">
						{Liferay.Language.get('target-database-postgresql')}
					</h4>

					{renderField(
						'targetJDBCURL',
						Liferay.Language.get('jdbc-url'),
						'text',
						'jdbc:postgresql://host:5432/lportal'
					)}

					{renderField(
						'targetUserName',
						Liferay.Language.get('user-name'),
						'text'
					)}

					{renderField(
						'targetPassword',
						Liferay.Language.get('password'),
						'password'
					)}

					{renderTest('target')}
				</div>
			</div>

			<ClayButton className="mt-4" disabled={submitting} type="submit">
				{Liferay.Language.get('start-migration')}
			</ClayButton>
		</ClayForm>
	);
};

export default MigrationForm;
