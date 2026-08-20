import type { ComponentType } from 'react';
import type { ViewPayload } from '../../types';
import { ArrayView } from './ArrayView';
import { TreeView } from './TreeView';
import { FallbackView } from './FallbackView';
import { HashTableView } from './HashTableView';

// Later phases register new kinds here and nowhere else.
const renderers: Record<string, ComponentType<{ view: any }>> = {
  ARRAY: ArrayView,
  HASH_TABLE: HashTableView,
  TREE: TreeView,
};

export function viewFor(kind: string): ComponentType<{ view: ViewPayload }> {
  return renderers[kind] ?? FallbackView;
}
