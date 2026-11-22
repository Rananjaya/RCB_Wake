import React from 'react';
import {
  PermissionsAndroid,
  Platform,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  Card,
  Chip,
  Divider,
  SectionTitle,
  ToggleRow,
} from '../../components/ui';
import { useT, type StringKey } from '../../i18n';
import { engine } from '../../services/engine';
import { useSettingsStore } from '../../storage/settingsStore';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

/** Material You colours come from the wallpaper and need Android 12 (API 31). */
const COLORS: { value: string; label: StringKey; minApi: number }[] = [
  { value: 'white', label: 'widgetColorWhite', minApi: 0 },
  { value: 'material', label: 'widgetColorMaterial', minApi: 31 },
  { value: 'accent', label: 'widgetColorAccent', minApi: 0 },
];

/** Appearance of the home-screen clock widget. */
export function WidgetSettings() {
  const theme = useTheme();
  const t = useT();
  const settings = useSettingsStore(s => s.settings);
  const update = useSettingsStore(s => s.update);
  const api = typeof Platform.Version === 'number' ? Platform.Version : 0;

  return (
    <>
      <SectionTitle>{t('widgetSection')}</SectionTitle>
      <Card style={styles.card}>
        <Text style={[styles.label, { color: theme.colors.text }]}>
          {t('widgetAdd')}
        </Text>
        <View style={styles.chips}>
          <Chip
            label={t('widgetClockName')}
            selected={false}
            onPress={() => engine.pinWidget('clock').catch(() => false)}
          />
          <Chip
            label={t('widgetGlanceName')}
            selected={false}
            onPress={() => engine.pinWidget('glance').catch(() => false)}
          />
        </View>
        <Divider />
        <Text style={[styles.label, { color: theme.colors.text }]}>
          {t('widgetTextColor')}
        </Text>
        <View style={styles.chips}>
          {COLORS.filter(c => api >= c.minApi).map(c => (
            <Chip
              key={c.value}
              label={t(c.label)}
              selected={settings.widgetTextColor === c.value}
              onPress={() =>
                update({ widgetTextColor: c.value }).catch(() => undefined)
              }
            />
          ))}
        </View>
        {api >= 31 && (
          <Text style={[styles.hint, { color: theme.colors.textSecondary }]}>
            {t('widgetTextColorHint')}
          </Text>
        )}
        <Divider />
        <ToggleRow
          label={t('widgetShowPlace')}
          hint={t('widgetShowPlaceHint')}
          value={settings.widgetShowPlace}
          onValueChange={v =>
            update({ widgetShowPlace: v }).catch(() => undefined)
          }
        />
        <Divider />
        <ToggleRow
          label={t('widgetShowGreeting')}
          hint={t('widgetShowGreetingHint')}
          value={settings.widgetShowGreeting}
          onValueChange={v =>
            update({ widgetShowGreeting: v }).catch(() => undefined)
          }
        />
        <Divider />
        <ToggleRow
          label={t('widgetShowHeadphones')}
          hint={t('widgetShowHeadphonesHint')}
          value={settings.widgetShowHeadphones}
          onValueChange={async v => {
            // Bluetooth names and battery need "Nearby devices" on Android 12+; wired works without.
            if (v && api >= 31) {
              await PermissionsAndroid.request(
                PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
              );
            }
            update({ widgetShowHeadphones: v }).catch(() => undefined);
          }}
        />
      </Card>
    </>
  );
}

const styles = StyleSheet.create({
  card: { paddingVertical: spacing.md, gap: spacing.sm },
  label: { fontSize: 16, fontWeight: '500' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  hint: { fontSize: 13 },
});
