import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { I18nProvider } from '../i18n/I18nContext';
import { Catalog } from './Catalog';

const server = setupServer(
  http.get('/api/demos', () =>
    HttpResponse.json([
      {
        id: 'binary-search',
        titleKey: 'demo.binarySearch.title',
        category: 'ALGORITHMS',
        descriptionKey: 'demo.binarySearch.description',
        parameters: [],
        sourceCode: '',
      },
      {
        id: 'bubble-sort',
        titleKey: 'demo.bubbleSort.title',
        category: 'ALGORITHMS',
        descriptionKey: 'demo.bubbleSort.description',
        parameters: [],
        sourceCode: '',
      },
    ]),
  ),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Catalog', () => {
  it('lists demos as links grouped by category', async () => {
    render(
      <I18nProvider>
        <MemoryRouter>
          <Catalog />
        </MemoryRouter>
      </I18nProvider>,
    );

    expect(await screen.findByRole('heading', { name: 'Algorithms' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Binary Search/ })).toHaveAttribute('href', '/demo/binary-search');
    expect(screen.getByRole('link', { name: /Bubble Sort/ })).toHaveAttribute('href', '/demo/bubble-sort');
  });
});
