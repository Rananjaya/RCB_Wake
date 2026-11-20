import * as sw from '../src/features/stopwatch/stopwatch';

describe('stopwatch', () => {
  it('starts, pauses and resumes on the monotonic clock', () => {
    let s = sw.start(sw.initialStopwatch(1), 1000);
    expect(sw.elapsed(s, 1500)).toBe(500);
    s = sw.pause(s, 2000);
    expect(sw.elapsed(s, 99_999)).toBe(1000);
    s = sw.start(s, 5000);
    expect(sw.elapsed(s, 5250)).toBe(1250);
  });

  it('records laps and finds fastest and slowest', () => {
    let s = sw.start(sw.initialStopwatch(), 0);
    s = sw.lap(s, 1000);
    s = sw.lap(s, 1500);
    s = sw.lap(s, 3500);
    const rows = sw.lapRows(s.splits);
    expect(rows.map(r => r.lapTime)).toEqual([1000, 500, 2000]);
    expect(sw.lapExtremes(rows)).toEqual({ fastest: 2, slowest: 3 });
  });

  it('does not mark extremes with a single lap', () => {
    expect(sw.lapExtremes(sw.lapRows([1000]))).toEqual({
      fastest: null,
      slowest: null,
    });
  });

  it('ignores laps while paused and resets cleanly', () => {
    let s = sw.pause(sw.start(sw.initialStopwatch(3), 0), 100);
    expect(sw.lap(s, 200).splits).toEqual([]);
    s = sw.reset(s);
    expect(s).toEqual(sw.initialStopwatch(3));
  });

  it('clears laps without stopping', () => {
    const s = sw.clearLaps(sw.lap(sw.start(sw.initialStopwatch(), 0), 10));
    expect(s.splits).toEqual([]);
    expect(s.status).toBe('running');
  });

  it('restores unchanged within the same boot', () => {
    const s = sw.start(sw.initialStopwatch(5), 100);
    expect(sw.restore(s, 5)).toBe(s);
  });

  it('pauses at the last known value after a reboot', () => {
    let s = sw.start(sw.initialStopwatch(5), 100);
    s = sw.lap(s, 1100);
    const restored = sw.restore(s, 6);
    expect(restored.status).toBe('paused');
    expect(sw.elapsed(restored, 0)).toBe(1000);
  });
});
