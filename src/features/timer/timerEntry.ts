/**
 * Keypad timer entry: digits fill from the right like a microwave keypad
 * ("1","3","0" → 00:01:30). At most six digits (HHMMSS).
 */
export const MAX_DIGITS = 6;

export function pushDigit(buffer: string, digit: string): string {
  if (!/^\d$/.test(digit)) return buffer;
  if (buffer.length >= MAX_DIGITS) return buffer;
  if (buffer === '' && digit === '0') return buffer;
  return buffer + digit;
}

export function popDigit(buffer: string): string {
  return buffer.slice(0, -1);
}

export function bufferToParts(buffer: string): {h: number; m: number; s: number} {
  const padded = buffer.padStart(MAX_DIGITS, '0');
  return {h: Number(padded.slice(0, 2)), m: Number(padded.slice(2, 4)), s: Number(padded.slice(4, 6))};
}

/** Minutes and seconds above 59 carry over (e.g. 90 s → 1:30), as on most kitchen timers. */
export function bufferToMillis(buffer: string): number {
  const {h, m, s} = bufferToParts(buffer);
  return ((h * 60 + m) * 60 + s) * 1000;
}

/** Remaining time of a timer for display, computed from the monotonic clock. */
export function displayRemaining(
  timer: {status: string; remainingMillis: number; endElapsedMillis: number},
  nowElapsed: number,
): number {
  if (timer.status === 'RUNNING') return Math.max(0, timer.endElapsedMillis - nowElapsed);
  if (timer.status === 'FINISHED') return 0;
  return timer.remainingMillis;
}

export const PRESETS_MINUTES = [1, 3, 5, 10, 15, 30];
