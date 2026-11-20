import { create } from 'zustand';
import {
  engine,
  type PomodoroConfig,
  type PomodoroState,
} from '../services/engine';

type PomodoroStore = {
  state: PomodoroState | null;
  load: () => Promise<void>;
  start: () => Promise<void>;
  pause: () => Promise<void>;
  skip: () => Promise<void>;
  reset: () => Promise<void>;
  saveConfig: (config: PomodoroConfig) => Promise<void>;
};

/** View of the native Pomodoro state; Kotlin owns the cycle and phase changes. */
export const usePomodoroStore = create<PomodoroStore>(set => ({
  state: null,
  load: async () => set({ state: await engine.getPomodoro() }),
  start: async () => set({ state: await engine.startPomodoro() }),
  pause: async () => set({ state: await engine.pausePomodoro() }),
  skip: async () => set({ state: await engine.skipPomodoroPhase() }),
  reset: async () => set({ state: await engine.resetPomodoro() }),
  saveConfig: async config =>
    set({ state: await engine.savePomodoroConfig(config) }),
}));
