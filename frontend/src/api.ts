import type { DemoSummary, Trace } from './types';

export class ApiRequestError extends Error {
  readonly field?: string;
  readonly messageKey: string;
  readonly messageArgs: Record<string, unknown>;

  constructor(messageKey: string, messageArgs: Record<string, unknown> = {}, field?: string) {
    super(messageKey);
    this.name = 'ApiRequestError';
    this.messageKey = messageKey;
    this.messageArgs = messageArgs;
    this.field = field;
  }
}

async function parseOrThrow<T>(response: Response): Promise<T> {
  if (response.ok) {
    return (await response.json()) as T;
  }
  let messageKey = 'error.requestFailed';
  let messageArgs: Record<string, unknown> = { status: response.status };
  let field: string | undefined;
  try {
    const body = await response.json();
    if (typeof body?.messageKey === 'string') {
      messageKey = body.messageKey;
      messageArgs = body.messageArgs ?? {};
    }
    if (typeof body?.field === 'string') field = body.field;
  } catch {
    // response had no JSON body; keep the status key
  }
  throw new ApiRequestError(messageKey, messageArgs, field);
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
