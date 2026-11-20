import type {StringKey, Translate} from '../i18n';

/** Monday-first day order matching the native repeat mask (bit 0 = Monday … bit 6 = Sunday). */
export const DAY_KEYS: StringKey[] = ['dayMon', 'dayTue', 'dayWed', 'dayThu', 'dayFri', 'daySat', 'daySun'];
export const DAY_LETTER_KEYS: StringKey[] = [
  'letterMon',
  'letterTue',
  'letterWed',
  'letterThu',
  'letterFri',
  'letterSat',
  'letterSun',
];

export const ALL_DAYS = 0b1111111;
export const WEEKDAYS = 0b0011111;
export const WEEKENDS = 0b1100000;

export const pad2 = (n: number) => String(n).padStart(2, '0');

export type ClockParts = {time: string; period: 'am' | 'pm' | null};

export function formatClock(hour: number, minute: number, is24Hour: boolean): ClockParts {
  if (is24Hour) return {time: `${pad2(hour)}:${pad2(minute)}`, period: null};
  const h12 = hour % 12 === 0 ? 12 : hour % 12;
  return {time: `${h12}:${pad2(minute)}`, period: hour < 12 ? 'am' : 'pm'};
}

export function formatClockText(hour: number, minute: number, is24Hour: boolean, t: Translate): string {
  const {time, period} = formatClock(hour, minute, is24Hour);
  return period ? `${time} ${t(period)}` : time;
}

export function toggleDay(mask: number, dayIndex: number): number {
  return mask ^ (1 << dayIndex);
}

export function isDaySelected(mask: number, dayIndex: number): boolean {
  return (mask & (1 << dayIndex)) !== 0;
}

export function repeatSummary(mask: number, t: Translate): string {
  if (mask === 0) return t('repeatOnce');
  if (mask === ALL_DAYS) return t('repeatEveryDay');
  if (mask === WEEKDAYS) return t('repeatWeekdays');
  if (mask === WEEKENDS) return t('repeatWeekends');
  return DAY_KEYS.filter((_, i) => isDaySelected(mask, i))
    .map(k => t(k))
    .join(', ');
}

/** "7 h 12 min" style duration until a future instant, rounded up to the next minute. */
export function formatUntil(fromMillis: number, toMillis: number, t: Translate): string {
  const diff = toMillis - fromMillis;
  if (diff < 60_000) return t('durationUnderMinute');
  const totalMinutes = Math.ceil(diff / 60_000);
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  return h > 0 ? t('durationHoursMinutes', {h, m}) : t('durationMinutes', {m});
}

/** Countdown display: H:MM:SS when an hour or more remains, otherwise MM:SS. Rounds up. */
export function formatCountdown(millis: number): string {
  const total = Math.max(0, Math.ceil(millis / 1000));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return h > 0 ? `${h}:${pad2(m)}:${pad2(s)}` : `${pad2(m)}:${pad2(s)}`;
}

/** Stopwatch display with centiseconds: MM:SS.cc, or H:MM:SS.cc past an hour. Truncates. */
export function formatStopwatch(millis: number): string {
  const ms = Math.max(0, Math.floor(millis));
  const cs = Math.floor((ms % 1000) / 10);
  const totalSeconds = Math.floor(ms / 1000);
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  const base = `${pad2(m)}:${pad2(s)}.${pad2(cs)}`;
  return h > 0 ? `${h}:${base}` : base;
}
