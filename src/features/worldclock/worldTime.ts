/**
 * World-clock arithmetic from UTC offsets supplied by java.time (which owns the IANA rules and
 * daylight-saving transitions). Offsets are re-fetched whenever a transition passes.
 */
export type ZoneTime = {hour: number; minute: number; dayOffset: -1 | 0 | 1; isDay: boolean};

/** Calendar day number (days since epoch) of a local wall time. */
const localDay = (utcMillis: number, offsetMillis: number) => Math.floor((utcMillis + offsetMillis) / 86_400_000);

export function zoneTime(utcMillis: number, zoneOffsetMillis: number, localOffsetMillis: number): ZoneTime {
  const shifted = new Date(utcMillis + zoneOffsetMillis);
  const hour = shifted.getUTCHours();
  const diffDays = localDay(utcMillis, zoneOffsetMillis) - localDay(utcMillis, localOffsetMillis);
  return {
    hour,
    minute: shifted.getUTCMinutes(),
    dayOffset: Math.max(-1, Math.min(1, diffDays)) as -1 | 0 | 1,
    // A simple 06:00–18:00 daytime window; it is an indicator, not a sunrise calculation.
    isDay: hour >= 6 && hour < 18,
  };
}

/** "5 h 30 min" difference string parts relative to local time. Positive = ahead. */
export function offsetDifference(zoneOffsetMillis: number, localOffsetMillis: number): {
  sign: -1 | 0 | 1;
  hours: number;
  minutes: number;
} {
  const diffMinutes = Math.round((zoneOffsetMillis - localOffsetMillis) / 60_000);
  const abs = Math.abs(diffMinutes);
  return {sign: Math.sign(diffMinutes) as -1 | 0 | 1, hours: Math.floor(abs / 60), minutes: abs % 60};
}
