/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import TextMetricsPanel from '../../../../../src/main/resources/META-INF/resources/js/content_editor/components/panels/TextMetricsPanel';

function appendForm({content = '', title = ''} = {}) {
	const form = document.createElement('form');

	form.className = 'lfr-main-form-container';
	form.innerHTML =
		'<input aria-label="Title" name="ObjectField_title" />' +
		'<div class="ck-editor__editable"></div>';

	form.querySelector('input')!.value = title;

	(form.querySelector('.ck-editor__editable') as any).ckeditorInstance = {
		getData: () => content,
	};

	document.body.appendChild(form);
}

function getValueElement(label: string) {
	return screen.getByText(label).nextElementSibling;
}

describe('TextMetricsPanel', () => {
	const originalGetBCP47LanguageId =
		global.Liferay.ThemeDisplay.getBCP47LanguageId;

	beforeEach(() => {
		global.Liferay.ThemeDisplay.getBCP47LanguageId = jest
			.fn()
			.mockReturnValue('en-US');
	});

	afterEach(() => {
		document.body.innerHTML = '';

		global.Liferay.ThemeDisplay.getBCP47LanguageId =
			originalGetBCP47LanguageId;
	});

	it('shows the metrics of the title and the rich text', async () => {
		appendForm({
			content:
				'<p>Our autumn campaign brings new discounts.</p>' +
				'<p>Visit the nearest store.</p>',
			title: 'Autumn Campaign',
		});

		render(<TextMetricsPanel />);

		await screen.findByText('words');

		expect(getValueElement('characters')).toHaveTextContent('80');
		expect(getValueElement('characters-without-spaces')).toHaveTextContent(
			'71'
		);
		expect(getValueElement('paragraphs')).toHaveTextContent('2');
		expect(getValueElement('reading-time')).toHaveTextContent('1 min');
		expect(getValueElement('words')).toHaveTextContent('12');
	});

	it('shows the empty state when there is no text', async () => {
		appendForm();

		render(<TextMetricsPanel />);

		expect(
			await screen.findByText('start-writing-to-see-the-text-metrics')
		).toBeInTheDocument();
	});

	it('updates the metrics as the author types', async () => {
		appendForm();

		render(<TextMetricsPanel />);

		await screen.findByText('start-writing-to-see-the-text-metrics');

		await userEvent.type(screen.getByLabelText('Title'), 'Hello world');

		await waitFor(() => {
			expect(getValueElement('words')).toHaveTextContent('2');
		});
	});
});
