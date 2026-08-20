import type { ComponentType } from 'react';
import type { ViewPayload } from '../../types';
import { ArrayView } from './ArrayView';
import { FallbackView } from './FallbackView';

// Later phases register new kinds here and nowhere else.
const renderers: Record<string, ComponentType<{ view: any }>> = {
  ARRAY: ArrayView,
};

export function viewFor(kind: string): ComponentType<{ view: ViewPayload }> {
  return renderers[kind] ?? FallbackView;
}
