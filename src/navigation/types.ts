export type RootStackParamList = {
  Tabs: { screen?: keyof TabParamList } | undefined;
  AlarmEditor: { alarmId?: number };
  Settings: undefined;
  About: undefined;
  /** `weather` picks the widget weather city instead of adding a world clock. */
  CityPicker: { mode?: 'worldclock' | 'weather' } | undefined;
  Challenge: undefined;
  Tutorial: undefined;
};

export type TabParamList = {
  Alarms: undefined;
  Timers: undefined;
  Stopwatch: undefined;
  WorldClock: undefined;
};
