import type { LinkedListViewPayload } from '../../types';
import './LinkedListView.css';

export function LinkedListView({ view }: { view: LinkedListViewPayload }) {
  const changed = new Set(view.changedLinks.map((link) => link.from));

  return (
    <div className="linked-list">
      {view.nodes.map((node) => (
        <div key={node.id} className="linked-list-cell">
          <span
            className="linked-list-node"
            data-testid={`list-node-${node.id}`}
            data-cursor={node.id === view.cursor}
            data-new={node.id === view.newNode}
          >
            {node.value}
          </span>
          {node.next !== null && (
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
      ))}
    </div>
  );
}
