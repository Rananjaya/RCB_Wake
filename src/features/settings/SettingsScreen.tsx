import React, { useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Floral } from '../../components/Floral';
import { Toolbar } from '../../components/Toolbar';
import {
  Card,
  Chip,
  Divider,
  NavRow,
  SectionTitle,
  ToggleRow,
} from '../../components/ui';
import {
  LanguageOverride,
  languageNames,
  useT,
  type Language,
} from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import { engine } from '../../services/engine';
import { useAlarmStore } from '../../storage/alarmStore';
import { useSettingsStore } from '../../storage/settingsStore';
import { themes, type ThemeName } from '../../theme/themes';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/themes';
import { DIFFICULTIES, QuestionStepper } from '../alarms/AlarmEditorScreen';
import { clampQuestions, SNOOZE_OPTIONS } from '../alarms/alarmDraft';
import { SoundPicker } from '../alarms/SoundPicker';
import { soundDisplayName } from '../alarms/soundNames';
import { AccentPicker } from './AccentPicker';
import { AppIconPicker } from './AppIconPicker';
import { CalendarSettings } from './CalendarSettings';
import { NfcTagSettings } from './NfcTagSettings';
import { SleepReminderSettings } from './SleepReminderSettings';
import { WeatherSettings } from './WeatherSettings';

type Props = NativeStackScreenProps<RootStackParamList, 'Settings'>;

export function SettingsScreen(props: Props) {
  return (
    <LanguageOverride language="en">
      <SettingsContent {...props} />
    </LanguageOverride>
  );
}

function SettingsContent({ navigation }: Props) {
  const theme = useTheme();
  const t = useT();
  const settings = useSettingsStore(s => s.settings);
  const update = useSettingsStore(s => s.update);
  const status = useAlarmStore(s => s.status);
  const refreshStatus = useAlarmStore(s => s.refreshStatus);
  const [soundPickerOpen, setSoundPickerOpen] = useState(false);
  const set = (patch: Parameters<typeof update>[0]) =>
    update(patch).catch(() => undefined);

  useEffect(() => {
    refreshStatus();
  }, [refreshStatus]);

  return (
    <SafeAreaView
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <Toolbar title={t('settings')} onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.content}>
        <SectionTitle>{t('appearance')}</SectionTitle>
        <View style={styles.themes}>
          {(['midnight', 'pink'] as ThemeName[]).map(name => (
            <ThemeCard
              key={name}
              name={name}
              label={name === 'midnight' ? t('themeMidnight') : t('themePink')}
              selected={settings.theme === name}
              onPress={() => set({ theme: name })}
            />
          ))}
        </View>

        <AccentPicker />

        <AppIconPicker />

        <SectionTitle>{t('timeFormat')}</SectionTitle>
        <View style={styles.wrap}>
          <Chip
            label={t('formatSystem')}
            selected={settings.timeFormat === 'system'}
            onPress={() => set({ timeFormat: 'system' })}
          />
          <Chip
            label={t('format12')}
            selected={settings.timeFormat === '12'}
            onPress={() => set({ timeFormat: '12' })}
          />
          <Chip
            label={t('format24')}
            selected={settings.timeFormat === '24'}
            onPress={() => set({ timeFormat: '24' })}
          />
        </View>

        <SectionTitle>{t('language')}</SectionTitle>
        <View style={styles.wrap}>
          {(Object.keys(languageNames) as Language[]).map(lang => (
            <Chip
              key={lang}
              label={languageNames[lang]}
              selected={settings.language === lang}
              onPress={() => set({ language: lang })}
            />
          ))}
        </View>

        <SectionTitle>{t('alarmDefaults')}</SectionTitle>
        <Card style={styles.card}>
          <NavRow
            icon="music"
            label={t('defaultSound')}
            value={soundDisplayName(
              settings.defaultSoundUri,
              settings.defaultSoundName,
              t,
            )}
            onPress={() => setSoundPickerOpen(true)}
          />
          <Divider />
          <Text
            style={[styles.subLabel, { color: theme.colors.textSecondary }]}
          >
            {t('defaultSnooze')}
          </Text>
          <View style={styles.wrap}>
            {SNOOZE_OPTIONS.map(m => (
              <Chip
                key={m}
                label={t('snoozeMinutes', { m })}
                selected={settings.defaultSnoozeMinutes === m}
                onPress={() => set({ defaultSnoozeMinutes: m })}
              />
            ))}
          </View>
          <Divider />
          <ToggleRow
            label={t('vibration')}
            value={settings.defaultVibration}
            onValueChange={v => set({ defaultVibration: v })}
          />
          <Divider />
          <ToggleRow
            label={t('gradualVolume')}
            hint={t('gradualVolumeHint')}
            value={settings.defaultGradualVolume}
            onValueChange={v => set({ defaultGradualVolume: v })}
          />
          <Divider />
          <ToggleRow
            label={t('challenge')}
            hint={t('challengeHint')}
            value={settings.defaultChallengeEnabled}
            onValueChange={v => set({ defaultChallengeEnabled: v })}
          />
          {settings.defaultChallengeEnabled && (
            <>
              <View style={[styles.wrap, styles.difficulties]}>
                {DIFFICULTIES.map(d => (
                  <Chip
                    key={d.value}
                    label={t(d.key)}
                    selected={settings.defaultChallengeDifficulty === d.value}
                    onPress={() => set({ defaultChallengeDifficulty: d.value })}
                  />
                ))}
              </View>
              <QuestionStepper
                value={settings.defaultChallengeQuestionCount}
                onChange={n =>
                  set({ defaultChallengeQuestionCount: clampQuestions(n) })
                }
              />
            </>
          )}
        </Card>

        <SleepReminderSettings />

        <NfcTagSettings />

        <WeatherSettings />

        <CalendarSettings />

        <SectionTitle>{t('permissions')}</SectionTitle>
        <Card style={styles.card}>
          <PermissionRow
            label={t('exactAlarms')}
            granted={status?.exactAlarmPermitted}
            onFix={() => engine.requestExactAlarmPermission()}
          />
          <Divider />
          <PermissionRow
            label={t('notifications')}
            granted={status?.notificationsPermitted}
            onFix={() => engine.openNotificationSettings()}
          />
          <Divider />
          <PermissionRow
            label={t('fullScreenAlerts')}
            granted={status?.fullScreenIntentPermitted}
            onFix={() => engine.requestFullScreenIntentPermission()}
          />
          <Text style={[styles.note, { color: theme.colors.textSecondary }]}>
            {t('reliabilityNote')}
          </Text>
        </Card>

        <SectionTitle>{t('about')}</SectionTitle>
        <Card style={styles.card}>
          <NavRow
            label={t('about')}
            value={status ? t('version', { v: status.versionName }) : undefined}
            onPress={() => navigation.navigate('About')}
          />
          <Divider />
          <NavRow
            label={t('showTutorial')}
            onPress={() => navigation.navigate('Tutorial')}
          />
        </Card>
      </ScrollView>
      <SoundPicker
        visible={soundPickerOpen}
        selectedUri={settings.defaultSoundUri}
        onSelect={s =>
          set({ defaultSoundUri: s.uri, defaultSoundName: s.name })
        }
        onClose={() => setSoundPickerOpen(false)}
      />
    </SafeAreaView>
  );
}

