import React, { useState } from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import Animated, {
  FadeIn,
  FadeOut,
  LinearTransition,
} from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Icon } from '../../components/Icon';
import { Toolbar } from '../../components/Toolbar';
import { Button, Chip } from '../../components/ui';
import { useElapsedNow } from '../../hooks/useElapsedNow';
import { useT } from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import type { TimerRecord } from '../../services/engine';
import { useTimerStore } from '../../storage/timerStore';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/themes';
import { formatCountdown, pad2 } from '../../utils/time';
import {
  bufferToMillis,
  bufferToParts,
  displayRemaining,
  popDigit,
  PRESETS_MINUTES,
  pushDigit,
} from './timerEntry';
import { PomodoroPanel } from './PomodoroPanel';

export function TimerScreen() {
  const navigation =
    useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const theme = useTheme();
  const t = useT();
  const timers = useTimerStore(s => s.timers);
  const create = useTimerStore(s => s.create);
  const [entryOpen, setEntryOpen] = useState(false);
  const [mode, setMode] = useState<'timers' | 'pomodoro'>('timers');
  const showEntry = entryOpen || timers.length === 0;
  const nowElapsed = useElapsedNow(timers.some(x => x.status === 'RUNNING'));

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <Toolbar
        title={t('tabTimer')}
        onSettings={() => navigation.navigate('Settings')}
      />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.modes}>
          <Chip
            label={t('timersMode')}
            selected={mode === 'timers'}
            onPress={() => setMode('timers')}
          />
          <Chip
            label={t('pomodoroMode')}
            selected={mode === 'pomodoro'}
            onPress={() => setMode('pomodoro')}
          />
        </View>
        {mode === 'pomodoro' ? (
          <PomodoroPanel />
        ) : (
          <>
            {timers.map(timer => (
              <TimerCard key={timer.id} timer={timer} nowElapsed={nowElapsed} />
            ))}
            {showEntry ? (
              <TimerEntry
                onStart={async (label, millis) => {
                  await create(label, millis);
                  setEntryOpen(false);
                }}
                onCancel={
                  timers.length > 0 ? () => setEntryOpen(false) : undefined
                }
              />
            ) : (
              <Button
                label={t('addTimer')}
                icon="plus"
                variant="secondary"
                onPress={() => setEntryOpen(true)}
              />
            )}
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

function TimerCard({
  timer,
  nowElapsed,
}: {
  timer: TimerRecord;
  nowElapsed: number;
}) {
  const theme = useTheme();
  const t = useT();
  const { start, pause, reset, addMinute, remove } = useTimerStore.getState();
  const remaining = displayRemaining(timer, nowElapsed);
  const fraction =
    timer.durationMillis > 0 ? remaining / timer.durationMillis : 0;
  const running = timer.status === 'RUNNING';
  const finished = timer.status === 'FINISHED' || (running && remaining === 0);

  return (
    <Animated.View
      entering={FadeIn}
      exiting={FadeOut}
      layout={LinearTransition}
      style={[styles.card, { backgroundColor: theme.colors.surface }]}
    >
      <View style={styles.cardHeader}>
        <Text
          style={[styles.cardLabel, { color: theme.colors.textSecondary }]}
          numberOfLines={1}
        >
          {timer.label || formatCountdown(timer.durationMillis)}
        </Text>
        <Pressable
          onPress={() => remove(timer.id)}
          accessibilityRole="button"
          accessibilityLabel={t('delete')}
          hitSlop={10}
        >
          <Icon name="close" color={theme.colors.textSecondary} size={20} />
        </Pressable>
      </View>
      <Text
        style={[
          styles.countdown,
          { color: finished ? theme.colors.primary : theme.colors.text },
        ]}
        accessibilityLabel={
          finished ? t('timerFinished') : formatCountdown(remaining)
        }
      >
        {finished ? t('timerFinished') : formatCountdown(remaining)}
      </Text>
      {timer.status === 'PAUSED' && (
        <Text style={[styles.paused, { color: theme.colors.secondary }]}>
          {t('timerPaused')}
        </Text>
      )}
      <View style={[styles.track, { backgroundColor: theme.colors.elevated }]}>
        <View
          style={[
            styles.fill,
            {
              backgroundColor: theme.colors.primary,
              width: `${Math.round(fraction * 100)}%`,
            },
          ]}
        />
      </View>
      <View style={styles.actions}>
        <Button
          label={t('reset')}
          variant="secondary"
          icon="reset"
          onPress={() => reset(timer.id)}
          style={styles.action}
        />
        <Button
          label={t('addMinute')}
          variant="secondary"
          onPress={() => addMinute(timer.id)}
          style={styles.action}
        />
        {running && !finished ? (
          <Button
            label={t('pause')}
            icon="pause"
            onPress={() => pause(timer.id)}
            style={styles.action}
          />
        ) : (
          <Button
            label={timer.status === 'PAUSED' ? t('resume') : t('start')}
            icon="play"
            onPress={() => start(timer.id)}
            style={styles.action}
          />
        )}
      </View>
    </Animated.View>
  );
}

function TimerEntry({
  onStart,
  onCancel,
}: {
  onStart: (label: string, millis: number) => void;
  onCancel?: () => void;
}) {
  const theme = useTheme();
  const t = useT();
  const [buffer, setBuffer] = useState('');
  const [label, setLabel] = useState('');
  const { h, m, s } = bufferToParts(buffer);
  const millis = bufferToMillis(buffer);

  const digit = (d: string) => (
    <Pressable
      key={d}
      testID={`key-${d}`}
      onPress={() => setBuffer(b => pushDigit(b, d))}
      accessibilityRole="button"
      style={({ pressed }) => [
        styles.key,
        {
          backgroundColor: pressed
            ? theme.colors.selected
            : theme.colors.elevated,
        },
      ]}
    >
      <Text style={[styles.keyText, { color: theme.colors.text }]}>{d}</Text>
    </Pressable>
  );

  const part = (value: number, unit: string) => (
    <View style={styles.part}>
      <Text
        style={[
          styles.partValue,
          { color: buffer ? theme.colors.text : theme.colors.textSecondary },
        ]}
      >
        {pad2(value)}
      </Text>
      <Text style={[styles.partUnit, { color: theme.colors.textSecondary }]}>
        {unit}
      </Text>
    </View>
  );

  return (
    <View style={[styles.card, { backgroundColor: theme.colors.surface }]}>
      <View style={styles.cardHeader}>
        <Text style={[styles.cardTitle, { color: theme.colors.text }]}>
          {t('newTimer')}
        </Text>
        {onCancel && (
          <Pressable
            onPress={onCancel}
            accessibilityRole="button"
            accessibilityLabel={t('cancel')}
            hitSlop={10}
          >
            <Icon name="close" color={theme.colors.textSecondary} size={20} />
          </Pressable>
        )}
      </View>
      <View style={styles.display} accessibilityLabel={formatCountdown(millis)}>
        {part(h, t('hoursUnit'))}
        {part(m, t('minutesUnit'))}
        {part(s, t('secondsUnit'))}
      </View>
      <View style={styles.keypad}>
        {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map(digit)}
        <Pressable
          onPress={() => setBuffer(b => pushDigit(pushDigit(b, '0'), '0'))}
          accessibilityRole="button"
          style={[styles.key, { backgroundColor: theme.colors.elevated }]}
        >
          <Text style={[styles.keyText, { color: theme.colors.text }]}>00</Text>
        </Pressable>
        {digit('0')}
        <Pressable
          onPress={() => setBuffer(popDigit)}
          accessibilityRole="button"
          accessibilityLabel={t('backspace')}
          style={[styles.key, { backgroundColor: theme.colors.elevated }]}
        >
          <Icon name="backspace" color={theme.colors.text} />
        </Pressable>
      </View>
      <Text
        style={[styles.presetsTitle, { color: theme.colors.textSecondary }]}
      >
        {t('presets')}
      </Text>
      <View style={styles.presets}>
        {PRESETS_MINUTES.map(p => (
          <Chip
            key={p}
            label={t('snoozeMinutes', { m: p })}
            selected={false}
            onPress={() => onStart(label, p * 60_000)}
          />
        ))}
      </View>
      <TextInput
        value={label}
        onChangeText={setLabel}
        placeholder={t('timerLabelPlaceholder')}
        placeholderTextColor={theme.colors.textSecondary}
        maxLength={40}
        accessibilityLabel={t('label')}
        style={[
          styles.input,
          { backgroundColor: theme.colors.elevated, color: theme.colors.text },
        ]}
      />
      <Button
        testID="start-timer"
        label={t('start')}
        icon="play"
        disabled={millis < 1000}
        onPress={() => onStart(label, millis)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  modes: { flexDirection: 'row', gap: 8, justifyContent: 'center' },
  content: {
    paddingHorizontal: spacing.lg,
    paddingBottom: 48,
    gap: spacing.md,
  },
  card: { borderRadius: radius.lg, padding: spacing.xl, gap: spacing.md },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  cardLabel: { fontSize: 15, flex: 1 },
  cardTitle: { fontSize: 18, fontWeight: '600' },
  countdown: { fontSize: 56, fontWeight: '200', fontVariant: ['tabular-nums'] },
  paused: { fontSize: 14, fontWeight: '600', marginTop: -8 },
  track: { height: 6, borderRadius: 3, overflow: 'hidden' },
  fill: { height: 6, borderRadius: 3 },
  actions: { flexDirection: 'row', gap: spacing.sm },
  action: { flex: 1, paddingHorizontal: spacing.sm },
  display: { flexDirection: 'row', justifyContent: 'center', gap: spacing.lg },
  part: { flexDirection: 'row', alignItems: 'baseline', gap: 2 },
  partValue: { fontSize: 48, fontWeight: '300', fontVariant: ['tabular-nums'] },
  partUnit: { fontSize: 16 },
  keypad: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    rowGap: spacing.sm,
  },
  key: {
    width: '31%',
    height: 56,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  keyText: { fontSize: 24, fontWeight: '500' },
  presetsTitle: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  presets: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  input: {
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 48,
    fontSize: 16,
  },
});
