import NativeRcbWake from '../src/native/NativeRcbWake';
import {
  newAlarmDraft,
  draftFromRecord,
  clampQuestions,
} from '../src/features/alarms/alarmDraft';
import { useAlarmStore } from '../src/storage/alarmStore';
import {
  defaultSettings,
  useSettingsStore,
} from '../src/storage/settingsStore';
import { useTimerStore } from '../src/storage/timerStore';

const fake = NativeRcbWake as unknown as {
  __reset: () => void;
  __advance: (ms: number) => void;
};

beforeEach(() => {
  fake.__reset();
  useAlarmStore.setState({
    alarms: [],
    loaded: false,
    session: null,
    status: null,
  });
  useTimerStore.setState({ timers: [] });
  useSettingsStore.setState({ settings: defaultSettings, loaded: false });
});

describe('alarm management', () => {
  it('creates, edits, toggles and deletes an alarm through the engine', async () => {
    const draft = {
      ...newAlarmDraft(defaultSettings, new Date(2026, 10, 20, 6, 15)),
      label: 'Gym',
    };
    expect(draft.hour).toBe(7);
    const created = await useAlarmStore.getState().save(draft);
    expect(useAlarmStore.getState().alarms).toHaveLength(1);
    expect(created.scheduleState).toBe('EXACT');

    await useAlarmStore
      .getState()
      .save({ ...draftFromRecord(created), hour: 8 });
    expect(useAlarmStore.getState().alarms[0].hour).toBe(8);

    const off = await useAlarmStore.getState().setEnabled(created.id, false);
    expect(off.scheduleState).toBe('NONE');

    await useAlarmStore.getState().remove(created.id);
    expect(useAlarmStore.getState().alarms).toHaveLength(0);
  });

  it('applies the user defaults to new alarms', () => {
    const draft = newAlarmDraft({
      ...defaultSettings,
      defaultSnoozeMinutes: 20,
      defaultChallengeEnabled: true,
    });
    expect(draft.snoozeMinutes).toBe(20);
    expect(draft.challengeEnabled).toBe(true);
    expect(clampQuestions(42)).toBe(10);
    expect(clampQuestions(0)).toBe(1);
  });
});

describe('timers', () => {
  it('starts, pauses, resumes and resets multiple timers', async () => {
    const store = useTimerStore.getState();
    await store.create('Tea', 180_000);
    await store.create('Eggs', 420_000);
    let [tea, eggs] = useTimerStore.getState().timers;
    expect(tea.status).toBe('RUNNING');
    expect(eggs.status).toBe('RUNNING');

    fake.__advance(60_000);
    await store.pause(tea.id);
    tea = useTimerStore.getState().timers[0];
    expect(tea.status).toBe('PAUSED');
    expect(tea.remainingMillis).toBe(120_000);

    await store.start(tea.id);
    await store.reset(eggs.id);
    [tea, eggs] = useTimerStore.getState().timers;
    expect(tea.status).toBe('RUNNING');
    expect(eggs).toMatchObject({ status: 'IDLE', remainingMillis: 420_000 });

    await store.remove(tea.id);
    expect(useTimerStore.getState().timers).toHaveLength(1);
  });
});

describe('settings', () => {
  it('persists theme changes through the engine', async () => {
    await useSettingsStore.getState().update({ theme: 'pink' });
    expect(useSettingsStore.getState().settings.theme).toBe('pink');
    await useSettingsStore.getState().load();
    expect(useSettingsStore.getState().settings.theme).toBe('pink');
  });
});
