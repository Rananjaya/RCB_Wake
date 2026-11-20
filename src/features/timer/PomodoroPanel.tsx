import React, { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { Button, Card, Divider, ToggleRow } from '../../components/ui';
import { useT, type StringKey } from '../../i18n';
import { engine, type PomodoroConfig } from '../../services/engine';
import { usePomodoroStore } from '../../storage/pomodoroStore';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';
import { formatCountdown } from '../../utils/time';
import { displayRemaining } from './timerEntry';

const PHASE_KEYS: Record<string, StringKey> = {
  FOCUS: 'pomodoroFocus',
  SHORT_BREAK: 'pomodoroShortBreak',
  LONG_BREAK: 'pomodoroLongBreak',
};

const RING_SIZE = 240;
const RING_STROKE = 10;

/** Pomodoro focus/break cycle. Phase changes run natively, so this view only displays state. */
export function PomodoroPanel() {
  const theme = useTheme();
  const t = useT();
  const { state, load, start, pause, skip, reset, saveConfig } =
    usePomodoroStore();
  const [now, setNow] = useState(() => engine.elapsedRealtime());
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    load();
  }, [load]);

  const running = state?.status === 'RUNNING';
  useEffect(() => {
    if (!running) return;
    const id = setInterval(() => setNow(engine.elapsedRealtime()), 250);
    return () => clearInterval(id);
  }, [running]);

  if (!state) return null;
  const c = state.config;
  const remaining = displayRemaining(
    state,
    running ? now : engine.elapsedRealtime(),
  );
  const phaseMinutes =
    state.phase === 'FOCUS'
      ? c.focusMinutes
      : state.phase === 'SHORT_BREAK'
      ? c.shortBreakMinutes
      : c.longBreakMinutes;
  const fraction = Math.min(
    1,
    Math.max(0, remaining / (phaseMinutes * 60_000)),
  );
  const r = (RING_SIZE - RING_STROKE) / 2;
  const circumference = 2 * Math.PI * r;
  const accent =
    state.phase === 'FOCUS' ? theme.colors.primary : theme.colors.success;

  const stepper = (
    key: keyof PomodoroConfig,
    label: StringKey,
    min: number,
    max: number,
  ) => {
    const value = c[key] as number;
    const set = (v: number) =>
      saveConfig({ ...c, [key]: Math.min(max, Math.max(min, v)) });
    return (
      <View style={styles.stepRow}>
        <Text style={[styles.stepLabel, { color: theme.colors.text }]}>
          {t(label)}
        </Text>
        <Pressable
          onPress={() => set(value - 1)}
          accessibilityRole="button"
          accessibilityLabel={`${t(label)} −`}
          style={[
            styles.stepButton,
            { backgroundColor: theme.colors.elevated },
          ]}
        >
          <Text style={[styles.stepSymbol, { color: theme.colors.text }]}>
            −
          </Text>
        </Pressable>
        <Text style={[styles.stepValue, { color: theme.colors.text }]}>
          {value}
        </Text>
        <Pressable
          onPress={() => set(value + 1)}
          accessibilityRole="button"
          accessibilityLabel={`${t(label)} +`}
          style={[
            styles.stepButton,
            { backgroundColor: theme.colors.elevated },
          ]}
        >
          <Text style={[styles.stepSymbol, { color: theme.colors.text }]}>
            +
          </Text>
        </Pressable>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.ringBox}>
        <Svg width={RING_SIZE} height={RING_SIZE}>
          <Circle
            cx={RING_SIZE / 2}
            cy={RING_SIZE / 2}
            r={r}
            stroke={theme.colors.elevated}
            strokeWidth={RING_STROKE}
            fill="none"
          />
          <Circle
            cx={RING_SIZE / 2}
            cy={RING_SIZE / 2}
            r={r}
            stroke={accent}
            strokeWidth={RING_STROKE}
            strokeLinecap="round"
            fill="none"
            strokeDasharray={`${circumference} ${circumference}`}
            strokeDashoffset={circumference * (1 - fraction)}
            transform={`rotate(-90 ${RING_SIZE / 2} ${RING_SIZE / 2})`}
          />
        </Svg>
        <View style={styles.ringText}>
          <Text style={[styles.phase, { color: accent }]}>
            {t(PHASE_KEYS[state.phase])}
          </Text>
          <Text
            style={[styles.countdown, { color: theme.colors.text }]}
            accessibilityRole="timer"
          >
            {formatCountdown(remaining)}
          </Text>
          <Text style={[styles.round, { color: theme.colors.textSecondary }]}>
            {t('pomodoroRound', {
              n: Math.min(state.completedFocus + 1, c.roundsBeforeLongBreak),
              total: c.roundsBeforeLongBreak,
            })}
          </Text>
        </View>
      </View>

      <View style={styles.dots}>
        {Array.from({ length: c.roundsBeforeLongBreak }, (_, i) => (
          <View
            key={i}
            style={[
              styles.dot,
              {
                backgroundColor:
                  i < state.completedFocus
                    ? theme.colors.primary
                    : theme.colors.divider,
              },
            ]}
          />
        ))}
      </View>
      <Text style={[styles.total, { color: theme.colors.textSecondary }]}>
        {t('pomodoroCompleted', { n: state.totalFocus })}
      </Text>

      <View style={styles.actions}>
        <Button
          label={t('reset')}
          icon="reset"
          variant="secondary"
          onPress={reset}
          style={styles.action}
        />
        {running ? (
          <Button
            testID="pomodoro-pause"
            label={t('pause')}
            icon="pause"
            onPress={pause}
            style={styles.action}
          />
        ) : (
          <Button
            testID="pomodoro-start"
            label={state.status === 'PAUSED' ? t('resume') : t('start')}
            icon="play"
            onPress={start}
            style={styles.action}
          />
        )}
        <Button
          label={t('pomodoroSkip')}
          variant="secondary"
          onPress={skip}
          style={styles.action}
        />
      </View>

      <Card style={styles.settings}>
        <Pressable
          onPress={() => setEditing(e => !e)}
          accessibilityRole="button"
          style={styles.settingsHeader}
        >
          <Text style={[styles.settingsTitle, { color: theme.colors.text }]}>
            {t('pomodoroSettings')}
          </Text>
          <Text
            style={[
              styles.settingsSummary,
              { color: theme.colors.textSecondary },
            ]}
          >
            {`${c.focusMinutes} / ${c.shortBreakMinutes} / ${c.longBreakMinutes}`}
          </Text>
        </Pressable>
        {editing && (
          <>
            <Divider />
            {stepper('focusMinutes', 'pomodoroFocusMinutes', 1, 180)}
            {stepper('shortBreakMinutes', 'pomodoroShortMinutes', 1, 60)}
            {stepper('longBreakMinutes', 'pomodoroLongMinutes', 1, 120)}
            {stepper('roundsBeforeLongBreak', 'pomodoroRounds', 1, 12)}
            <ToggleRow
              label={t('pomodoroAutoStart')}
              value={c.autoStartNext}
              onValueChange={v => saveConfig({ ...c, autoStartNext: v })}
            />
          </>
        )}
      </Card>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { alignItems: 'center', gap: spacing.md },
  ringBox: {
    width: RING_SIZE,
    height: RING_SIZE,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.md,
  },
  ringText: { position: 'absolute', alignItems: 'center' },
  phase: { fontSize: 16, fontWeight: '700', letterSpacing: 0.5 },
  countdown: { fontSize: 52, fontWeight: '200', fontVariant: ['tabular-nums'] },
  round: { fontSize: 14 },
  dots: { flexDirection: 'row', gap: 8 },
  dot: { width: 10, height: 10, borderRadius: 5 },
  total: { fontSize: 13 },
  actions: { flexDirection: 'row', gap: spacing.sm, alignSelf: 'stretch' },
  action: { flex: 1, paddingHorizontal: spacing.sm },
  settings: { alignSelf: 'stretch', paddingVertical: spacing.xs },
  settingsHeader: { flexDirection: 'row', alignItems: 'center', minHeight: 52 },
  settingsTitle: { flex: 1, fontSize: 16, fontWeight: '600' },
  settingsSummary: { fontSize: 14 },
  stepRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    minHeight: 52,
  },
  stepLabel: { flex: 1, fontSize: 15 },
  stepButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepSymbol: { fontSize: 20, fontWeight: '500' },
  stepValue: {
    fontSize: 17,
    fontWeight: '600',
    minWidth: 32,
    textAlign: 'center',
  },
});
