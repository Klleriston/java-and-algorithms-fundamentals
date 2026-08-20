import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useTracePlayer } from './useTracePlayer';

describe('useTracePlayer', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('starts at the first step', () => {
    const { result } = renderHook(() => useTracePlayer(5));
    expect(result.current.index).toBe(0);
    expect(result.current.playing).toBe(false);
  });

  it('steps forward and back, clamping at both ends', () => {
    const { result } = renderHook(() => useTracePlayer(3));

    act(() => result.current.prev());
    expect(result.current.index).toBe(0);

    act(() => result.current.next());
    act(() => result.current.next());
    act(() => result.current.next());
    expect(result.current.index).toBe(2);

    act(() => result.current.prev());
    expect(result.current.index).toBe(1);
  });

  it('jumps to first and last', () => {
    const { result } = renderHook(() => useTracePlayer(4));
    act(() => result.current.last());
    expect(result.current.index).toBe(3);
    act(() => result.current.first());
    expect(result.current.index).toBe(0);
  });

  it('seeks to an arbitrary step, clamped', () => {
    const { result } = renderHook(() => useTracePlayer(4));
    act(() => result.current.seek(2));
    expect(result.current.index).toBe(2);
    act(() => result.current.seek(99));
    expect(result.current.index).toBe(3);
  });

  it('advances while playing and stops at the end', () => {
    const { result } = renderHook(() => useTracePlayer(3, 100));

    act(() => result.current.play());
    expect(result.current.playing).toBe(true);

    act(() => vi.advanceTimersByTime(100));
    expect(result.current.index).toBe(1);

    act(() => vi.advanceTimersByTime(100));
    expect(result.current.index).toBe(2);
    expect(result.current.playing).toBe(false);
  });

  it('pauses', () => {
    const { result } = renderHook(() => useTracePlayer(5, 100));
    act(() => result.current.play());
    act(() => result.current.pause());
    act(() => vi.advanceTimersByTime(500));
    expect(result.current.index).toBe(0);
  });

  it('resets to zero when the trace changes length', () => {
    const { result, rerender } = renderHook(({ count }) => useTracePlayer(count), {
      initialProps: { count: 5 },
    });
    act(() => result.current.last());
    expect(result.current.index).toBe(4);

    rerender({ count: 2 });
    expect(result.current.index).toBe(0);
  });
});
