import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { viewFor } from './registry';
import { ArrayView } from './ArrayView';

describe('viewFor', () => {
  it('returns the ARRAY renderer', () => {
    expect(viewFor('ARRAY')).toBe(ArrayView);
  });

  it('falls back for an unknown kind without crashing', () => {
    const Renderer = viewFor('TREE');
    render(<Renderer view={{ kind: 'TREE', nodes: [] }} />);
    expect(screen.getByText(/TREE/)).toBeInTheDocument();
  });
});
