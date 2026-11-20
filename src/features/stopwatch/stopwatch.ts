/**
 * Stopwatch state on the monotonic clock (SystemClock.elapsedRealtime()).
 * Wall-clock time is never used, so clock changes cannot distort measurements.
 */
export type StopwatchState = {
  status: 'idle' | 'running' | 'paused';
  /** elapsedRealtime() when the current running segment started. */
  segmentStart: number;
  /** Time accumulated in previous segments. */
  accumulated: number;
  /** Cumulative split times, oldest first. */
  splits: number[];
  /** Boot count when last saved; elapsedRealtime() restarts from zero after a reboot. */
  bootCount: number;
};

export const initialStopwatch = (bootCount = 0): StopwatchState => ({
  status: 'idle',
  segmentStart: 0,
  accumulated: 0,
  splits: [],
  bootCount,
});

export function elapsed(state: StopwatchState, now: number): number {
  return state.status === 'running'
    ? state.accumulated + Math.max(0, now - state.segmentStart)
    : state.accumulated;
}

export function start(state: StopwatchState, now: number): StopwatchState {
  if (state.status === 'running') return state;
  return { ...state, status: 'running', segmentStart: now };
}

export function pause(state: StopwatchState, now: number): StopwatchState {
  if (state.status !== 'running') return state;
  return {
    ...state,
    status: 'paused',
    accumulated: elapsed(state, now),
    segmentStart: 0,
  };
}

export function lap(state: StopwatchState, now: number): StopwatchState {
  if (state.status !== 'running') return state;
  return { ...state, splits: [...state.splits, elapsed(state, now)] };
}

export function reset(state: StopwatchState): StopwatchState {
  return initialStopwatch(state.bootCount);
}

export function clearLaps(state: StopwatchState): StopwatchState {
  return { ...state, splits: [] };
}

export type LapRow = { index: number; lapTime: number; splitTime: number };

/** Individual lap durations derived from cumulative splits. */
export function lapRows(splits: number[]): LapRow[] {
  return splits.map((split, i) => ({
    index: i + 1,
    splitTime: split,
    lapTime: split - (i > 0 ? splits[i - 1] : 0),
  }));
}

/** Fastest and slowest lap numbers; only meaningful with two or more laps. */
export function lapExtremes(rows: LapRow[]): {
  fastest: number | null;
  slowest: number | null;
} {
  if (rows.length < 2) return { fastest: null, slowest: null };
  let fastest = rows[0];
  let slowest = rows[0];
  for (const r of rows) {
    if (r.lapTime < fastest.lapTime) fastest = r;
    if (r.lapTime > slowest.lapTime) slowest = r;
  }
  return { fastest: fastest.index, slowest: slowest.index };
}

/**
 * Restores a saved stopwatch. After a reboot the monotonic clock restarted, so a running
 * stopwatch cannot be continued accurately; it is restored paused at its last recorded value
 * (the latest lap or pause), never by guessing from wall-clock time.
 */
export function restore(
  saved: StopwatchState,
  bootCount: number,
): StopwatchState {
  if (saved.bootCount === bootCount) return saved;
  if (saved.status !== 'running') return { ...saved, bootCount };
  const lastKnown = Math.max(
    saved.accumulated,
    saved.splits[saved.splits.length - 1] ?? 0,
  );
  return {
    ...saved,
    status: 'paused',
    accumulated: lastKnown,
    segmentStart: 0,
    bootCount,
  };
}
