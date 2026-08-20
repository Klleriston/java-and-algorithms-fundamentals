import type { TreeViewPayload } from '../../types';
import './TreeView.css';

interface Placed {
  id: number;
  value: number;
  column: number;
  depth: number;
}

function place(view: TreeViewPayload): Placed[] {
  const byId = new Map(view.nodes.map((node) => [node.id, node]));
  const root = view.nodes.find((node) => !view.nodes.some((other) => other.left === node.id || other.right === node.id));
  const placed: Placed[] = [];
  let column = 0;

  const walk = (id: number | null, depth: number) => {
    if (id === null) return;
    const node = byId.get(id);
    if (node === undefined) return;
    walk(node.left, depth + 1);
    placed.push({ id: node.id, value: node.value, column: column++, depth });
    walk(node.right, depth + 1);
  };

  walk(root?.id ?? null, 0);
  return placed;
}

export function TreeView({ view }: { view: TreeViewPayload }) {
  const placed = place(view);
  const depth = Math.max(0, ...placed.map((node) => node.depth));

  return (
    <div className="tree" style={{ height: `${(depth + 1) * 64}px` }}>
      {placed.map((node) => (
        <span
          key={node.id}
          className="tree-node"
          data-testid={`node-${node.id}`}
          data-on-path={view.path.includes(node.id)}
          data-active={node.id === view.activeNode}
          data-inserted={node.id === view.insertedNode}
          style={{ left: `${node.column * 56}px`, top: `${node.depth * 64}px` }}
        >
          {node.value}
        </span>
      ))}
    </div>
  );
}