function ThemeCard({
  name,
  label,
  selected,
  onPress,
}: {
  name: ThemeName;
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  const current = useTheme();
  const preview = themes[name];
  return (
    <Pressable
      testID={`theme-${name}`}
      onPress={onPress}
      accessibilityRole="radio"
      accessibilityState={{ checked: selected }}
      accessibilityLabel={label}
      style={[
        styles.themeCard,
        {
          backgroundColor: preview.colors.background,
          borderColor: selected
            ? current.colors.primary
            : current.colors.divider,
        },
      ]}
    >
      {preview.floral && <Floral size={32} style={styles.themeFloral} />}
      <View
        style={[
          styles.themeSwatch,
          { backgroundColor: preview.colors.surface },
        ]}
      >
        <View
          style={[styles.themeDot, { backgroundColor: preview.colors.primary }]}
        />
        <View
          style={[
            styles.themeLine,
            { backgroundColor: preview.colors.secondary },
          ]}
        />
      </View>
      <Text style={[styles.themeLabel, { color: preview.colors.text }]}>
        {label}
      </Text>
    </Pressable>
  );
}

function PermissionRow({
  label,
  granted,
  onFix,
}: {
  label: string;
  granted?: boolean;
  onFix: () => void;
}) {
  const theme = useTheme();
  const t = useT();
  return (
    <View style={styles.permRow}>
      <Text style={[styles.permLabel, { color: theme.colors.text }]}>
        {label}
      </Text>
      {granted ? (
        <Text style={[styles.permState, { color: theme.colors.success }]}>
          {t('granted')}
        </Text>
      ) : (
        <Pressable onPress={onFix} accessibilityRole="button" hitSlop={8}>
          <Text style={[styles.permState, { color: theme.colors.warning }]}>
            {t('notGranted')} · {t('allow')}
          </Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { paddingHorizontal: spacing.lg, paddingBottom: 64 },
  themes: { flexDirection: 'row', gap: spacing.md },
  themeCard: {
    flex: 1,
    borderRadius: radius.md,
    borderWidth: 2,
    padding: spacing.md,
    gap: spacing.md,
    overflow: 'hidden',
  },
  themeFloral: { position: 'absolute', right: 4, top: 4 },
  themeSwatch: {
    height: 56,
    borderRadius: radius.sm,
    padding: spacing.sm,
    justifyContent: 'space-between',
  },
  themeDot: { width: 18, height: 18, borderRadius: 9 },
  themeLine: { height: 6, width: '60%', borderRadius: 3 },
  themeLabel: { fontSize: 15, fontWeight: '600' },
  wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  card: { paddingVertical: spacing.xs },
  subLabel: { fontSize: 14, marginTop: spacing.md, marginBottom: spacing.sm },
  difficulties: { marginTop: spacing.sm },
  permRow: { flexDirection: 'row', alignItems: 'center', minHeight: 52 },
  permLabel: { flex: 1, fontSize: 16 },
  permState: { fontSize: 14, fontWeight: '600' },
  note: { fontSize: 13, paddingVertical: spacing.md, lineHeight: 18 },
});
