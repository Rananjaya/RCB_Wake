/**
 * TurboModule contract between React Native and the Kotlin alarm engine.
 * See docs/native-contract.md for the full request/response/event documentation.
 *
 * Kotlin owns all persistence (SQLite + preferences). This module is the only way JavaScript
 * reads or writes alarms, timers, world clocks and settings.
 */
import type { CodegenTypes, TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type AlarmConfig = {
  /** 0 creates a new alarm; a positive id updates an existing one. */
  id: number;
  hour: number;
  minute: number;
  enabled: boolean;
  /** Bitmask, bit 0 = Monday … bit 6 = Sunday. 0 = one-time alarm. */
  repeatDays: number;
  label: string;
  /** `builtin:<name>`, `system:default` or a `content://` URI from the Storage Access Framework. */
  soundUri: string;
  soundName: string;
  vibrationEnabled: boolean;
  gradualVolumeEnabled: boolean;
  snoozeMinutes: number;
  challengeEnabled: boolean;
  /** `easy` | `medium` | `hard` */
  challengeDifficulty: string;
  challengeQuestionCount: number;
  /** `math` | `nfc` */
  challengeType: string;
};

export type AlarmRecord = {
  id: number;
  hour: number;
  minute: number;
  enabled: boolean;
  repeatDays: number;
  label: string;
  soundUri: string;
  soundName: string;
  vibrationEnabled: boolean;
  gradualVolumeEnabled: boolean;
  snoozeMinutes: number;
  challengeEnabled: boolean;
  challengeDifficulty: string;
  challengeQuestionCount: number;
  /** `math` | `nfc` */
  challengeType: string;
  createdAt: number;
  updatedAt: number;
  /** 0 when not snoozed. */
  snoozedUntil: number;
  /** Epoch millis the OS will fire this alarm; 0 when not scheduled. */
  nextTriggerAt: number;
  /** `NONE` | `EXACT` | `INEXACT` | `FAILED` | `RINGING` */
  scheduleState: string;
};

export type ScheduledAlarm = {
  alarmId: number;
  triggerAt: number;
  scheduleState: string;
  /** Whether a PendingIntent is actually registered with AlarmManager. */
  registered: boolean;
};

export type EngineStatus = {
  exactAlarmPermitted: boolean;
  notificationsPermitted: boolean;
  fullScreenIntentPermitted: boolean;
  sdkInt: number;
  versionName: string;
  versionCode: number;
  scheduledCount: number;
  failedCount: number;
};

export type SessionInfo = {
  alarmIds: number[];
  labels: string[];
  startedAt: number;
  challengeRequired: boolean;
  challengeDifficulty: string;
  challengeQuestionCount: number;
  /** `math` | `nfc` */
  challengeType: string;
};

export type DeviceCalendarInfo = {
  id: number;
  name: string;
  /** e.g. the Google account, or a local account name used by apps such as RCB Calendar. */
  accountName: string;
  /** e.g. `com.google` for Google Calendar, `LOCAL` for on-device calendars. */
  accountType: string;
  color: string;
  selected: boolean;
};

export type CalendarSettings = {
  enabled: boolean;
  permitted: boolean;
  calendars: DeviceCalendarInfo[];
};

export type NfcStatus = {
  supported: boolean;
  enabled: boolean;
};

export type NfcTagInfo = {
  id: string;
  name: string;
};

export type Sound = {
  uri: string;
  name: string;
};

export type PickedTime = {
  hour: number;
  minute: number;
  cancelled: boolean;
};

export type TimerRecord = {
  id: number;
  label: string;
  durationMillis: number;
  /** Remaining time when paused or idle. */
  remainingMillis: number;
  /** SystemClock.elapsedRealtime() at which a running timer completes; 0 otherwise. */
  endElapsedMillis: number;
  /** `IDLE` | `RUNNING` | `PAUSED` | `FINISHED` */
  status: string;
  createdAt: number;
};

export type PomodoroConfig = {
  focusMinutes: number;
  shortBreakMinutes: number;
  longBreakMinutes: number;
  roundsBeforeLongBreak: number;
  autoStartNext: boolean;
};

export type PomodoroState = {
  config: PomodoroConfig;
  /** `IDLE` | `RUNNING` | `PAUSED` */
  status: string;
  /** `FOCUS` | `SHORT_BREAK` | `LONG_BREAK` */
  phase: string;
  /** Focus sessions finished in the current cycle. */
  completedFocus: number;
  /** Focus sessions finished since the last reset. */
  totalFocus: number;
  remainingMillis: number;
  /** SystemClock.elapsedRealtime() at which the running phase ends; 0 otherwise. */
  endElapsedMillis: number;
};

export type WorldClockRecord = {
  id: number;
  cityName: string;
  zoneId: string;
  displayOrder: number;
};

export type ZoneOffset = {
  zoneId: string;
  /** Offset from UTC in milliseconds at the requested instant (DST-aware). */
  offsetMillis: number;
  /** Next DST/offset transition after the instant, epoch millis; 0 if none. */
  nextTransitionAt: number;
};

export type Settings = {
  /** `midnight` | `pink` */
  theme: string;
  /** `default` | `purple` | `blue` | `teal` | `green` | `orange` | `rose` */
  accentColor: string;
  /** `system` (follow the device) | `12` | `24` */
  timeFormat: string;
  /** Resolved 24-hour flag (read-only; includes the device setting when timeFormat is `system`). */
  is24Hour: boolean;
  /** `en` | `si` | `ta` */
  language: string;
  defaultSoundUri: string;
  defaultSoundName: string;
  defaultSnoozeMinutes: number;
  defaultVibration: boolean;
  defaultGradualVolume: boolean;
  defaultChallengeEnabled: boolean;
  defaultChallengeDifficulty: string;
  defaultChallengeQuestionCount: number;
  /** Opt-in widget weather. When false the app makes no network requests. */
  weatherEnabled: boolean;
  weatherCity: string;
  /** Use coarse device location (refreshed while the app is open) instead of weatherCity. */
  weatherUseLocation: boolean;
  weatherLatitude: number;
  weatherLongitude: number;
  /** `c` | `f` */
  temperatureUnit: string;
  /** Bedtime reminder before the next alarm. */
  sleepReminderEnabled: boolean;
  /** 240-720 */
  sleepGoalMinutes: number;
  /** 0-120 */
  sleepReminderLeadMinutes: number;
  /** Widget text: `white` | `material` (Material You, Android 12+) | `accent` */
  widgetTextColor: string;
  /** 15 | 30 | 60 | 180 */
  weatherRefreshMinutes: number;
  /** `openmeteo` (default, no key) | `openweathermap` (user's own API key) */
  weatherProvider: string;
  openWeatherMapApiKey: string;
};

export type WeatherInfo = {
  /** Empty when the reading is for the device location. */
  city: string;
  temperatureCelsius: number;
  /** `CLEAR` | `PARTLY_CLOUDY` | `CLOUDY` | `FOG` | `DRIZZLE` | `RAIN` | `SHOWERS` | `SNOW` | `SNOW_SHOWERS` | `THUNDERSTORM` */
  condition: string;
  isDay: boolean;
  fetchedAt: number;
  /** Rounded coordinates sent to the provider. */
  latitude: number;
  longitude: number;
  /** Place the provider matched (OpenWeatherMap), else empty. */
  place: string;
};

export type EngineEvent = {
  /** `alarmsChanged` | `timersChanged` | `pomodoroChanged` | `sessionStarted` | `sessionEnded` | `settingsChanged` */
  type: string;
};

export interface Spec extends TurboModule {
  readonly onEngineEvent: CodegenTypes.EventEmitter<EngineEvent>;

  // Engine lifecycle and status
  initializeEngine(): Promise<EngineStatus>;
  getAlarmEngineStatus(): Promise<EngineStatus>;
  requestExactAlarmPermission(): void;
  requestFullScreenIntentPermission(): void;
  openNotificationSettings(): void;

  // Alarms
  getAlarms(): Promise<AlarmRecord[]>;
  saveAlarm(config: AlarmConfig): Promise<AlarmRecord>;
  setAlarmEnabled(alarmId: number, enabled: boolean): Promise<AlarmRecord>;
  deleteAlarm(alarmId: number): Promise<void>;
  cancelAllAlarms(): Promise<void>;
  getScheduledAlarms(): Promise<ScheduledAlarm[]>;
  getNextAlarm(): Promise<AlarmRecord | null>;

  // Ringing session
  getActiveSession(): Promise<SessionInfo | null>;
  snoozeSession(): Promise<number>;
  /**
   * method: `normal` (refused natively while a challenge is required), `challenge` (all questions
   * solved) or `emergency`. Resolves false if no session is ringing.
   */
  dismissSession(method: string): Promise<boolean>;

  // Launcher icon: `default` | `blue` | `teal` | `green` | `orange` | `rose` | `midnight` | `cream`
  getAppIcon(): Promise<string>;
  setAppIcon(variant: string): Promise<void>;

  // Widget calendar (optional, READ_CALENDAR)
  getCalendarSettings(): Promise<CalendarSettings>;
  /** Empty calendarIds means all visible calendars. */
  saveCalendarSettings(enabled: boolean, calendarIds: number[]): Promise<void>;

  // NFC dismiss challenge
  getNfcStatus(): Promise<NfcStatus>;
  getNfcTags(): Promise<NfcTagInfo[]>;
  /** Waits up to 30 s for a tag on the current screen; resolves null on timeout. */
  registerNfcTag(name: string): Promise<NfcTagInfo | null>;
  removeNfcTag(id: string): Promise<void>;
  listenForDismissTag(): void;
  stopNfcListening(): void;
  openNfcSettings(): void;

  // Sounds and pickers
  getBuiltInSounds(): Promise<Sound[]>;
  pickCustomSound(): Promise<Sound | null>;
  previewSound(uri: string): void;
  stopPreview(): void;
  showTimePicker(
    hour: number,
    minute: number,
    is24Hour: boolean,
  ): Promise<PickedTime>;

  // Timers
  getTimers(): Promise<TimerRecord[]>;
  createTimer(
    label: string,
    durationMillis: number,
    start: boolean,
  ): Promise<TimerRecord>;
  startTimer(timerId: number): Promise<TimerRecord>;
  pauseTimer(timerId: number): Promise<TimerRecord>;
  resetTimer(timerId: number): Promise<TimerRecord>;
  addTimerTime(timerId: number, millis: number): Promise<TimerRecord>;
  deleteTimer(timerId: number): Promise<void>;

  // Pomodoro (native, survives the app being closed)
  getPomodoro(): Promise<PomodoroState>;
  startPomodoro(): Promise<PomodoroState>;
  pausePomodoro(): Promise<PomodoroState>;
  skipPomodoroPhase(): Promise<PomodoroState>;
  resetPomodoro(): Promise<PomodoroState>;
  savePomodoroConfig(config: PomodoroConfig): Promise<PomodoroState>;

  // Clocks (synchronous, monotonic)
  elapsedRealtime(): number;
  getBootCount(): number;

  // World clock
  getWorldClocks(): Promise<WorldClockRecord[]>;
  addWorldClock(cityName: string, zoneId: string): Promise<WorldClockRecord>;
  removeWorldClock(clockId: number): Promise<void>;
  /** Pass `local` for the device's current zone. */
  getZoneOffsets(zoneIds: string[], atMillis: number): Promise<ZoneOffset[]>;

  // Settings and small state
  getSettings(): Promise<Settings>;
  saveSettings(settings: Settings): Promise<Settings>;
  getValue(key: string): Promise<string | null>;

  // Opt-in weather (Open-Meteo), shown on the home-screen widget
  getWeather(): Promise<WeatherInfo | null>;
  /** Refreshes the coarse location when "use my location" is on; resolves whether a position is known. */
  updateLocation(): Promise<boolean>;
  /** Fetches now. Resolves null when weather is disabled or the request fails. */
  refreshWeather(): Promise<WeatherInfo | null>;
  setValue(key: string, value: string): Promise<void>;
  removeValue(key: string): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RcbWake');
