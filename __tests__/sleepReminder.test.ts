import {
  formatGoal,
  nextReminderAt,
} from '../src/features/settings/SleepReminderSettings';

const H = 3_600_000;

describe('sleep reminder', () => {
  it('fires goal plus lead before the alarm', () => {
    expect(nextReminderAt(30 * H, 480, 30, 0)).toBe(30 * H - 8.5 * H);
  });
  it('is skipped when bedtime has already passed or there is no alarm', () => {
    expect(nextReminderAt(6 * H, 480, 30, 0)).toBeNull();
    expect(nextReminderAt(0, 480, 30, 0)).toBeNull();
  });
  it('formats the sleep goal', () => {
    expect(formatGoal(480)).toBe('8 h');
    expect(formatGoal(450)).toBe('7 h 30 min');
  });
});
