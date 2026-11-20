/**
 * In-memory stand-in for the Kotlin TurboModule, used only by Jest. It mirrors the contract in
 * src/native/NativeRcbWake.ts closely enough to exercise stores and screens.
 */
const state = {
  alarms: [],
  timers: [],
  clocks: [],
  values: {},
  nextId: 1,
  elapsed: 1_000_000,
  settings: null,
};

const defaults = {
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
  weatherEnabled: false,
  weatherCity: '',
  weatherLatitude: 0,
  weatherLongitude: 0,
  temperatureUnit: 'c',
};

const defaultPomodoroConfig = {
  focusMinutes: 25,
  shortBreakMinutes: 5,
  longBreakMinutes: 15,
  roundsBeforeLongBreak: 4,
  autoStartNext: true,
};

function freshPomodoro(config = defaultPomodoroConfig) {
  return {
    config: { ...config },
    status: 'IDLE',
    phase: 'FOCUS',
    completedFocus: 0,
    totalFocus: 0,
    remainingMillis: config.focusMinutes * 60_000,
    endElapsedMillis: 0,
  };
}

function reset() {
  state.pomodoro = freshPomodoro();
  state.alarms = [];
  state.timers = [];
  state.clocks = [];
  state.values = {};
  state.nextId = 1;
  state.elapsed = 1_000_000;
  state.settings = { ...defaults };
}
reset();

const nextTrigger = a => (a.enabled ? Date.now() + 3_600_000 : 0);

