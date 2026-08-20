import type { LinkedListViewPayload } from '../../types';
import './LinkedListView.css';

/** Nodes reachable from the head, in order. Anything else was created but not linked yet. */
function chainOf(view: LinkedListViewPayload): Set<number> {
  const byId = new Map(view.nodes.map((node) => [node.id, node]));
  const reachable = new Set<number>();
  let current: LinkedListViewPayload['nodes'][number] | undefined = view.nodes[0];
  while (current !== undefined && !reachable.has(current.id)) {
    reachable.add(current.id);
    current = current.next == null ? undefined : byId.get(current.next);
  }
  return reachable;
}

function Cell({ node, view, changed }: {
  node: LinkedListViewPayload['nodes'][number];
  view: LinkedListViewPayload;
  changed: Set<number | null | undefined>;
}) {
  return (
    <div className="linked-list-cell">
      <span
        className="linked-list-node"
        data-testid={`list-node-${node.id}`}
        data-cursor={node.id === view.cursor}
        data-new={node.id === view.newNode}
      >
        {node.value}
      </span>
      {node.next != null && (
        <span
          className="linked-list-link"
          data-testid={`link-${node.id}`}
          data-changed={changed.has(node.id)}
          aria-hidden="true"
        >
          →
        </span>
      )}
    </div>
  );
}

export function LinkedListView({ view }: { view: LinkedListViewPayload }) {
  const changed = new Set(view.changedLinks.map((link) => link.from));
  const chain = chainOf(view);
  const detached = view.nodes.filter((node) => !chain.has(node.id));

  return (
    <div className="linked-list-board">
      <div className="linked-list">
        {view.nodes.filter((node) => chain.has(node.id)).map((node) => (
          <Cell key={node.id} node={node} view={view} changed={changed} />
        ))}
      </div>
      {detached.length > 0 && (
        <div className="linked-list linked-list--detached" data-testid="detached-nodes">
          {detached.map((node) => (
            <Cell key={node.id} node={node} view={view} changed={changed} />
          ))}
        </div>
      )}
    </div>
  );
}
