import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { DemoScreen } from './DemoScreen';
import binarySearchTrace from '../fixtures/binary-search.json';

const demos = [
  {
    id: 'binary-search',
    title: 'Binary Search',
    category: 'ALGORITHMS',
    description: 'desc',
    parameters: [
      { name: 'array', type: 'INT_ARRAY', label: 'Sorted array', required: true, maxLength: 512, defaultValue: [2, 5, 8, 12, 20, 33] },
      { name: 'target', type: 'INT', label: 'Target value', required: true, min: -1000, max: 1000, defaultValue: 20 },
    ],
    sourceCode: binarySearchTrace.sourceCode,
  },
];

let failNext = false;

const server = setupServer(
  http.get('/api/demos', () => HttpResponse.json(demos)),
  http.post('/api/demos/binary-search/trace', () => {
    if (failNext) {
      failNext = false;
      return HttpResponse.json(
        { error: 'INVALID_INPUT', message: 'array must be sorted ascending', field: 'array' },
        { status: 400 },
      );
    }
    return HttpResponse.json(binarySearchTrace);
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  failNext = false;
});
afterAll(() => server.close());

function renderScreen() {
  return render(
    <MemoryRouter initialEntries={['/demo/binary-search']}>
      <Routes>
        <Route path="/demo/:id" element={<DemoScreen />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('DemoScreen', () => {
  it('loads the demo and shows the first step', async () => {
    renderScreen();

    expect(await screen.findByText('Binary Search')).toBeInTheDocument();
    expect(await screen.findByText(`step 1 / ${binarySearchTrace.steps.length}`)).toBeInTheDocument();
    expect(screen.getAllByTestId('cell')).toHaveLength(binarySearchTrace.steps[0].view.items.length);
  });

  it('advances the highlighted line when stepping forward', async () => {
    renderScreen();
    const user = userEvent.setup();

    await screen.findByText(`step 1 / ${binarySearchTrace.steps.length}`);
    await user.click(screen.getByLabelText('next step'));

    await waitFor(() => expect(screen.getByText('step 2 / ' + binarySearchTrace.steps.length)).toBeInTheDocument());
    const active = screen.getAllByTestId('code-line').filter((node) => node.dataset.active === 'true');
    expect(active).toHaveLength(1);
  });

  it('shows the step message and variables', async () => {
    renderScreen();
    await screen.findByText(binarySearchTrace.steps[0].message);
    expect(screen.getAllByTestId('var-row').length).toBeGreaterThan(0);
  });

  it('keeps the previous trace and shows the server message when a run fails', async () => {
    renderScreen();
    const user = userEvent.setup();
    await screen.findByText(`step 1 / ${binarySearchTrace.steps.length}`);

    failNext = true;
    await user.click(screen.getByRole('button', { name: 'Run' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('array must be sorted ascending');
    expect(screen.getByText(`step 1 / ${binarySearchTrace.steps.length}`)).toBeInTheDocument();
  });
});
