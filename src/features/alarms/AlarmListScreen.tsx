import React from 'react';
import {
  FlatList,
  Pressable,
  StyleSheet,
  Switch,
  Text,
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
import { Floral } from '../../components/Floral';
import { Icon } from '../../components/Icon';
import { Toolbar } from '../../components/Toolbar';
import { useNow } from '../../hooks/useNow';
import { useT } from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import type { AlarmRecord } from '../../services/engine';
import { useAlarmStore } from '../../storage/alarmStore';
import { useSettingsStore } from '../../storage/settingsStore';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/themes';
import {
  formatClock,
  formatClockText,
  formatUntil,
  repeatSummary,
} from '../../utils/time';
import { PermissionBanners } from './PermissionBanners';
import { RingingBanner } from './RingingBanner';

type Nav = NativeStackNavigationProp<RootStackParamList>;

export function AlarmListScreen() {
  const navigation = useNavigation<Nav>();
  const theme = useTheme();
  const t = useT();
  const alarms = useAlarmStore(s => s.alarms);
  const loaded = useAlarmStore(s => s.loaded);
  const now = useNow(15_000);

  const next = alarms
    .filter(a => a.nextTriggerAt > 0 && a.scheduleState !== 'FAILED')
    .sort((a, b) => a.nextTriggerAt - b.nextTriggerAt)[0];

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <Toolbar
        title={t('tabAlarm')}
        onSettings={() => navigation.navigate('Settings')}
      />
      <FlatList
        data={alarms}
        keyExtractor={a => String(a.id)}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <>
            <RingingBanner />
            <PermissionBanners />
            <Text
              style={[styles.next, { color: theme.colors.textSecondary }]}
              accessibilityLiveRegion="polite"
            >
              {next
                ? t('nextAlarmIn', {
                    duration: formatUntil(now, next.nextTriggerAt, t),
                  })
                : t('noAlarmsScheduled')}
            </Text>
          </>
        }
        ListEmptyComponent={loaded ? <EmptyState /> : undefined}
        renderItem={({ item }) => (
          <AlarmCard
            alarm={item}
            onPress={() =>
              navigation.navigate('AlarmEditor', { alarmId: item.id })
            }
          />
        )}
      />
      <Pressable
        testID="add-alarm"
        onPress={() => navigation.navigate('AlarmEditor', {})}
        accessibilityRole="button"
        accessibilityLabel={t('addAlarm')}
        style={({ pressed }) => [
          styles.fab,
          {
            backgroundColor: theme.colors.primary,
            opacity: pressed ? 0.85 : 1,
          },
        ]}
      >
        <Icon name="plus" color={theme.colors.onPrimary} size={28} />
      </Pressable>
    </SafeAreaView>
  );
}

function EmptyState() {
  const theme = useTheme();
  const t = useT();
  return (
    <View style={styles.empty}>
      {theme.floral ? (
        <Floral size={72} />
      ) : (
        <Icon
          name="alarm"
          color={theme.colors.divider}
          size={72}
          strokeWidth={1.4}
        />
      )}
      <Text style={[styles.emptyTitle, { color: theme.colors.text }]}>
        {t('emptyAlarmsTitle')}
      </Text>
      <Text style={[styles.emptyBody, { color: theme.colors.textSecondary }]}>
        {t('emptyAlarmsBody')}
      </Text>
    </View>
  );
}

function AlarmCard({
  alarm,
  onPress,
}: {
  alarm: AlarmRecord;
  onPress: () => void;
}) {
  const theme = useTheme();
  const t = useT();
  const is24 = useSettingsStore(s => s.settings.is24Hour);
  const setEnabled = useAlarmStore(s => s.setEnabled);
  const session = useAlarmStore(s => s.session);
  const { time, period } = formatClock(alarm.hour, alarm.minute, is24);
  const active = alarm.enabled || alarm.snoozedUntil > 0;
  const ringing = session?.alarmIds.includes(alarm.id) ?? false;

  let status: { text: string; color: string } | null = null;
  if (ringing) status = { text: t('ringingNow'), color: theme.colors.primary };
  else if (alarm.snoozedUntil > 0) {
    const d = new Date(alarm.snoozedUntil);
    status = {
      text: t('snoozedUntil', {
        time: formatClockText(d.getHours(), d.getMinutes(), is24, t),
      }),
      color: theme.colors.secondary,
    };
  } else if (active && alarm.scheduleState === 'INEXACT')
    status = { text: t('scheduleInexact'), color: theme.colors.warning };
  else if (active && alarm.scheduleState === 'FAILED')
    status = { text: t('scheduleFailed'), color: theme.colors.danger };

  const details = [alarm.label, repeatSummary(alarm.repeatDays, t)]
    .filter(Boolean)
    .join(' · ');
  const textColor = active ? theme.colors.text : theme.colors.textSecondary;

  return (
    <Animated.View
      entering={FadeIn}
      exiting={FadeOut}
      layout={LinearTransition}
    >
      <Pressable
        onPress={onPress}
        accessibilityRole="button"
        accessibilityLabel={`${formatClockText(
          alarm.hour,
          alarm.minute,
          is24,
          t,
        )}, ${details}`}
        style={[styles.card, { backgroundColor: theme.colors.surface }]}
      >
        <View style={styles.cardText}>
          <View style={styles.timeRow}>
            <Text style={[styles.time, { color: textColor }]}>{time}</Text>
            {period && (
              <Text
                style={[styles.period, { color: theme.colors.textSecondary }]}
              >
                {t(period)}
              </Text>
            )}
          </View>
          <Text
            style={[styles.details, { color: theme.colors.textSecondary }]}
            numberOfLines={1}
          >
            {details}
          </Text>
          {status && (
            <Text style={[styles.status, { color: status.color }]}>
              {status.text}
            </Text>
          )}
        </View>
        <Switch
          value={active}
          onValueChange={value => {
            setEnabled(alarm.id, value).catch(() => undefined);
          }}
          accessibilityLabel={t('toggleAlarm', {
            state: active ? t('off') : t('on'),
          })}
          trackColor={{
            false: theme.colors.divider,
            true: theme.colors.selected,
          }}
          thumbColor={
            active ? theme.colors.primary : theme.colors.textSecondary
          }
        />
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  list: { paddingHorizontal: spacing.lg, paddingBottom: 120 },
  next: {
    fontSize: 15,
    marginBottom: spacing.lg,
    marginHorizontal: spacing.xs,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: radius.lg,
    paddingVertical: spacing.lg,
    paddingHorizontal: spacing.xl,
    marginBottom: spacing.md,
  },
  cardText: { flex: 1 },
  timeRow: { flexDirection: 'row', alignItems: 'baseline', gap: 6 },
  time: { fontSize: 44, fontWeight: '300', letterSpacing: -0.5 },
  period: { fontSize: 16, fontWeight: '500' },
  details: { fontSize: 14, marginTop: 2 },
  status: { fontSize: 13, marginTop: 6, fontWeight: '600' },
  fab: {
    position: 'absolute',
    right: spacing.xl,
    bottom: spacing.xl,
    width: 64,
    height: 64,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 6,
  },
  empty: { alignItems: 'center', paddingTop: 80, gap: spacing.md },
  emptyTitle: { fontSize: 20, fontWeight: '600' },
  emptyBody: { fontSize: 15 },
});
