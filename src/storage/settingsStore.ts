import { create } from 'zustand';
import { engine, type Settings } from '../services/engine';

export const defaultSettings: Settings = {
  theme: 'midnight',
  accentColor: 'default',
  timeFormat: 'system',
  is24Hour: false,
  language: 'en',
  defaultSoundUri: 'builtin:dawn',
  defaultSoundName: 'Dawn',
  defaultSnoozeMinutes: 10,
  defaultVibration: true,
  defaultGradualVolume: true,
  defaultChallengeEnabled: false,
  defaultChallengeDifficulty: 'easy',
  defaultChallengeQuestionCount: 3,
  weatherEnabled: false,
  weatherCity: '',
  weatherUseLocation: false,
  weatherLatitude: 0,
  weatherLongitude: 0,
  temperatureUnit: 'c',
  sleepReminderEnabled: false,
  sleepGoalMinutes: 480,
  sleepReminderLeadMinutes: 30,
  widgetTextColor: 'white',
  weatherRefreshMinutes: 30,
  widgetShowPlace: true,
  widgetShowHeadphones: false,
  widgetShowGreeting: true,
  widgetRotatePages: false,
  remindersEnabled: true,
  reminderLeadMinutes: 10,
  weatherProvider: 'openmeteo',
  openWeatherMapApiKey: '',
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
    set({ settings, loaded: true });
  },
  update: async patch => {
    const previous = get().settings;
    const next = { ...previous, ...patch };
    set({ settings: next });
    try {
      set({ settings: await engine.saveSettings(next) });
    } catch (e) {
      set({ settings: previous });
      throw e;
    }
  },
}));