const engine = {
  __state: state,
  __reset: reset,
  __advance: ms => {
    state.elapsed += ms;
  },
  onEngineEvent: () => ({ remove: () => {} }),
  initializeEngine: async () => engine.getAlarmEngineStatus(),
  getAlarmEngineStatus: async () => ({
    exactAlarmPermitted: true,
    notificationsPermitted: true,
    fullScreenIntentPermitted: true,
    sdkInt: 36,
    versionName: '1.0.0',
    versionCode: 100,
    scheduledCount: state.alarms.filter(a => a.enabled).length,
    failedCount: 0,
  }),
  requestExactAlarmPermission: () => {},
  requestFullScreenIntentPermission: () => {},
  openNotificationSettings: () => {},
  getAlarms: async () => state.alarms.map(a => ({ ...a })),
  saveAlarm: async config => {
    if (config.hour < 0 || config.hour > 23)
      throw new Error('hour must be 0-23');
    const now = Date.now();
    const existing = state.alarms.find(a => a.id === config.id);
    const record = {
      ...config,
      id: existing ? existing.id : state.nextId++,
      createdAt: existing ? existing.createdAt : now,
      updatedAt: now,
      snoozedUntil: 0,
    };
    record.nextTriggerAt = nextTrigger(record);
    record.scheduleState = record.enabled ? 'EXACT' : 'NONE';
    state.alarms = existing
      ? state.alarms.map(a => (a.id === record.id ? record : a))
      : [...state.alarms, record];
    return { ...record };
  },
  setAlarmEnabled: async (id, enabled) => {
    const a = state.alarms.find(x => x.id === id);
    a.enabled = enabled;
    a.nextTriggerAt = nextTrigger(a);
    a.scheduleState = enabled ? 'EXACT' : 'NONE';
    return { ...a };
  },
  deleteAlarm: async id => {
    state.alarms = state.alarms.filter(a => a.id !== id);
  },
  cancelAllAlarms: async () => {},
  getScheduledAlarms: async () => [],
  getNextAlarm: async () => null,
  getActiveSession: async () => null,
  snoozeSession: async () => 0,
  dismissSession: async () => false,
  getBuiltInSounds: async () => [{ uri: 'builtin:dawn', name: 'Dawn' }],
  pickCustomSound: async () => null,
  previewSound: () => {},
  stopPreview: () => {},
  showTimePicker: async (hour, minute) => ({ hour, minute, cancelled: true }),
  getTimers: async () => state.timers.map(t => ({ ...t })),
  createTimer: async (label, durationMillis, start) => {
    const t = {
      id: state.nextId++,
      label,
      durationMillis,
      remainingMillis: durationMillis,
      endElapsedMillis: start ? state.elapsed + durationMillis : 0,
      status: start ? 'RUNNING' : 'IDLE',
      createdAt: Date.now(),
    };
    state.timers.push(t);
    return { ...t };
  },
  startTimer: async id => {
    const t = state.timers.find(x => x.id === id);
    t.status = 'RUNNING';
    t.endElapsedMillis = state.elapsed + t.remainingMillis;
    return { ...t };
  },
  pauseTimer: async id => {
    const t = state.timers.find(x => x.id === id);
    t.remainingMillis = Math.max(0, t.endElapsedMillis - state.elapsed);
    t.status = 'PAUSED';
    t.endElapsedMillis = 0;
    return { ...t };
  },
  resetTimer: async id => {
    const t = state.timers.find(x => x.id === id);
    Object.assign(t, {
      status: 'IDLE',
      remainingMillis: t.durationMillis,
      endElapsedMillis: 0,
    });
    return { ...t };
  },
  addTimerTime: async (id, ms) => {
    const t = state.timers.find(x => x.id === id);
    if (t.status === 'RUNNING') t.endElapsedMillis += ms;
    else t.remainingMillis += ms;
    return { ...t };
  },
  deleteTimer: async id => {
    state.timers = state.timers.filter(t => t.id !== id);
  },
  elapsedRealtime: () => state.elapsed,
  getBootCount: () => 7,
  getWorldClocks: async () => state.clocks.map(c => ({ ...c })),
  addWorldClock: async (cityName, zoneId) => {
    const c = {
      id: state.nextId++,
      cityName,
      zoneId,
      displayOrder: state.clocks.length,
    };
    state.clocks.push(c);
    return { ...c };
  },
  removeWorldClock: async id => {
    state.clocks = state.clocks.filter(c => c.id !== id);
  },
  getZoneOffsets: async zoneIds =>
    zoneIds.map(zoneId => ({ zoneId, offsetMillis: 0, nextTransitionAt: 0 })),
  getSettings: async () => ({ ...state.settings }),
  saveSettings: async s => {
    state.settings = { ...s };
    return { ...s };
  },
  getValue: async key => state.values[key] ?? null,
  setValue: async (key, value) => {
    state.values[key] = value;
  },
  removeValue: async key => {
    delete state.values[key];
  },
  getPomodoro: async () => ({
    ...state.pomodoro,
    config: { ...state.pomodoro.config },
  }),
  startPomodoro: async () => {
    const p = state.pomodoro;
    p.status = 'RUNNING';
    p.endElapsedMillis = state.elapsed + p.remainingMillis;
    return engine.getPomodoro();
  },
  pausePomodoro: async () => {
    const p = state.pomodoro;
    p.remainingMillis = Math.max(0, p.endElapsedMillis - state.elapsed);
    p.status = 'PAUSED';
    p.endElapsedMillis = 0;
    return engine.getPomodoro();
  },
  skipPomodoroPhase: async () => {
    const p = state.pomodoro;
    if (p.phase === 'FOCUS') {
      p.completedFocus += 1;
      p.totalFocus += 1;
      p.phase =
        p.completedFocus >= p.config.roundsBeforeLongBreak
          ? 'LONG_BREAK'
          : 'SHORT_BREAK';
    } else {
      if (p.phase === 'LONG_BREAK') p.completedFocus = 0;
      p.phase = 'FOCUS';
    }
    const minutes = {
      FOCUS: 'focusMinutes',
      SHORT_BREAK: 'shortBreakMinutes',
      LONG_BREAK: 'longBreakMinutes',
    }[p.phase];
    p.remainingMillis = p.config[minutes] * 60_000;
    return engine.getPomodoro();
  },
  resetPomodoro: async () => {
    state.pomodoro = freshPomodoro(state.pomodoro.config);
    return engine.getPomodoro();
  },
  savePomodoroConfig: async config => {
    state.pomodoro.config = { ...config };
    if (state.pomodoro.status === 'IDLE')
      state.pomodoro.remainingMillis = config.focusMinutes * 60_000;
    return engine.getPomodoro();
  },
  getWeather: async () => null,
  refreshWeather: async () => null,
};

module.exports = { __esModule: true, default: engine };
