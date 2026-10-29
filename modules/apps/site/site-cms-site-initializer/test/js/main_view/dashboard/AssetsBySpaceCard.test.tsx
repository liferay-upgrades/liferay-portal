/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';

// eslint-disable-next-line @liferay/portal/no-cross-module-deep-import
import {checkAccessibility} from '@liferay/layout-js-components-web/test/__lib__/index';
import {
	act,
	render,
	screen,
	waitFor,
	waitForElementToBeRemoved,
	within,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import SpaceService from '../../../../src/main/resources/META-INF/resources/js/common/services/SpaceService';
import {Space} from '../../../../src/main/resources/META-INF/resources/js/common/types/Space';
import {
	SpaceOption,
	initialSpace,
} from '../../../../src/main/resources/META-INF/resources/js/main_view/dashboard/common/SpacePicker';
import GovernanceService, {
	DEFAULT_CMS_FILTER,
	FacetBucket,
} from '../../../../src/main/resources/META-INF/resources/js/main_view/dashboard/governance/GovernanceService';
import {
	InventoryContext,
	InventoryContextProvider,
	State,
	initialLanguage,
} from '../../../../src/main/resources/META-INF/resources/js/main_view/dashboard/inventory/InventoryContext';
import {
	AssetsBySpaceCard,
	SpaceLibrary,
	getRows,
} from '../../../../src/main/resources/META-INF/resources/js/main_view/dashboard/inventory/components/AssetsBySpaceCard';

jest.mock('frontend-js-web', () => ({
	...(jest.requireActual('frontend-js-web') as object),
	sub: (key: string, ...values: unknown[]) =>
		[key, ...values.flat()].join(' '),
}));

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/common/services/SpaceService'
);

jest.mock(
	'../../../../src/main/resources/META-INF/resources/js/main_view/dashboard/governance/GovernanceService'
);

const mockedGovernanceService = GovernanceService as jest.Mocked<
	typeof GovernanceService
>;

const mockedSpaceService = SpaceService as jest.Mocked<typeof SpaceService>;

function createSpace(
	overrides: Partial<Space> & Pick<Space, 'id' | 'siteId'>
): Space {
	return {
		assetLibraryKey: `space-${overrides.id}`,
		creatorUserId: '1',
		description: '',
		externalReferenceCode: `ERC_${overrides.id}`,
		friendlyURL: `/space-${overrides.id}`,
		name: `Space ${overrides.id}`,
		...overrides,
	};
}

const SPACES: Space[] = [
	createSpace({id: 1, name: 'Marketing', siteId: 101}),
	createSpace({id: 2, name: 'Engineering', siteId: 102}),
	createSpace({id: 3, name: 'Legal', siteId: 103}),
];

const BUCKETS: FacetBucket[] = [
	{frequency: 10, term: '102'},
	{frequency: 30, term: '101'},
	{frequency: 10, term: '103'},
	{frequency: 5, term: '999'},
];

const LOADING_TEST_ID = 'loading';

type Filters = {language: SpaceOption; space: SpaceOption};

const DEFAULT_FILTERS: Filters = {
	language: initialLanguage,
	space: initialSpace,
};

function createDeferred<T>() {
	let resolve!: (value: T) => void;

	const promise = new Promise<T>((resolvePromise) => {
		resolve = resolvePromise;
	});

	return {promise, resolve};
}

/**
 * Renders the card through the real provider, mirroring production usage.
 */
function renderComponent(value: Partial<State> = {}) {
	return render(
		<InventoryContextProvider value={value}>
			<AssetsBySpaceCard />
		</InventoryContextProvider>
	);
}

/**
 * Renders the card with a raw context value so that filter changes can be
 * driven through `rerender` and the change callbacks can be asserted.
 */
