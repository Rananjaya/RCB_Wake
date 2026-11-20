import { create } from 'zustand';
import {
  engine,
  type WorldClockRecord,
  type ZoneOffset,
} from '../services/engine';

/** The device's current zone, resolved natively. */
export const LOCAL_ZONE = 'local';

type WorldClockState = {
  clocks: WorldClockRecord[];
  offsets: Record<string, ZoneOffset>;
  load: () => Promise<void>;
  refreshOffsets: () => Promise<void>;
  add: (cityName: string, zoneId: string) => Promise<void>;
  remove: (id: number) => Promise<void>;
};

export const useWorldClockStore = create<WorldClockState>((set, get) => ({
  clocks: [],
  offsets: {},
  load: async () => set({ clocks: await engine.getWorldClocks() }),
  refreshOffsets: async () => {
    const zones = [
      ...new Set([LOCAL_ZONE, ...get().clocks.map(c => c.zoneId)]),
    ];
    const result = await engine.getZoneOffsets(zones, Date.now());
    set({ offsets: Object.fromEntries(result.map(o => [o.zoneId, o])) });
  },
  add: async (cityName, zoneId) => {
    await engine.addWorldClock(cityName, zoneId);
    await get().load();
  },
  remove: async id => {
    await engine.removeWorldClock(id);
    await get().load();
  },
}));
