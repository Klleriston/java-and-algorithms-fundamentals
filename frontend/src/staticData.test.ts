import { describe, expect, it } from 'vitest';
import { isDefaultParams, staticDemos, staticTrace } from './staticData';

describe('static data', () => {
  it('ships one recorded trace per demo in the catalog', () => {
    const catalog = staticDemos();
    expect(catalog.length).toBeGreaterThan(0);
    for (const demo of catalog) {
      expect(staticTrace(demo.id), `no recorded trace for ${demo.id}`).toBeDefined();
    }
  });

  it('recognises the parameters each demo opens with', () => {
    for (const demo of staticDemos()) {
      const defaults = Object.fromEntries(demo.parameters.map((spec) => [spec.name, spec.defaultValue]));
      expect(isDefaultParams(demo.id, defaults), `${demo.id} should match its own defaults`).toBe(true);
    }
  });

  it('recognises a changed parameter', () => {
    const demo = staticDemos()[0];
    const changed = Object.fromEntries(demo.parameters.map((spec) => [spec.name, spec.defaultValue]));
    const first = demo.parameters[0].name;
    changed[first] = 'something else';
    expect(isDefaultParams(demo.id, changed)).toBe(false);
  });

  it('serves the recorded trace for the demo that was asked for', () => {
    for (const demo of staticDemos()) {
      expect(staticTrace(demo.id)!.demoId).toBe(demo.id);
    }
  });
});
