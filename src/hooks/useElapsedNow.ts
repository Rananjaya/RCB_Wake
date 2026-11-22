import { useEffect, useReducer } from 'react';
import { engine } from '../services/engine';

/**
 * Current SystemClock.elapsedRealtime() for countdown displays.
 *
 * The clock is read on every render rather than cached in state: a cached value is stale the
 * moment a timer starts, which made countdowns briefly show a few extra seconds before the first
 * tick corrected them. The interval only triggers re-renders while [active].
 */
export function useElapsedNow(active: boolean, intervalMillis = 250): number {
  const [, rerender] = useReducer((n: number) => n + 1, 0);
  useEffect(() => {
    if (!active) return;
    const id = setInterval(rerender, intervalMillis);
    return () => clearInterval(id);
  }, [active, intervalMillis]);
  return engine.elapsedRealtime();
}
