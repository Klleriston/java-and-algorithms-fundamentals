import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import fixture from '../../fixtures/bst-insert.json';
import type { Trace, TreeViewPayload } from '../../types';
import { TreeView } from './TreeView';

const trace = fixture as unknown as Trace;

describe('TreeView', () => {
  it('draws one element per node', () => {
    const view = trace.steps[0].view as TreeViewPayload;
    render(<TreeView view={view} />);
    expect(screen.getAllByTestId(/^node-/)).toHaveLength(view.nodes.length);
  });

  it('marks the nodes on the path taken so far', () => {
    const view = trace.steps[trace.steps.length - 1].view as TreeViewPayload;
    render(<TreeView view={view} />);
    for (const id of view.path) {
      expect(screen.getByTestId(`node-${id}`)).toHaveAttribute('data-on-path', 'true');
    }
  });

  it('marks the inserted node', () => {
    const view = trace.steps[trace.steps.length - 1].view as TreeViewPayload;
    render(<TreeView view={view} />);
    expect(screen.getByTestId(`node-${view.insertedNode}`)).toHaveAttribute('data-inserted', 'true');
  });
});
