import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import fixture from '../../fixtures/hash-map-put.json';
import setFixture from '../../fixtures/hash-set-add.json';
import type { HashTableViewPayload, Trace } from '../../types';
import { HashTableView } from './HashTableView';

const trace = fixture as unknown as Trace;

describe('HashTableView', () => {
  it('draws every bucket, including the empty ones', () => {
    const view = trace.steps[0].view as HashTableViewPayload;
    render(<HashTableView view={view} />);
    expect(screen.getAllByTestId(/^bucket-/)).toHaveLength(view.capacity);
  });

  it('marks the bucket the key hashed to', () => {
    const view = trace.steps[0].view as HashTableViewPayload;
    render(<HashTableView view={view} />);
    expect(screen.getByTestId(`bucket-${view.activeBucket}`)).toHaveAttribute('data-active', 'true');
  });

  it('marks the inserted entry on the last step', () => {
    const view = trace.steps[trace.steps.length - 1].view as HashTableViewPayload;
    render(<HashTableView view={view} />);
    expect(screen.getByText('13')).toHaveAttribute('data-state', 'INSERTED');
  });
});

it('prints keys alone when entries carry no value, as a set sends them', () => {
  const setLike: HashTableViewPayload = {
    kind: 'HASH_TABLE',
    capacity: 2,
    buckets: [
      { index: 0, entries: [] },
      { index: 1, entries: [{ key: 7, state: 'NORMAL' }] },
    ],
    activeBucket: 1,
  };
  render(<HashTableView view={setLike} />);
  expect(screen.getByTestId('bucket-1').textContent).toBe('17');
  expect(screen.getByTestId('bucket-1').textContent).not.toContain('undefined');
});

describe('HashTableView, drawing a set', () => {
  const setTrace = setFixture as unknown as Trace;

  it('prints keys with no value and no stray undefined', () => {
    const view = setTrace.steps[setTrace.steps.length - 1].view as HashTableViewPayload;
    render(<HashTableView view={view} />);

    const bucket = screen.getByTestId(`bucket-${view.activeBucket}`);
    expect(bucket.textContent).not.toContain('undefined');
    expect(bucket.textContent).not.toContain(':');
  });

  it('marks the entry the duplicate matched', () => {
    const view = setTrace.steps[setTrace.steps.length - 1].view as HashTableViewPayload;
    const matched = view.buckets.flatMap((b) => b.entries).find((e) => e.state === 'MATCHED');
    expect(matched, 'the set fixture should end on a duplicate').toBeDefined();
    render(<HashTableView view={view} />);
    expect(screen.getByText(String(matched!.key))).toHaveAttribute('data-state', 'MATCHED');
  });
});
