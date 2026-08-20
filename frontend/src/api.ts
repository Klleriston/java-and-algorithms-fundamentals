import type { DemoSummary, Trace } from './types';

export class ApiRequestError extends Error {
  readonly field?: string;

  constructor(message: string, field?: string) {
    super(message);
    this.name = 'ApiRequestError';
    this.field = field;
  }
}

async function parseOrThrow<T>(response: Response): Promise<T> {
  if (response.ok) {
    return (await response.json()) as T;
  }
  let message = `Request failed with status ${response.status}`;
  let field: string | undefined;
  try {
    const body = await response.json();
    if (typeof body?.message === 'string') message = body.message;
    if (typeof body?.field === 'string') field = body.field;
  } catch {
    // response had no JSON body; keep the status message
  }
  throw new ApiRequestError(message, field);
}

export async function fetchDemos(): Promise<DemoSummary[]> {
  return parseOrThrow<DemoSummary[]>(await fetch('/api/demos'));
}

export async function runTrace(id: string, params: Record<string, unknown>): Promise<Trace> {
  const response = await fetch(`/api/demos/${id}/trace`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  });
  return parseOrThrow<Trace>(response);
}
