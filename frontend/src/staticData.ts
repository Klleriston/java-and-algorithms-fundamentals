import type { DemoSummary, Trace } from './types';
import demos from './fixtures/demos.json';
import binarySearch from './fixtures/binary-search.json';
import bubbleSort from './fixtures/bubble-sort.json';
import hashMapPut from './fixtures/hash-map-put.json';
import hashSetAdd from './fixtures/hash-set-add.json';
import bstInsert from './fixtures/bst-insert.json';
import linkedListInsert from './fixtures/linked-list-insert.json';

/**
 * The build published to GitHub Pages has no backend behind it — Pages serves static files
 * and the tracer is Java. It replays the golden fixtures instead: the same JSON the backend
 * produces, frozen at the parameters each demo opens with.
 */
export const STATIC_MODE = import.meta.env.VITE_STATIC === 'true';

const traces: Record<string, unknown> = {
  'binary-search': binarySearch,
  'bubble-sort': bubbleSort,
  'hash-map-put': hashMapPut,
  'hash-set-add': hashSetAdd,
  'bst-insert': bstInsert,
  'linked-list-insert': linkedListInsert,
};

export function staticDemos(): DemoSummary[] {
  return demos as unknown as DemoSummary[];
}

/** Same shape the form sends, so a changed parameter is detectable rather than silently ignored. */
function normalize(value: unknown): string {
  return Array.isArray(value) ? value.join(',') : String(value);
}

export function isDefaultParams(id: string, params: Record<string, unknown>): boolean {
  const demo = staticDemos().find((candidate) => candidate.id === id);
  if (demo === undefined) return false;
  return demo.parameters.every((spec) => normalize(params[spec.name]) === normalize(spec.defaultValue));
}

export function staticTrace(id: string): Trace | undefined {
  return traces[id] as Trace | undefined;
}
