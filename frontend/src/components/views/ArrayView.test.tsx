import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ArrayView } from './ArrayView';
import type { ArrayViewPayload } from '../../types';

const view: ArrayViewPayload = {
  kind: 'ARRAY',
  items: [2, 5, 8, 12],
  pointers: { low: 2, mid: 1, high: 3 },
  ranges: [{ from: 0, to: 1, state: 'DISCARDED' }],
  swapped: [2, 3],
};

describe('ArrayView', () => {
  it('renders one cell per item', () => {
    render(<ArrayView view={view} />);
    expect(screen.getAllByTestId('cell').map((node) => node.textContent)).toEqual(['2', '5', '8', '12']);
  });

  it('marks cells inside a range with its state', () => {
    render(<ArrayView view={view} />);
    expect(screen.getAllByTestId('cell').map((node) => node.dataset.state)).toEqual([
      'DISCARDED',
      'DISCARDED',
      'default',
      'default',
    ]);
  });

  it('marks swapped cells', () => {
    render(<ArrayView view={view} />);
    expect(screen.getAllByTestId('cell').map((node) => node.dataset.swapped)).toEqual([
      'false',
      'false',
      'true',
      'true',
    ]);
  });

  it('renders a pointer label for each pointer', () => {
    render(<ArrayView view={view} />);
    expect(screen.getByText('low')).toBeInTheDocument();
    expect(screen.getByText('mid')).toBeInTheDocument();
    expect(screen.getByText('high')).toBeInTheDocument();
  });

  it('renders an empty array without crashing', () => {
    render(<ArrayView view={{ kind: 'ARRAY', items: [], pointers: {}, ranges: [], swapped: [] }} />);
    expect(screen.queryAllByTestId('cell')).toHaveLength(0);
  });
});
