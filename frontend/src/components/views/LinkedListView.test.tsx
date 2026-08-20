import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import fixture from '../../fixtures/linked-list-insert.json';
import type { LinkedListViewPayload, Trace } from '../../types';
import { LinkedListView } from './LinkedListView';

const trace = fixture as unknown as Trace;

describe('LinkedListView', () => {
  it('draws the nodes in the order the pointers say', () => {
    const view = trace.steps[trace.steps.length - 1].view as LinkedListViewPayload;
    render(<LinkedListView view={view} />);
    const rendered = screen.getAllByTestId(/^list-node-/).map((node) => node.textContent);
    expect(rendered).toEqual(view.nodes.map((node) => String(node.value)));
  });

  it('marks the node under the cursor', () => {
    const view = trace.steps[0].view as LinkedListViewPayload;
    render(<LinkedListView view={view} />);
    expect(screen.getByTestId(`list-node-${view.cursor}`)).toHaveAttribute('data-cursor', 'true');
  });

  it('marks the rewired link on the last step', () => {
    const view = trace.steps[trace.steps.length - 1].view as LinkedListViewPayload;
    render(<LinkedListView view={view} />);
    expect(screen.getAllByTestId(/^link-/)).not.toHaveLength(0);
    for (const link of view.changedLinks) {
      expect(screen.getByTestId(`link-${link.from}`)).toHaveAttribute('data-changed', 'true');
    }
  });
});

it('draws the created node detached, before anything points at it', () => {
  const view = trace.steps[2].view as LinkedListViewPayload;
  render(<LinkedListView view={view} />);

  const detached = screen.getByTestId('detached-nodes');
  expect(detached).toContainElement(screen.getByTestId(`list-node-${view.newNode}`));
  expect(screen.getByTestId(`list-node-${view.newNode}`)).toHaveAttribute('data-new', 'true');
});

it('joins the new node into the chain on the step that links it', () => {
  const view = trace.steps[trace.steps.length - 1].view as LinkedListViewPayload;
  render(<LinkedListView view={view} />);
  expect(screen.queryByTestId('detached-nodes')).not.toBeInTheDocument();
});

it('draws no arrow after the last node', () => {
  const view = trace.steps[trace.steps.length - 1].view as LinkedListViewPayload;
  const tail = view.nodes.find((node) => node.next == null);
  render(<LinkedListView view={view} />);
  expect(screen.queryByTestId(`link-${tail!.id}`)).not.toBeInTheDocument();
});
