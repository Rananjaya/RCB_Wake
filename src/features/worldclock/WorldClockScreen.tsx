import React, { useCallback, useEffect } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import Animated, {
  FadeIn,
  FadeOut,
  LinearTransition,
} from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Icon } from '../../components/Icon';
import { Toolbar } from '../../components/Toolbar';
import { useNow } from '../../hooks/useNow';
import { useT, type Translate } from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import type { WorldClockRecord } from '../../services/engine';
import { useSettingsStore } from '../../storage/settingsStore';
import { LOCAL_ZONE, useWorldClockStore } from '../../storage/worldClockStore';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/themes';
import { DAY_KEYS, formatClock } from '../../utils/time';
import { offsetDifference, zoneTime } from './worldTime';

type Nav = NativeStackNavigationProp<RootStackParamList>;

function differenceLabel(
  zoneOffset: number,
  localOffset: number,
  t: Translate,
): string {
  const { sign, hours, minutes } = offsetDifference(zoneOffset, localOffset);
  if (sign === 0) return t('sameTime');
  const diff = minutes
    ? t('durationHoursMinutes', { h: hours, m: minutes })
    : `${hours} h`;
  return sign > 0 ? t('aheadOf', { diff }) : t('behind', { diff });
}

export function WorldClockScreen() {
  const navigation = useNavigation<Nav>();
  const theme = useTheme();
  const t = useT();
  const is24 = useSettingsStore(s => s.settings.timeFormat === '24');
  const { clocks, offsets, load, refreshOffsets } = useWorldClockStore();
  const now = useNow(1000);

  useFocusEffect(
    useCallback(() => {
      load().then(refreshOffsets);
    }, [load, refreshOffsets]),
  );

  // Re-fetch offsets each minute and as soon as any zone passes a DST transition.
  const minute = Math.floor(now / 60_000);
  const transitionPassed = Object.values(offsets).some(
    o => o.nextTransitionAt > 0 && o.nextTransitionAt <= now,
  );
  useEffect(() => {
    refreshOffsets();
  }, [minute, transitionPassed, clocks.length, refreshOffsets]);

  const local = offsets[LOCAL_ZONE];
  const localOffset =
    local?.offsetMillis ?? -new Date(now).getTimezoneOffset() * 60_000;
  const localTime = zoneTime(now, localOffset, localOffset);
  const localParts = formatClock(localTime.hour, localTime.minute, is24);
  const localDate = new Date(now + localOffset);

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <Toolbar
        title={t('tabWorldClock')}
        onSettings={() => navigation.navigate('Settings')}
      />
      <FlatList
        data={clocks}
        keyExtractor={c => String(c.id)}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <View style={styles.local}>
            <View style={styles.localTimeRow}>
              <Text style={[styles.localTime, { color: theme.colors.text }]}>
                {localParts.time}
              </Text>
              {localParts.period && (
                <Text
                  style={[
                    styles.localPeriod,
                    { color: theme.colors.secondary },
                  ]}
                >
                  {t(localParts.period)}
                </Text>
              )}
            </View>
            <Text
              style={[styles.localDate, { color: theme.colors.textSecondary }]}
            >
              {t('localTime')} · {t(DAY_KEYS[(localDate.getUTCDay() + 6) % 7])}{' '}
              {localDate.getUTCDate()}
            </Text>
          </View>
        }
        ListEmptyComponent={
          <Text style={[styles.empty, { color: theme.colors.textSecondary }]}>
            {t('noCities')}
          </Text>
        }
        renderItem={({ item }) => (
          <CityRow
            clock={item}
            now={now}
            localOffset={localOffset}
            offset={offsets[item.zoneId]?.offsetMillis}
          />
        )}
      />
      <Pressable
        testID="add-city"
        onPress={() => navigation.navigate('CityPicker')}
        accessibilityRole="button"
        accessibilityLabel={t('addCity')}
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

type RowProps = {
  clock: WorldClockRecord;
  now: number;
  localOffset: number;
  offset: number | undefined;
};

function CityRow({ clock, now, localOffset, offset }: RowProps) {
  const theme = useTheme();
  const t = useT();
  const is24 = useSettingsStore(s => s.settings.timeFormat === '24');
  const remove = useWorldClockStore(s => s.remove);
  if (offset === undefined) return null;
  const zt = zoneTime(now, offset, localOffset);
  const { time, period } = formatClock(zt.hour, zt.minute, is24);
  const day =
    zt.dayOffset === 0
      ? t('today')
      : zt.dayOffset > 0
      ? t('tomorrow')
      : t('yesterday');
  return (
    <Animated.View
      entering={FadeIn}
      exiting={FadeOut}
      layout={LinearTransition}
      style={[styles.card, { backgroundColor: theme.colors.surface }]}
    >
      <Icon
        name={zt.isDay ? 'sun' : 'moon'}
        color={zt.isDay ? theme.colors.secondary : theme.colors.textSecondary}
      />
      <View
        style={styles.cityText}
        accessible
        accessibilityLabel={`${clock.cityName}, ${time} ${
          period ? t(period) : ''
        }, ${day}, ${zt.isDay ? t('daytime') : t('nighttime')}`}
      >
        <Text
          style={[styles.city, { color: theme.colors.text }]}
          numberOfLines={1}
        >
          {clock.cityName}
        </Text>
        <Text style={[styles.diff, { color: theme.colors.textSecondary }]}>
          {day} · {differenceLabel(offset, localOffset, t)}
        </Text>
      </View>
      <View style={styles.timeRow}>
        <Text style={[styles.time, { color: theme.colors.text }]}>{time}</Text>
        {period && (
          <Text style={[styles.period, { color: theme.colors.textSecondary }]}>
            {t(period)}
          </Text>
        )}
      </View>
      <Pressable
        onPress={() => remove(clock.id)}
        accessibilityRole="button"
        accessibilityLabel={t('removeCity', { city: clock.cityName })}
        hitSlop={10}
      >
        <Icon name="close" color={theme.colors.textSecondary} size={18} />
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  list: { paddingHorizontal: spacing.lg, paddingBottom: 120 },
  local: { alignItems: 'center', paddingVertical: spacing.xl },
  localTimeRow: { flexDirection: 'row', alignItems: 'baseline', gap: 8 },
  localTime: { fontSize: 72, fontWeight: '200' },
  localPeriod: { fontSize: 22, fontWeight: '500' },
  localDate: { fontSize: 15, marginTop: spacing.xs },
  empty: {
    textAlign: 'center',
    fontSize: 15,
    marginTop: spacing.xl,
    paddingHorizontal: spacing.xl,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    borderRadius: radius.lg,
    paddingVertical: spacing.lg,
    paddingHorizontal: spacing.lg,
    marginBottom: spacing.md,
  },
  cityText: { flex: 1 },
  city: { fontSize: 18, fontWeight: '500' },
  diff: { fontSize: 13, marginTop: 2 },
  timeRow: { flexDirection: 'row', alignItems: 'baseline', gap: 4 },
  time: { fontSize: 30, fontWeight: '300' },
  period: { fontSize: 13 },
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
});
