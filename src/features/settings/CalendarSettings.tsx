import React, { useCallback, useEffect, useState } from 'react';
import {
  PermissionsAndroid,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Icon } from '../../components/Icon';
import {
  Button,
  Card,
  Divider,
  SectionTitle,
  ToggleRow,
} from '../../components/ui';
import { useT } from '../../i18n';
import {
  engine,
  type CalendarSettings as Model,
  type DeviceCalendarInfo,
} from '../../services/engine';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

/** Human label for a calendar account type, e.g. Google Calendar or an on-device calendar. */
export function accountLabel(
  c: Pick<DeviceCalendarInfo, 'accountName' | 'accountType'>,
): string {
  if (c.accountType === 'com.google') return `Google · ${c.accountName}`;
  if (c.accountType === 'LOCAL') return `On this device · ${c.accountName}`;
  return c.accountName || c.accountType;
}

/** Next calendar event on the home-screen widget, from calendars the user picks. */
export function CalendarSettings() {
  const theme = useTheme();
  const t = useT();
  const [model, setModel] = useState<Model | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [refreshedAt, setRefreshedAt] = useState<Date | null>(null);

  const reload = useCallback(
    async () => setModel(await engine.getCalendarSettings()),
    [],
  );
  useEffect(() => {
    reload();
  }, [reload]);

  if (!model) return null;

  const save = async (enabled: boolean, calendars: DeviceCalendarInfo[]) => {
    const selected = calendars.filter(c => c.selected).map(c => c.id);
    // All selected is stored as "all", so calendars added later are included automatically.
    await engine.saveCalendarSettings(
      enabled,
      selected.length === calendars.length ? [] : selected,
    );
    reload();
  };

  const toggle = async (on: boolean) => {
    if (on && !model.permitted) {
      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.READ_CALENDAR,
      );
      if (result !== PermissionsAndroid.RESULTS.GRANTED) return;
      const fresh = await engine.getCalendarSettings();
      await save(true, fresh.calendars);
      return;
    }
    await save(on, model.calendars);
  };

  const toggleCalendar = (id: number) => {
    const calendars = model.calendars.map(c =>
      c.id === id ? { ...c, selected: !c.selected } : c,
    );
    setModel({ ...model, calendars });
    save(true, calendars);
  };

  const groups = new Map<string, DeviceCalendarInfo[]>();
  for (const c of model.calendars) {
    const key = accountLabel(c);
    groups.set(key, [...(groups.get(key) ?? []), c]);
  }

  return (
    <>
      <SectionTitle>{t('widgetCalendar')}</SectionTitle>
      <Card style={styles.card}>
        <ToggleRow
          label={t('widgetCalendarToggle')}
          hint={t('widgetCalendarHint')}
          value={model.enabled && model.permitted}
          onValueChange={toggle}
        />
        {model.enabled && model.permitted && (
          <>
            {model.calendars.length === 0 && (
              <Text
                style={[styles.empty, { color: theme.colors.textSecondary }]}
              >
                {t('widgetCalendarNone')}
              </Text>
            )}
            {[...groups.entries()].map(([account, calendars]) => (
              <View key={account}>
                <Divider />
                <Text
                  style={[
                    styles.account,
                    { color: theme.colors.textSecondary },
                  ]}
                >
                  {account}
                </Text>
                {calendars.map(c => (
                  <Pressable
                    key={c.id}
                    onPress={() => toggleCalendar(c.id)}
                    accessibilityRole="checkbox"
                    accessibilityState={{ checked: c.selected }}
                    style={styles.row}
                  >
                    <View style={[styles.dot, { backgroundColor: c.color }]} />
                    <Text
                      style={[styles.name, { color: theme.colors.text }]}
                      numberOfLines={1}
                    >
                      {c.name}
                    </Text>
                    <View
                      style={[
                        styles.box,
                        {
                          borderColor: c.selected
                            ? theme.colors.primary
                            : theme.colors.divider,
                        },
                        c.selected && { backgroundColor: theme.colors.primary },
                      ]}
                    >
                      {c.selected && (
                        <Icon
                          name="check"
                          color={theme.colors.onPrimary}
                          size={16}
                          strokeWidth={3}
                        />
                      )}
                    </View>
                  </Pressable>
                ))}
              </View>
            ))}
            <Divider />
            <View style={styles.refresh}>
              <Button
                label={
                  refreshing ? t('calendarRefreshing') : t('calendarRefresh')
                }
                icon="reset"
                variant="secondary"
                disabled={refreshing}
                onPress={async () => {
                  setRefreshing(true);
                  try {
                    await engine.refreshCalendar();
                    setRefreshedAt(new Date());
                  } finally {
                    setRefreshing(false);
                  }
                }}
              />
              {refreshedAt && (
                <Text
                  style={[styles.empty, { color: theme.colors.textSecondary }]}
                >
                  {t('calendarRefreshed', {
                    time: refreshedAt.toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    }),
                  })}
                </Text>
              )}
            </View>
          </>
        )}
      </Card>
    </>
  );
}

const styles = StyleSheet.create({
  refresh: { paddingVertical: 12, gap: 4 },
  card: { paddingVertical: spacing.xs },
  empty: { fontSize: 14, paddingVertical: spacing.md },
  account: {
    fontSize: 13,
    fontWeight: '600',
    paddingTop: spacing.md,
    paddingBottom: spacing.xs,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    minHeight: 48,
  },
  dot: { width: 12, height: 12, borderRadius: 6 },
  name: { flex: 1, fontSize: 16 },
  box: {
    width: 22,
    height: 22,
    borderRadius: 6,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
