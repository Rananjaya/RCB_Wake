import {useEffect, useState} from 'react';

/** Wall-clock "now" for display only, refreshed at [intervalMillis]. Never used for alarm timing. */
export function useNow(intervalMillis = 1000): number {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMillis);
    return () => clearInterval(id);
  }, [intervalMillis]);
  return now;
}
