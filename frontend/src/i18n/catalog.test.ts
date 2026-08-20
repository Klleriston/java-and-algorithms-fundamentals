import { describe, expect, it } from 'vitest';
import en from './en.json';
import pt from './pt.json';
import binarySearch from '../fixtures/binary-search.json';
import bubbleSort from '../fixtures/bubble-sort.json';
import bstInsert from '../fixtures/bst-insert.json';
import linkedListInsert from '../fixtures/linked-list-insert.json';
import type { Trace } from '../types';

const catalogs: Record<string, Record<string, string>> = { en, pt };
const traces = [binarySearch, bubbleSort, bstInsert, linkedListInsert] as unknown as Trace[];

function keysInUse(): { key: string; args: Record<string, unknown> }[] {
  return traces.flatMap((trace) => [
    { key: trace.titleKey, args: {} },
    ...trace.steps.map((step) => ({ key: step.messageKey, args: step.messageArgs })),
  ]);
}

describe('message catalogs', () => {
  it('define exactly the same keys in both languages', () => {
    expect(Object.keys(pt).sort()).toEqual(Object.keys(en).sort());
  });

  it('cover every key the backend emits', () => {
    for (const { key } of keysInUse()) {
      for (const [lang, catalog] of Object.entries(catalogs)) {
        expect(catalog[key], `${key} missing from ${lang}.json`).toBeDefined();
      }
    }
  });

  it('only use placeholders the backend actually sends', () => {
    for (const { key, args } of keysInUse()) {
      for (const [lang, catalog] of Object.entries(catalogs)) {
        const placeholders = [...(catalog[key] ?? '').matchAll(/\{(\w+)\}/g)].map((match) => match[1]);
        for (const placeholder of placeholders) {
          expect(args, `${key} in ${lang}.json wants {${placeholder}}`).toHaveProperty(placeholder);
        }
      }
    }
  });
});
