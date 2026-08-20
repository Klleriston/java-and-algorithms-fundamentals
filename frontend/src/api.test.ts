import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { ApiRequestError, fetchDemos, runTrace } from './api';
import binarySearchTrace from './fixtures/binary-search.json';

const server = setupServer(
  http.get('/api/demos', () =>
    HttpResponse.json([
      {
        id: 'binary-search',
        titleKey: 'demo.binarySearch.title',
        category: 'ALGORITHMS',
        descriptionKey: 'demo.binarySearch.description',
        parameters: [
          {
            name: 'target',
            type: 'INT',
            labelKey: 'demo.binarySearch.param.target',
            required: true,
            min: -1000,
            max: 1000,
            defaultValue: 20,
          },
        ],
        sourceCode: 'code',
      },
    ]),
  ),
  http.post('/api/demos/binary-search/trace', () => HttpResponse.json(binarySearchTrace)),
  http.post('/api/demos/bubble-sort/trace', () =>
    HttpResponse.json(
      { error: 'INVALID_INPUT', messageKey: 'error.arrayNotSorted', messageArgs: {}, field: 'array' },
      { status: 400 },
    ),
  ),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('api', () => {
  it('lists demos', async () => {
    const demos = await fetchDemos();
    expect(demos).toHaveLength(1);
    expect(demos[0].parameters[0].type).toBe('INT');
  });

  it('runs a trace', async () => {
    const trace = await runTrace('binary-search', { target: 20 });
    expect(trace.demoId).toBe('binary-search');
    expect(trace.steps[0].view.kind).toBe('ARRAY');
  });

  it('throws ApiRequestError carrying the message key, its args and the field', async () => {
    await expect(runTrace('bubble-sort', {})).rejects.toMatchObject({
      name: 'ApiRequestError',
      messageKey: 'error.arrayNotSorted',
      messageArgs: {},
      field: 'array',
    });
    await expect(runTrace('bubble-sort', {})).rejects.toBeInstanceOf(ApiRequestError);
  });
});
