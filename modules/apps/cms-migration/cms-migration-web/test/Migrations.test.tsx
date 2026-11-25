/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

// eslint-disable-next-line @liferay/portal/no-cross-module-deep-import
import {checkAccessibility} from '@liferay/layout-js-components-web/test/__lib__/index';
import {act, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import '@testing-library/jest-dom';

import Migrations from '../src/main/resources/META-INF/resources/js/Migrations';
import {
	COMPLETED_MIGRATION,
	IN_PROGRESS_MIGRATION,
	QUEUED_MIGRATION,
	SAVE_LOGS_MIGRATION,
} from './mocks/migrations';

const fetchMock = fetch as unknown as jest.Mock<Promise<Response>> & {
	mockRejectOnce: (error: Error) => void;
	mockResponseOnce: (body: string | (() => Promise<string>)) => void;
};

describe('Migrations', () => {
	const languageGetImplementation = (
		Liferay.Language.get as jest.Mock
	).getMockImplementation();
	const location = window.location;
	const reload = jest.fn();

	beforeEach(() => {
		jest.clearAllMocks();
		jest.useFakeTimers();

		Object.defineProperty(window, 'location', {
			configurable: true,
			value: {...location, reload},
		});
	});

	afterEach(() => {
		(Liferay.Language.get as jest.Mock).mockImplementation(
			languageGetImplementation
		);

		jest.useRealTimers();

		window.sessionStorage.clear();

		Object.defineProperty(window, 'location', {
			configurable: true,
			value: location,
		});
	});

	it('explains that no migrations have been run yet', () => {
		render(<Migrations migrationIncomplete={false} migrations={[]} />);

		expect(
			screen.getByRole('heading', {name: 'cms-migration-migrations'})
		).toBeInTheDocument();
		expect(
			screen.getByText('cms-migration-no-migrations')
		).toBeInTheDocument();
	});

	it('shows the counts of a completed migration without errors or warnings', () => {
		render(
			<Migrations
				migrationIncomplete={false}
				migrations={[COMPLETED_MIGRATION]}
			/>
		);

		const [documents, folders] = screen.getAllByRole('listitem');

		expect(documents).toHaveTextContent('4 Documents');
		expect(folders).toHaveTextContent('11 Folders');

		expect(
			screen.queryByRole('button', {name: 'cms-migration-x-errors'})
		).not.toBeInTheDocument();
		expect(
			screen.queryByRole('button', {name: 'cms-migration-x-warnings'})
		).not.toBeInTheDocument();
	});

	it('does not reload the page when no migration is incomplete', () => {
		render(
			<Migrations
				migrationIncomplete={false}
				migrations={[COMPLETED_MIGRATION]}
			/>
		);

		act(() => {
			jest.runAllTimers();
		});

		expect(reload).not.toHaveBeenCalled();
	});

	it('has no accessibility violations', async () => {
		jest.useRealTimers();

		fetchMock.mockResponseOnce(() => new Promise(() => {}));

		const {container} = render(
			<Migrations
				migrationIncomplete={true}
				migrations={[SAVE_LOGS_MIGRATION, IN_PROGRESS_MIGRATION]}
			/>
		);

		await checkAccessibility({bestPractices: true, context: container});

		await userEvent.click(
			screen.getByRole('button', {name: 'save-logs Migration 1'})
		);

		await checkAccessibility({bestPractices: true, context: container});
	});

	it('lists the errors and the warnings of a completed migration', () => {
		render(
			<Migrations
				migrationIncomplete={false}
				migrations={[
					{
						...COMPLETED_MIGRATION,
						counts: [],
						errors: ['Error 1'],
						warnings: ['Warning 1', 'Warning 2'],
					},
				]}
			/>
		);

		const [, errors, warnings] = screen.getAllByRole('region');

		expect(
			screen.getByRole('button', {name: 'cms-migration-x-errors'})
		).toHaveAttribute('aria-expanded', 'false');
		expect(within(errors).getByText('Error 1')).toBeInTheDocument();
		expect(
			screen.getByRole('button', {name: 'cms-migration-x-warnings'})
		).toHaveAttribute('aria-expanded', 'false');
		expect(within(warnings).getAllByRole('listitem')).toHaveLength(2);
	});

	it('shows the progress of a migration in progress and reloads the page', () => {
		render(
			<Migrations
				migrationIncomplete={true}
				migrations={[IN_PROGRESS_MIGRATION]}
			/>
		);

		expect(screen.getByText('In Progress')).toBeInTheDocument();
		expect(screen.getByText('Web Content')).toBeInTheDocument();
		expect(screen.getByText('Welcome')).toBeInTheDocument();
		expect(screen.getByRole('progressbar')).toHaveAttribute(
			'aria-valuenow',
			'40'
		);

		expect(reload).not.toHaveBeenCalled();

		act(() => {
			jest.advanceTimersByTime(5000);
		});

		expect(reload).toHaveBeenCalledTimes(1);
	});

	it('shows only the name, the date, and the status of a queued migration', () => {
		render(
			<Migrations
				migrationIncomplete={true}
				migrations={[QUEUED_MIGRATION]}
			/>
		);

		expect(
			screen.getByRole('heading', {name: 'Migration 3'})
		).toBeInTheDocument();
		expect(screen.getByText('10/11/26, 18:49')).toBeInTheDocument();
		expect(screen.getByText('Queued')).toBeInTheDocument();
		expect(
			screen.queryByText('cms-migration-no-migrations')
		).not.toBeInTheDocument();
		expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
		expect(screen.queryByRole('list')).not.toBeInTheDocument();
	});

	it('renders the status label with its display type', () => {
		render(
			<Migrations
				migrationIncomplete={true}
				migrations={[
					{...QUEUED_MIGRATION, statusDisplayType: 'warning'},
				]}
			/>
		);

		expect(screen.getByText('Queued').closest('.label')).toHaveClass(
			'label-warning'
		);
	});

	it.each([
		['en-US', '27,985 Widgets'],
		['pt-BR', '27.985 Widgets'],
	])(
		'formats the counts with the %s grouping separator',
		(languageId, text) => {
			(
				Liferay.ThemeDisplay.getBCP47LanguageId as jest.Mock
			).mockReturnValueOnce(languageId);

			render(
				<Migrations
					migrationIncomplete={false}
					migrations={[
						{
							...COMPLETED_MIGRATION,
							counts: [{label: 'Widgets', value: 27985}],
						},
					]}
				/>
			);

			expect(screen.getByRole('listitem')).toHaveTextContent(text);
		}
	);

	it('groups the completed migrations in a collapsed panel below the migrations not completed', () => {
		render(
			<Migrations
				migrationIncomplete={true}
				migrations={[
					QUEUED_MIGRATION,
					IN_PROGRESS_MIGRATION,
					COMPLETED_MIGRATION,
					{...COMPLETED_MIGRATION, backgroundTaskId: 4},
				]}
			/>
		);

		const [queued, inProgress, ...completed] = screen.getAllByRole(
			'heading',
			{level: 3}
		);

		expect(queued).toHaveTextContent('Migration 3');
		expect(inProgress).toHaveTextContent('Migration 2');
		expect(completed).toHaveLength(2);

		expect(screen.getByRole('button', {name: 'x-y'})).toHaveAttribute(
			'aria-expanded',
			'false'
		);
		expect(
			within(screen.getByRole('region')).getAllByRole('heading', {
				name: 'Migration 1',
			})
		).toHaveLength(2);
	});

	it.each([
		['en-US', 'completed (1,000)'],
		['pt-BR', 'completed (1.000)'],
	])(
		'shows the number of completed migrations in the panel title with the %s grouping separator',
		(languageId, name) => {
			(Liferay.Language.get as jest.Mock).mockImplementation(
				(key: string) => (key === 'x-y' ? '{0} ({1})' : key)
			);
			(
				Liferay.ThemeDisplay.getBCP47LanguageId as jest.Mock
			).mockReturnValueOnce(languageId);

			render(
				<Migrations
					migrationIncomplete={true}
					migrations={Array.from({length: 1000}, (_, index) => ({
						...COMPLETED_MIGRATION,
						backgroundTaskId: index,
					}))}
				/>
			);

			expect(screen.getByRole('button', {name})).toBeInTheDocument();
		}
	);

	it('expands the completed migrations panel when every migration is completed', () => {
		render(
			<Migrations
				migrationIncomplete={false}
				migrations={[COMPLETED_MIGRATION]}
			/>
		);

		expect(screen.getByRole('button', {name: 'x-y'})).toHaveAttribute(
			'aria-expanded',
			'true'
		);
	});

	it('keeps the completed migrations panel expanded by the user after the page reloads', async () => {
		const user = userEvent.setup({
			advanceTimers: jest.advanceTimersByTime,
		});

		const migrations = [IN_PROGRESS_MIGRATION, COMPLETED_MIGRATION];

		const {unmount} = render(
			<Migrations migrationIncomplete={true} migrations={migrations} />
		);

		await user.click(screen.getByRole('button', {name: 'x-y'}));

		unmount();

		render(
			<Migrations migrationIncomplete={true} migrations={migrations} />
		);

		expect(screen.getByRole('button', {name: 'x-y'})).toHaveAttribute(
			'aria-expanded',
			'true'
		);
	});

	it('keeps the completed migrations panel collapsed by the user after the page reloads', async () => {
		const user = userEvent.setup({
			advanceTimers: jest.advanceTimersByTime,
		});

		const migrations = [COMPLETED_MIGRATION];

		const {unmount} = render(
			<Migrations migrationIncomplete={false} migrations={migrations} />
		);

		await user.click(screen.getByRole('button', {name: 'x-y'}));

		unmount();

		render(
			<Migrations migrationIncomplete={false} migrations={migrations} />
		);

		expect(screen.getByRole('button', {name: 'x-y'})).toHaveAttribute(
			'aria-expanded',
			'false'
		);
	});

	it('keeps the errors and warnings panels as the user left them after the page reloads', async () => {
		const user = userEvent.setup({
			advanceTimers: jest.advanceTimersByTime,
		});

		const migrations = [
			{
				...COMPLETED_MIGRATION,
				errors: ['Error 1'],
				warnings: ['Warning 1'],
			},
			{
				...COMPLETED_MIGRATION,
				backgroundTaskId: 4,
				errors: ['Error 2'],
				name: 'Migration 4',
			},
		];

		const {unmount} = render(
			<Migrations migrationIncomplete={false} migrations={migrations} />
		);

		await user.click(
			screen.getAllByRole('button', {name: 'cms-migration-x-errors'})[0]
		);

		unmount();

		render(
			<Migrations migrationIncomplete={false} migrations={migrations} />
		);

		const errorsButtons = screen.getAllByRole('button', {
			name: 'cms-migration-x-errors',
		});

		expect(errorsButtons[0]).toHaveAttribute('aria-expanded', 'true');
		expect(errorsButtons[1]).toHaveAttribute('aria-expanded', 'false');
		expect(
			screen.getByRole('button', {name: 'cms-migration-x-warnings'})
		).toHaveAttribute('aria-expanded', 'false');
	});

	it('does not show the completed migrations panel without completed migrations', () => {
		render(
			<Migrations
				migrationIncomplete={true}
				migrations={[QUEUED_MIGRATION]}
			/>
		);

		expect(
			screen.queryByRole('button', {name: 'x-y'})
		).not.toBeInTheDocument();
	});

	it('saves the logs of a completed migration and links to the folder in a toast', async () => {
		(Liferay.Language.get as jest.Mock).mockImplementation((key: string) =>
			key === 'x-has-been-saved-in-x' ? '{0} has been saved in {1}.' : key
		);

		let respond: (body: string) => void = () => {};

		fetchMock.mockResponseOnce(
			() =>
				new Promise((resolve) => {
					respond = resolve;
				})
		);

		const user = userEvent.setup({
			advanceTimers: jest.advanceTimersByTime,
		});

		render(
			<Migrations
				migrationIncomplete={false}
				migrations={[SAVE_LOGS_MIGRATION]}
			/>
		);

		const button = screen.getByRole('button', {
			name: 'save-logs Migration 1',
		});

		await user.click(button);

		expect(fetchMock).toHaveBeenCalledWith(
			'/save-logs',
			expect.objectContaining({method: 'POST'})
		);
		expect(button).toBeDisabled();
		expect(button.querySelector('.loading-animation')).toBeInTheDocument();

		await act(async () => {
			respond(
				JSON.stringify({
					fileName: 'cms-migration-<b>1</b>-2026-11-24-101530.csv',
					folderName: 'Migration <b>Logs</b>',
					folderURL: '/web/cms/e/view-folder/1/2?name="Logs"',
				})
			);
		});

		expect(
			await screen.findByText(
				'cms-migration-<b>1</b>-2026-11-24-101530.csv has been saved in',
				{exact: false}
			)
		).toBeInTheDocument();
		expect(
			screen.getByRole('link', {name: 'Migration <b>Logs</b>'})
		).toHaveAttribute('href', '/web/cms/e/view-folder/1/2?name="Logs"');
		expect(button).toBeEnabled();
	});

	it('shows the error of the server in a toast when the logs cannot be saved', async () => {
		fetchMock.mockResponseOnce(
			JSON.stringify({error: 'Unable to save the <b>logs</b>.'})
		);

		const user = userEvent.setup({
			advanceTimers: jest.advanceTimersByTime,
		});

		render(
			<Migrations
				migrationIncomplete={false}
				migrations={[SAVE_LOGS_MIGRATION]}
			/>
		);

		const button = screen.getByRole('button', {
			name: 'save-logs Migration 1',
		});

		await user.click(button);

		expect(
			(
				await screen.findByText('Unable to save the <b>logs</b>.')
			).closest('.alert-danger')
		).toBeInTheDocument();
		expect(
			screen.queryByText('an-unexpected-error-occurred')
		).not.toBeInTheDocument();

		await waitFor(() => expect(button).toBeEnabled());
	});

	it('shows an error toast when the request to save the logs fails', async () => {
		fetchMock.mockRejectOnce(new Error());

		const user = userEvent.setup({
			advanceTimers: jest.advanceTimersByTime,
		});

		render(
			<Migrations
				migrationIncomplete={false}
				migrations={[SAVE_LOGS_MIGRATION]}
			/>
		);

		const button = screen.getByRole('button', {
			name: 'save-logs Migration 1',
		});

		await user.click(button);

		expect(
			(await screen.findByText('an-unexpected-error-occurred')).closest(
				'.alert-danger'
			)
		).toBeInTheDocument();

		await waitFor(() => expect(button).toBeEnabled());
	});

	it('does not show the save logs button without the URL to save them', () => {
		render(
			<Migrations
				migrationIncomplete={true}
				migrations={[COMPLETED_MIGRATION, IN_PROGRESS_MIGRATION]}
			/>
		);

		expect(
			screen.queryByRole('button', {name: /save-logs/})
		).not.toBeInTheDocument();
	});
});