function renderWithContext(
	filters: Filters = DEFAULT_FILTERS,
	overrides: Partial<State> = {}
) {
	const state: State = {
		changeLanguage: jest.fn(),
		changeSpace: jest.fn(),
		constants: {},
		filters,
		...overrides,
	};

	const ui = (nextState: State) => (
		<InventoryContext.Provider value={nextState}>
			<AssetsBySpaceCard />
		</InventoryContext.Provider>
	);

	const result = render(ui(state));

	return {
		...result,
		rerenderWithFilters: (nextFilters: Filters) =>
			result.rerender(ui({...state, filters: nextFilters})),
		state,
	};
}

async function waitForLoaded() {
	await waitForElementToBeRemoved(() => screen.getByTestId(LOADING_TEST_ID));
}

describe('[CMS Dashboard] AssetsBySpaceCard', () => {
	describe('getRows', () => {
		it('maps buckets to known spaces ordered by count and computes share and bar width relative to the total', () => {
			expect(getRows(BUCKETS, SPACES)).toEqual([
				{
					count: 30,
					groupId: '101',
					name: 'Marketing',
					relativeWidth: 60,
					share: 0.6,
				},
				{
					count: 10,
					groupId: '102',
					name: 'Engineering',
					relativeWidth: 20,
					share: 0.2,
				},
				{
					count: 10,
					groupId: '103',
					name: 'Legal',
					relativeWidth: 20,
					share: 0.2,
				},
			]);
		});

		it('returns no rows when there are no buckets', () => {
			expect(getRows([], SPACES)).toEqual([]);
		});

		it('returns no rows when spaces are omitted', () => {
			expect(getRows(BUCKETS)).toEqual([]);
		});

		it.each([
			['zero', 0],
			['negative', -3],
		])('ignores buckets with a %s frequency', (_, frequency) => {
			expect(
				getRows(
					[
						{frequency, term: '101'},
						{frequency: 4, term: '102'},
					],
					SPACES
				)
			).toEqual([
				{
					count: 4,
					groupId: '102',
					name: 'Engineering',
					relativeWidth: 100,
					share: 1,
				},
			]);
		});

		it('ignores buckets that do not belong to an accessible space and excludes them from the share', () => {
			const rows = getRows(
				[
					{frequency: 5, term: '101'},
					{frequency: 95, term: '999'},
				],
				SPACES
			);

			expect(rows).toHaveLength(1);
			expect(rows[0]).toMatchObject({relativeWidth: 100, share: 1});
		});

		it('falls back to groupId when a space has no siteId', () => {
			const spaces: SpaceLibrary[] = [{groupId: 201, name: 'Archive'}];

			expect(getRows([{frequency: 2, term: '201'}], spaces)).toEqual([
				{
					count: 2,
					groupId: '201',
					name: 'Archive',
					relativeWidth: 100,
					share: 1,
				},
			]);
		});

		it('does not mutate the given buckets', () => {
			const buckets = [...BUCKETS];

			getRows(buckets, SPACES);

			expect(buckets).toEqual(BUCKETS);
		});
	});

	describe('component', () => {
		beforeEach(() => {
			mockedSpaceService.getSpaces.mockResolvedValue(SPACES);

			mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
				data: BUCKETS,
				error: null,
			});
		});

		afterEach(() => {
			jest.clearAllMocks();
			jest.restoreAllMocks();
		});

		describe('rendering', () => {
			it('shows the loading indicator while data is being fetched', async () => {
				renderComponent();

				expect(screen.getByTestId(LOADING_TEST_ID)).toBeInTheDocument();
				expect(screen.queryByRole('list')).not.toBeInTheDocument();

				await waitForLoaded();
			});

			it('renders the total asset count and rows ordered by number of assets', async () => {
				const {container} = renderComponent();

				await waitForLoaded();

				expect(
					screen.getByRole('heading', {name: 'assets-by-space'})
				).toBeInTheDocument();

				expect(
					screen.getByText(
						'this-is-how-the-assets-are-distributed-across-the-spaces'
					)
				).toBeInTheDocument();

				expect(screen.getByText('50')).toBeInTheDocument();
				expect(
					screen.getByText('assets-in-x-spaces 3')
				).toBeInTheDocument();

				const items = screen.getAllByRole('listitem');

				expect(items).toHaveLength(3);

				[
					['M', 'Marketing', '30', '60%'],
					['E', 'Engineering', '10', '20%'],
					['L', 'Legal', '10', '20%'],
				].forEach((texts, index) => {
					texts.forEach((text) => {
						expect(
							within(items[index]).getByText(text)
						).toBeInTheDocument();
					});
				});

				const bars = container.querySelectorAll(
					'.cms-dashboard__assets-by-space__bar-fill'
				);

				expect(bars).toHaveLength(3);
				expect(bars[0]).toHaveStyle({width: '60%'});
				expect(bars[1]).toHaveStyle({width: '20%'});
				expect(bars[2]).toHaveStyle({width: '20%'});
			});

			it('hides the decorative bars from assistive technologies', async () => {
				const {container} = renderComponent();

				await waitForLoaded();

				container
					.querySelectorAll('.cms-dashboard__assets-by-space__bar')
					.forEach((bar) => {
						expect(bar).toHaveAttribute('aria-hidden', 'true');
					});
			});

			it('formats large counts using the current locale', async () => {
				mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
					data: [
						{frequency: 12000, term: '101'},
						{frequency: 3456, term: '102'},
					],
					error: null,
				});

				renderComponent();

				await waitForLoaded();

				const numberFormat = new Intl.NumberFormat(
					Liferay.ThemeDisplay.getBCP47LanguageId()
				);

				expect(
					screen.getByText(numberFormat.format(15456))
				).toBeInTheDocument();

				const [first, second] = screen.getAllByRole('listitem');

				expect(
					within(first).getByText(numberFormat.format(12000))
				).toBeInTheDocument();
				expect(
					within(second).getByText(numberFormat.format(3456))
				).toBeInTheDocument();
			});
		});

		describe('space scoping', () => {
			it('requests all spaces when no space is selected', async () => {
				renderComponent();

				await waitForLoaded();

				expect(
					mockedGovernanceService.getAssetsBySpace
				).toHaveBeenCalledWith(
					DEFAULT_CMS_FILTER,
					undefined,
					expect.any(AbortSignal)
				);
			});

			it('uses the singular space label when a single space is selected', async () => {
				mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
					data: [{frequency: 30, term: '101'}],
					error: null,
				});

				renderComponent({
					filters: {
						language: initialLanguage,
						space: {
							externalReferenceCode: 'ERC_1',
							label: 'Marketing',
							value: '1',
						},
					},
				});

				await waitForLoaded();

				expect(
					screen.getByText('assets-in-x-space 1')
				).toBeInTheDocument();
				expect(screen.getByText('100%')).toBeInTheDocument();
			});

			it.each<[string, SpaceOption, number | undefined]>([
				[
					'the siteId of the option',
					{label: 'Anything', siteId: 777, value: '1'},
					777,
				],
				[
					'the matching external reference code',
					{
						externalReferenceCode: 'ERC_1',
						label: 'Renamed',
						value: 'x',
					},
					101,
				],
				['the matching space id', {label: 'Renamed', value: '2'}, 102],
				['the matching space name', {label: 'Legal', value: 'x'}, 103],
				[
					'the numeric option value when no space matches',
					{label: 'Unknown', value: '555'},
					555,
				],
				[
					'no group when the option value is not numeric and no space matches',
					{label: 'Unknown', value: 'abc'},
					undefined,
				],
			])(
				'scopes the request to %s',
				async (_, space, expectedGroupId) => {
					renderWithContext({language: initialLanguage, space});

					await waitForLoaded();

					expect(
						mockedGovernanceService.getAssetsBySpace
					).toHaveBeenCalledWith(
						DEFAULT_CMS_FILTER,
						expectedGroupId,
						expect.any(AbortSignal)
					);
				}
			);

			it('scopes the request by groupId when the matched space has no siteId', async () => {
				mockedSpaceService.getSpaces.mockResolvedValue([
					{
						...createSpace({id: 9, name: 'Archive', siteId: 0}),
						groupId: 909,
						siteId: undefined,
					} as unknown as Space,
				]);

				renderWithContext({
					language: initialLanguage,
					space: {
						externalReferenceCode: 'ERC_9',
						label: 'Archive',
						value: '9',
					},
				});

				await waitForLoaded();

				expect(
					mockedGovernanceService.getAssetsBySpace
				).toHaveBeenCalledWith(
					DEFAULT_CMS_FILTER,
					909,
					expect.any(AbortSignal)
				);
			});
		});

		describe('empty states', () => {
			it('renders the empty state when there are no assets', async () => {
				mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
					data: [],
					error: null,
				});

				renderComponent();

				await waitForLoaded();

				expect(screen.getByText('no-assets-yet')).toBeInTheDocument();
				expect(
					screen.getByText(
						'the-spaces-you-can-access-do-not-have-any-assets'
					)
				).toBeInTheDocument();
				expect(
					screen.queryByRole('button', {name: 'clear-filters'})
				).not.toBeInTheDocument();
				expect(screen.queryByRole('list')).not.toBeInTheDocument();
			});

			it('renders the empty state when assets only belong to inaccessible spaces', async () => {
				mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
					data: [{frequency: 42, term: '999'}],
					error: null,
				});

				renderComponent();

				await waitForLoaded();

				expect(screen.getByText('no-assets-yet')).toBeInTheDocument();
			});

			it.each<[string, Filters]>([
				[
					'a language',
					{
						language: {label: 'English', value: 'en_US'},
						space: initialSpace,
					},
				],
				[
					'a space',
					{
						language: initialLanguage,
						space: {label: 'Marketing', value: '1'},
					},
				],
			])(
				'renders the no results state when %s filter is applied',
				async (_, filters) => {
					mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
						data: [],
						error: null,
					});

					renderWithContext(filters);

					await waitForLoaded();

					expect(
						screen.getByText('no-results-found')
					).toBeInTheDocument();
					expect(
						screen.getByText('sorry,-no-results-were-found')
					).toBeInTheDocument();
					expect(
						screen.queryByText('no-assets-yet')
					).not.toBeInTheDocument();
				}
			);

			it('resets both filters when clicking clear filters', async () => {
				mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
					data: [],
					error: null,
				});

				const {state} = renderWithContext({
					language: {label: 'English', value: 'en_US'},
					space: {label: 'Marketing', value: '1'},
				});

				await waitForLoaded();

				await userEvent.click(
					screen.getByRole('button', {name: 'clear-filters'})
				);

				expect(state.changeLanguage).toHaveBeenCalledTimes(1);
				expect(state.changeLanguage).toHaveBeenCalledWith({
					label: 'all-languages',
					value: 'all',
				});

				expect(state.changeSpace).toHaveBeenCalledTimes(1);
				expect(state.changeSpace).toHaveBeenCalledWith({
					label: 'all-spaces',
					value: 'all',
				});
			});
		});

		describe('error states', () => {
			it('logs the error and renders the empty state when the request fails', async () => {
				const consoleErrorSpy = jest
					.spyOn(console, 'error')
					.mockImplementation(() => {});

				mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
					data: null,
					error: 'Something went wrong',
				});

				renderComponent();

				await waitForLoaded();

				expect(consoleErrorSpy).toHaveBeenCalledWith(
					'Something went wrong'
				);
				expect(screen.getByText('no-assets-yet')).toBeInTheDocument();
			});

			it('treats a missing spaces response as no accessible spaces', async () => {
				mockedSpaceService.getSpaces.mockResolvedValue(
					undefined as unknown as Space[]
				);

				renderComponent();

				await waitForLoaded();

				expect(
					mockedGovernanceService.getAssetsBySpace
				).toHaveBeenCalledWith(
					DEFAULT_CMS_FILTER,
					undefined,
					expect.any(AbortSignal)
				);
				expect(screen.getByText('no-assets-yet')).toBeInTheDocument();
			});
		});

		describe('request lifecycle', () => {
			it('does not refetch when re-rendered with the same filters', async () => {
				const {rerenderWithFilters} = renderWithContext();

				await waitForLoaded();

				rerenderWithFilters(DEFAULT_FILTERS);

				expect(mockedSpaceService.getSpaces).toHaveBeenCalledTimes(1);
				expect(
					mockedGovernanceService.getAssetsBySpace
				).toHaveBeenCalledTimes(1);
			});

			it('aborts the in-flight request and ignores its stale response when filters change', async () => {
				const staleRequest =
					createDeferred<
						Awaited<
							ReturnType<
								typeof GovernanceService.getAssetsBySpace
							>
						>
					>();

				mockedGovernanceService.getAssetsBySpace
					.mockReturnValueOnce(staleRequest.promise)
					.mockResolvedValueOnce({
						data: [{frequency: 7, term: '102'}],
						error: null,
					});

				const {rerenderWithFilters} = renderWithContext();

				await waitFor(() =>
					expect(
						mockedGovernanceService.getAssetsBySpace
					).toHaveBeenCalledTimes(1)
				);

				const [, , staleSignal] =
					mockedGovernanceService.getAssetsBySpace.mock.calls[0];

				rerenderWithFilters({
					language: initialLanguage,
					space: {label: 'Engineering', value: '2'},
				});

				expect(staleSignal?.aborted).toBe(true);

				await waitForLoaded();

				await act(async () => {
					staleRequest.resolve({data: BUCKETS, error: null});
				});

				expect(
					mockedGovernanceService.getAssetsBySpace
				).toHaveBeenLastCalledWith(
					DEFAULT_CMS_FILTER,
					102,
					expect.any(AbortSignal)
				);

				const items = screen.getAllByRole('listitem');

				expect(items).toHaveLength(1);
				expect(
					within(items[0]).getByText('Engineering')
				).toBeInTheDocument();
				expect(screen.queryByText('Marketing')).not.toBeInTheDocument();
			});

			it('skips the assets request when unmounted while spaces are loading', async () => {
				const spacesRequest = createDeferred<Space[]>();

				mockedSpaceService.getSpaces.mockReturnValueOnce(
					spacesRequest.promise
				);

				const {unmount} = renderComponent();

				unmount();

				await act(async () => {
					spacesRequest.resolve(SPACES);
				});

				expect(
					mockedGovernanceService.getAssetsBySpace
				).not.toHaveBeenCalled();
			});

			it('aborts the assets request when unmounted', async () => {
				const assetsRequest =
					createDeferred<
						Awaited<
							ReturnType<
								typeof GovernanceService.getAssetsBySpace
							>
						>
					>();

				mockedGovernanceService.getAssetsBySpace.mockReturnValueOnce(
					assetsRequest.promise
				);

				const consoleErrorSpy = jest.spyOn(console, 'error');

				const {unmount} = renderComponent();

				await waitFor(() =>
					expect(
						mockedGovernanceService.getAssetsBySpace
					).toHaveBeenCalled()
				);

				const [, , signal] =
					mockedGovernanceService.getAssetsBySpace.mock.calls[0];

				unmount();

				expect(signal?.aborted).toBe(true);

				await act(async () => {
					assetsRequest.resolve({data: null, error: 'aborted'});
				});

				expect(consoleErrorSpy).not.toHaveBeenCalled();
			});
		});

		describe('accessibility', () => {
			it('has no accessibility violations with data', async () => {
				const {container} = renderComponent();

				await waitForLoaded();

				await checkAccessibility({
					bestPractices: true,
					context: container,
				});
			});

			it('has no accessibility violations in the no results state', async () => {
				mockedGovernanceService.getAssetsBySpace.mockResolvedValue({
					data: [],
					error: null,
				});

				const {container} = renderWithContext({
					language: {label: 'English', value: 'en_US'},
					space: initialSpace,
				});

				await waitForLoaded();

				await checkAccessibility({
					bestPractices: true,
					context: container,
				});
			});
		});
	});
});
