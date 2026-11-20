import {
  bufferToMillis,
  bufferToParts,
  displayRemaining,
  popDigit,
  pushDigit,
} from '../src/features/timer/timerEntry';

describe('timer keypad entry', () => {
  it('fills digits from the right', () => {
    let b = '';
    for (const d of ['1', '3', '0']) b = pushDigit(b, d);
    expect(bufferToParts(b)).toEqual({ h: 0, m: 1, s: 30 });
    expect(bufferToMillis(b)).toBe(90_000);
  });
  it('ignores leading zeros, non-digits and overflow', () => {
    expect(pushDigit('', '0')).toBe('');
    expect(pushDigit('12', 'x')).toBe('12');
    expect(pushDigit('123456', '7')).toBe('123456');
  });
  it('carries over seconds above 59', () => {
    expect(bufferToMillis('90')).toBe(90_000);
  });
  it('deletes the last digit', () => {
    expect(popDigit('123')).toBe('12');
  });
});

describe('displayRemaining', () => {
  it('uses the monotonic end time for running timers', () => {
    expect(
      displayRemaining(
        { status: 'RUNNING', remainingMillis: 0, endElapsedMillis: 10_000 },
        4_000,
      ),
    ).toBe(6_000);
    expect(
      displayRemaining(
        { status: 'RUNNING', remainingMillis: 0, endElapsedMillis: 10_000 },
        12_000,
      ),
    ).toBe(0);
  });
  it('uses stored remaining time when paused', () => {
    expect(
      displayRemaining(
        { status: 'PAUSED', remainingMillis: 3_000, endElapsedMillis: 0 },
        99,
      ),
    ).toBe(3_000);
    expect(
      displayRemaining(
        { status: 'FINISHED', remainingMillis: 3_000, endElapsedMillis: 0 },
        99,
      ),
    ).toBe(0);
  });
});
