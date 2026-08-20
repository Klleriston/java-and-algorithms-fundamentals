import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { Catalog } from './Catalog';

const server = setupServer(
  http.get('/api/demos', () =>
    HttpResponse.json([
      { id: 'binary-search', title: 'Binary Search', category: 'ALGORITHMS', description: 'Halve the range', parameters: [], sourceCode: '' },
      { id: 'bubble-sort', title: 'Bubble Sort', category: 'ALGORITHMS', description: 'Swap neighbours', parameters: [], sourceCode: '' },
    ]),
  ),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Catalog', () => {
  it('lists demos as links grouped by category', async () => {
    render(
      <MemoryRouter>
        <Catalog />
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'ALGORITHMS' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Binary Search/ })).toHaveAttribute('href', '/demo/binary-search');
    expect(screen.getByRole('link', { name: /Bubble Sort/ })).toHaveAttribute('href', '/demo/bubble-sort');
  });
});
