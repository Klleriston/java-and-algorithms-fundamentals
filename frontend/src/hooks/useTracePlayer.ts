import { useCallback, useEffect, useRef, useState } from 'react';

export const DEFAULT_SPEED_MS = 600;

export function useTracePlayer(stepCount: number, initialSpeedMs: number = DEFAULT_SPEED_MS) {
  const [index, setIndex] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [speedMs, setSpeed] = useState(initialSpeedMs);
  const lastIndex = Math.max(stepCount - 1, 0);
  const lastIndexRef = useRef(lastIndex);
  lastIndexRef.current = lastIndex;

  useEffect(() => {
    setIndex(0);
    setPlaying(false);
  }, [stepCount]);

  useEffect(() => {
    if (!playing) return;
    const timer = setInterval(() => {
      setIndex((current) => {
        const next = current + 1;
        if (next >= lastIndexRef.current) {
          setPlaying(false);
          return lastIndexRef.current;
        }
        return next;
      });
    }, speedMs);
    return () => clearInterval(timer);
  }, [playing, speedMs]);

  const clamp = useCallback((value: number) => Math.min(Math.max(value, 0), lastIndex), [lastIndex]);

  return {
    index,
    playing,
    speedMs,
    setSpeed,
    play: useCallback(() => setPlaying(true), []),
    pause: useCallback(() => setPlaying(false), []),
    toggle: useCallback(() => setPlaying((value) => !value), []),
    next: useCallback(() => setIndex((current) => clamp(current + 1)), [clamp]),
    prev: useCallback(() => setIndex((current) => clamp(current - 1)), [clamp]),
    first: useCallback(() => setIndex(0), []),
    last: useCallback(() => setIndex(lastIndex), [lastIndex]),
    seek: useCallback((value: number) => setIndex(clamp(value)), [clamp]),
  };
}
