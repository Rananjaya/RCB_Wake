import {create} from 'zustand';
import {engine, type AlarmConfig, type AlarmRecord, type EngineStatus, type SessionInfo} from '../services/engine';

type AlarmState = {
  alarms: AlarmRecord[];
  status: EngineStatus | null;
  session: SessionInfo | null;
  loaded: boolean;
  load: () => Promise<void>;
  refreshStatus: () => Promise<void>;
  refreshSession: () => Promise<void>;
  save: (config: AlarmConfig) => Promise<AlarmRecord>;
  setEnabled: (id: number, enabled: boolean) => Promise<AlarmRecord>;
  remove: (id: number) => Promise<void>;
};

export const useAlarmStore = create<AlarmState>((set, get) => ({
  alarms: [],
  status: null,
  session: null,
  loaded: false,
  load: async () => {
    set({alarms: await engine.getAlarms(), loaded: true});
  },
  refreshStatus: async () => {
    set({status: await engine.getAlarmEngineStatus()});
  },
  refreshSession: async () => {
    set({session: await engine.getActiveSession()});
  },
  save: async config => {
    const saved = await engine.saveAlarm(config);
    await get().load();
    return saved;
  },
  setEnabled: async (id, enabled) => {
    // Optimistic toggle; the engine's answer (including schedule state) replaces it.
    set({alarms: get().alarms.map(a => (a.id === id ? {...a, enabled} : a))});
    try {
      const updated = await engine.setAlarmEnabled(id, enabled);
      set({alarms: get().alarms.map(a => (a.id === id ? updated : a))});
      return updated;
    } catch (e) {
      await get().load();
      throw e;
    }
  },
  remove: async id => {
    await engine.deleteAlarm(id);
    set({alarms: get().alarms.filter(a => a.id !== id)});
  },
}));
