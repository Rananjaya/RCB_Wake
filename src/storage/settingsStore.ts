import {create} from 'zustand';
import {engine, type Settings} from '../services/engine';

export const defaultSettings: Settings = {
  theme: 'midnight',
  timeFormat: '12',
  language: 'en',
  defaultSoundUri: 'builtin:dawn',
  defaultSoundName: 'Dawn',
  defaultSnoozeMinutes: 10,
  defaultVibration: true,
  defaultGradualVolume: true,
  defaultChallengeEnabled: false,
  defaultChallengeDifficulty: 'easy',
  defaultChallengeQuestionCount: 3,
};

type SettingsState = {
  settings: Settings;
  loaded: boolean;
  load: () => Promise<void>;
  update: (patch: Partial<Settings>) => Promise<void>;
};

export const useSettingsStore = create<SettingsState>((set, get) => ({
  settings: defaultSettings,
  loaded: false,
  load: async () => {
    const settings = await engine.getSettings();
    set({settings, loaded: true});
  },
  update: async patch => {
    const previous = get().settings;
    const next = {...previous, ...patch};
    set({settings: next});
    try {
      set({settings: await engine.saveSettings(next)});
    } catch (e) {
      set({settings: previous});
      throw e;
    }
  },
}));
