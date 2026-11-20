import {create} from 'zustand';
import {engine, type TimerRecord} from '../services/engine';

type TimerState = {
  timers: TimerRecord[];
  load: () => Promise<void>;
  create: (label: string, durationMillis: number) => Promise<void>;
  start: (id: number) => Promise<void>;
  pause: (id: number) => Promise<void>;
  reset: (id: number) => Promise<void>;
  addMinute: (id: number) => Promise<void>;
  remove: (id: number) => Promise<void>;
};

/** Thin view of native timer state. Every action round-trips through Kotlin, which owns the truth. */
export const useTimerStore = create<TimerState>(set => {
  const reload = async () => set({timers: await engine.getTimers()});
  return {
    timers: [],
    load: reload,
    create: async (label, durationMillis) => {
      await engine.createTimer(label, durationMillis, true);
      await reload();
    },
    start: async id => {
      await engine.startTimer(id);
      await reload();
    },
    pause: async id => {
      await engine.pauseTimer(id);
      await reload();
    },
    reset: async id => {
      await engine.resetTimer(id);
      await reload();
    },
    addMinute: async id => {
      await engine.addTimerTime(id, 60_000);
      await reload();
    },
    remove: async id => {
      await engine.deleteTimer(id);
      await reload();
    },
  };
});
