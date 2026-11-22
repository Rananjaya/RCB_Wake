import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import {
  Card,
  Chip,
  Divider,
  SectionTitle,
  ToggleRow,
} from '../../components/ui';
import { useT } from '../../i18n';
import { useSettingsStore } from '../../storage/settingsStore';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

const LEADS = [5, 10, 15, 30];

/** Small notifications before the next alarm (silent) and the next timed calendar event. */
export function RemindersSettings() {
  const theme = useTheme();
  const t = useT();
  const settings = useSettingsStore(s => s.settings);
  const update = useSettingsStore(s => s.update);
  return (
    <>
      <SectionTitle>{t('remindersSection')}</SectionTitle>
      <Card style={styles.card}>
        <ToggleRow
          label={t('remindersToggle')}
          hint={t('remindersHint')}
          value={settings.remindersEnabled}
          onValueChange={v =>
            update({ remindersEnabled: v }).catch(() => undefined)
          }
        />
        {settings.remindersEnabled && (
          <>
            <Divider />
            <Text style={[styles.label, { color: theme.colors.text }]}>
              {t('remindersLead')}
            </Text>
            <View style={styles.chips}>
              {LEADS.map(m => (
                <Chip
                  key={m}
                  label={t('snoozeMinutes', { m })}
                  selected={settings.reminderLeadMinutes === m}
                  onPress={() =>
                    update({ reminderLeadMinutes: m }).catch(() => undefined)
                  }
                />
              ))}
            </View>
          </>
        )}
      </Card>
    </>
  );
}

const styles = StyleSheet.create({
  card: { paddingVertical: spacing.xs },
  label: { fontSize: 16, fontWeight: '500', paddingTop: spacing.md },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
    paddingVertical: spacing.md,
  },
});
