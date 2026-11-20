import { CITIES, searchCities } from '../src/features/worldclock/cities';
import {
  offsetDifference,
  zoneTime,
} from '../src/features/worldclock/worldTime';

const H = 3_600_000;

describe('zoneTime', () => {
  const noonUtc = Date.UTC(2026, 10, 20, 12, 0);
  it('computes local wall time from an offset', () => {
    expect(zoneTime(noonUtc, 5.5 * H, 0)).toMatchObject({
      hour: 17,
      minute: 30,
      dayOffset: 0,
      isDay: true,
    });
  });
  it('detects tomorrow and yesterday relative to local time', () => {
    const lateUtc = Date.UTC(2026, 10, 20, 22, 0);
    expect(zoneTime(lateUtc, 9 * H, 0).dayOffset).toBe(1);
    expect(zoneTime(Date.UTC(2026, 10, 20, 2, 0), -5 * H, 0).dayOffset).toBe(
      -1,
    );
  });
  it('marks night hours', () => {
    expect(zoneTime(Date.UTC(2026, 10, 20, 23, 0), 0, 0).isDay).toBe(false);
  });
});

describe('offsetDifference', () => {
  it('handles half-hour zones and direction', () => {
    expect(offsetDifference(5.5 * H, 0)).toEqual({
      sign: 1,
      hours: 5,
      minutes: 30,
    });
    expect(offsetDifference(-5 * H, 5.5 * H)).toEqual({
      sign: -1,
      hours: 10,
      minutes: 30,
    });
    expect(offsetDifference(H, H).sign).toBe(0);
  });
});

describe('cities', () => {
  it('have unique names per zone, IANA ids and valid coordinates', () => {
    const keys = new Set(CITIES.map(c => `${c.name}|${c.zoneId}`));
    expect(keys.size).toBe(CITIES.length);
    for (const c of CITIES) {
      expect(c.zoneId).toMatch(/^[A-Za-z]+\/[A-Za-z_/-]+$/);
      expect(Math.abs(c.lat)).toBeLessThanOrEqual(90);
      expect(Math.abs(c.lon)).toBeLessThanOrEqual(180);
    }
  });
  it('search by city, country or zone', () => {
    expect(searchCities('colombo').map(c => c.name)).toContain('Colombo');
    expect(searchCities('japan').map(c => c.name)).toEqual(['Tokyo', 'Osaka']);
    expect(searchCities('  ')).toHaveLength(CITIES.length);
  });
});
