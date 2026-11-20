export type RootStackParamList = {
  Tabs: {screen?: keyof TabParamList} | undefined;
  AlarmEditor: {alarmId?: number};
  Settings: undefined;
  About: undefined;
  CityPicker: undefined;
  Challenge: undefined;
};

export type TabParamList = {
  Alarms: undefined;
  Timers: undefined;
  Stopwatch: undefined;
  WorldClock: undefined;
};
