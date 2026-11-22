import React from 'react';
import { Platform, StyleSheet, Text, View } from 'react-native';
import { Card, Chip, SectionTitle } from '../../components/ui';
import { useT, type StringKey } from '../../i18n';
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
