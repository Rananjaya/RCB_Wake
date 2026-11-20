import { translate } from '../src/i18n';
import {
  ALL_DAYS,
  WEEKDAYS,
  formatClock,
  formatCountdown,
  formatStopwatch,
  formatUntil,
  isDaySelected,
  repeatSummary,
  toggleDay,
} from '../src/utils/time';

const t = (
  key: Parameters<typeof translate>[1],
  params?: Record<string, string | number>,
) => translate('en', key, params);

describe('formatClock', () => {
  it('formats 12-hour time with period', () => {
    expect(formatClock(0, 5, false)).toEqual({ time: '12:05', period: 'am' });
    expect(formatClock(12, 0, false)).toEqual({ time: '12:00', period: 'pm' });
    expect(formatClock(23, 59, false)).toEqual({ time: '11:59', period: 'pm' });
  });
  it('formats 24-hour time', () => {
    expect(formatClock(7, 3, true)).toEqual({ time: '07:03', period: null });
  });
});

describe('repeat days', () => {
  it('toggles Monday-first bits', () => {
    let mask = 0;
    mask = toggleDay(mask, 0);
    mask = toggleDay(mask, 6);
    expect(isDaySelected(mask, 0)).toBe(true);
    expect(isDaySelected(mask, 6)).toBe(true);
    expect(isDaySelected(mask, 3)).toBe(false);
    expect(toggleDay(mask, 0)).toBe(0b1000000);
  });
  it('summarises common patterns', () => {
    expect(repeatSummary(0, t)).toBe('Once');
    expect(repeatSummary(ALL_DAYS, t)).toBe('Every day');
    expect(repeatSummary(WEEKDAYS, t)).toBe('Weekdays');
    expect(repeatSummary(0b0000101, t)).toBe('Mon, Wed');
  });
});

describe('durations', () => {
  it('rounds time-until up to the next minute', () => {
    expect(formatUntil(0, 30_000, t)).toBe('less than a minute');
    expect(formatUntil(0, 61_000, t)).toBe('2 min');
    expect(formatUntil(0, (7 * 60 + 12) * 60_000, t)).toBe('7 h 12 min');
  });
  it('formats countdowns rounding up to whole seconds', () => {
    expect(formatCountdown(0)).toBe('00:00');
    expect(formatCountdown(59_001)).toBe('01:00');
    expect(formatCountdown(3_600_000)).toBe('1:00:00');
  });
  it('formats stopwatch with centiseconds', () => {
    expect(formatStopwatch(61_234)).toBe('01:01.23');
    expect(formatStopwatch(3_723_450)).toBe('1:02:03.45');
  });
});

describe('accent colours', () => {
  const {
    accents,
    midnightPurple,
    pinkDream,
    withAccent,
  } = require('../src/theme/themes');
  it('keep the theme when set to default', () => {
    expect(withAccent(midnightPurple, 'default')).toBe(midnightPurple);
  });
  it('apply the dark or light variant to the matching theme', () => {
    expect(withAccent(midnightPurple, 'teal').colors.primary).toBe(
      accents.teal.dark.primary,
    );
    expect(withAccent(pinkDream, 'teal').colors.primary).toBe(
      accents.teal.light.primary,
    );
    expect(withAccent(pinkDream, 'teal').colors.background).toBe(
      pinkDream.colors.background,
    );
  });
});
