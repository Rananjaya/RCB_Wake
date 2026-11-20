import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import {
  Card,
  Chip,
  Divider,
  SectionTitle,
  ToggleRow,
} from '../../components/ui';
import { useT } from '../../i18n';
import { useAlarmStore } from '../../storage/alarmStore';
import { useSettingsStore } from '../../storage/settingsStore';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';
import { formatClockText } from '../../utils/time';

export const SLEEP_LEAD_OPTIONS = [0, 15, 30, 60];
const GOAL_STEP = 30;
const GOAL_MIN = 240;
const GOAL_MAX = 720;

export function formatGoal(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m ? `${h} h ${m} min` : `${h} h`;
}

/** When the next bedtime reminder fires, mirroring the native SleepMath rule. */
export function nextReminderAt(
  nextAlarmAt: number,
  goalMinutes: number,
  leadMinutes: number,
  now: number,
): number | null {
  if (!nextAlarmAt || nextAlarmAt <= now) return null;
  const at = nextAlarmAt - (goalMinutes + leadMinutes) * 60_000;
  return at > now ? at : null;
}

export function SleepReminderSettings() {
  const theme = useTheme();
  const t = useT();
  const settings = useSettingsStore(s => s.settings);
  const update = useSettingsStore(s => s.update);
  const alarms = useAlarmStore(s => s.alarms);
  const set = (patch: Parameters<typeof update>[0]) =>
    update(patch).catch(() => undefined);
  const is24 = settings.timeFormat === '24';

  const next = alarms
    .filter(a => a.nextTriggerAt > 0 && a.scheduleState !== 'FAILED')
    .sort((a, b) => a.nextTriggerAt - b.nextTriggerAt)[0];
  const remindAt = next
    ? nextReminderAt(
        next.nextTriggerAt,
        settings.sleepGoalMinutes,
        settings.sleepReminderLeadMinutes,
        Date.now(),
      )
    : null;
  const clock = (ms: number) => {
    const d = new Date(ms);
    return formatClockText(d.getHours(), d.getMinutes(), is24, t);
  };

  const goalButton = (delta: number, symbol: string) => (
    <Pressable
      onPress={() =>
        set({
          sleepGoalMinutes: Math.min(
            GOAL_MAX,
            Math.max(GOAL_MIN, settings.sleepGoalMinutes + delta),
          ),
        })
      }
      accessibilityRole="button"
      accessibilityLabel={`${t('sleepGoal')} ${symbol}`}
      style={[styles.stepButton, { backgroundColor: theme.colors.elevated }]}
    >
      <Text style={[styles.stepSymbol, { color: theme.colors.text }]}>
        {symbol}
      </Text>
    </Pressable>
  );

  return (
    <>
      <SectionTitle>{t('sleepReminder')}</SectionTitle>
      <Card style={styles.card}>
        <ToggleRow
          label={t('sleepReminderToggle')}
          hint={t('sleepReminderHint')}
          value={settings.sleepReminderEnabled}
          onValueChange={v => set({ sleepReminderEnabled: v })}
        />
        {settings.sleepReminderEnabled && (
          <>
            <Divider />
            <View style={styles.row}>
              <Text style={[styles.label, { color: theme.colors.text }]}>
                {t('sleepGoal')}
              </Text>
              {goalButton(-GOAL_STEP, '−')}
              <Text style={[styles.value, { color: theme.colors.text }]}>
                {formatGoal(settings.sleepGoalMinutes)}
              </Text>
              {goalButton(GOAL_STEP, '+')}
            </View>
            <Divider />
            <Text
              style={[styles.subLabel, { color: theme.colors.textSecondary }]}
            >
              {t('sleepReminderLead')}
            </Text>
            <View style={styles.wrap}>
              {SLEEP_LEAD_OPTIONS.map(m => (
                <Chip
                  key={m}
                  label={m === 0 ? t('atBedtime') : t('snoozeMinutes', { m })}
                  selected={settings.sleepReminderLeadMinutes === m}
                  onPress={() => set({ sleepReminderLeadMinutes: m })}
                />
              ))}
            </View>
            <Text
              style={[styles.preview, { color: theme.colors.textSecondary }]}
            >
              {next && remindAt
                ? t('sleepReminderPreview', {
                    time: clock(remindAt),
                    alarm: clock(next.nextTriggerAt),
                  })
                : t('sleepReminderNone')}
            </Text>
          </>
        )}
      </Card>
    </>
  );
}

const styles = StyleSheet.create({
  card: { paddingVertical: spacing.xs },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    minHeight: 56,
  },
  label: { flex: 1, fontSize: 16, fontWeight: '500' },
  value: { fontSize: 16, fontWeight: '600', minWidth: 72, textAlign: 'center' },
  stepButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepSymbol: { fontSize: 20, fontWeight: '500' },
  subLabel: { fontSize: 14, marginTop: spacing.md, marginBottom: spacing.sm },
  wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  preview: { fontSize: 14, paddingVertical: spacing.md },
});
