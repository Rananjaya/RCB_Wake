import React, { useEffect, useState } from 'react';
import {
  Alert,
  PermissionsAndroid,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  ToastAndroid,
  View,
} from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { FEATURES } from '../../app/features';
import { Icon } from '../../components/Icon';
import {
  Button,
  Card,
  Chip,
  Divider,
  NavRow,
  SectionTitle,
  ToggleRow,
} from '../../components/ui';
import { useT, type StringKey } from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import { engine, type AlarmConfig } from '../../services/engine';
import { useAlarmStore } from '../../storage/alarmStore';
import { useSettingsStore } from '../../storage/settingsStore';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/themes';
import {
  DAY_KEYS,
  DAY_LETTER_KEYS,
  formatClock,
  formatUntil,
  isDaySelected,
  toggleDay,
} from '../../utils/time';
import {
  clampQuestions,
  draftFromRecord,
  newAlarmDraft,
  SNOOZE_OPTIONS,
} from './alarmDraft';
import { SoundPicker } from './SoundPicker';
import { soundDisplayName } from './soundNames';

type Props = NativeStackScreenProps<RootStackParamList, 'AlarmEditor'>;

export const DIFFICULTIES: { value: string; key: StringKey }[] = [
  { value: 'easy', key: 'difficultyEasy' },
  { value: 'medium', key: 'difficultyMedium' },
  { value: 'hard', key: 'difficultyHard' },
];

export function AlarmEditorScreen({ navigation, route }: Props) {
  const theme = useTheme();
  const t = useT();
  const settings = useSettingsStore(s => s.settings);
  const existing = useAlarmStore(s =>
    s.alarms.find(a => a.id === route.params?.alarmId),
  );
  const save = useAlarmStore(s => s.save);
  const remove = useAlarmStore(s => s.remove);
  const [draft, setDraft] = useState<AlarmConfig>(() =>
    existing ? draftFromRecord(existing) : newAlarmDraft(settings),
  );
  const [soundPickerOpen, setSoundPickerOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  // null = device has no NFC (option hidden); otherwise the number of registered tags.
  const [nfcTagCount, setNfcTagCount] = useState<number | null>(null);
  useEffect(() => {
    engine.getNfcStatus().then(async status => {
      if (FEATURES.nfcChallenge && status.supported) {
        setNfcTagCount((await engine.getNfcTags()).length);
      }
    });
  }, []);
  const is24 = settings.is24Hour;
  const update = (patch: Partial<AlarmConfig>) =>
    setDraft(d => ({ ...d, ...patch }));
  const { time, period } = formatClock(draft.hour, draft.minute, is24);

  const pickTime = async () => {
    const picked = await engine.showTimePicker(draft.hour, draft.minute, is24);
    if (!picked.cancelled) update({ hour: picked.hour, minute: picked.minute });
  };

  const onSave = async () => {
    setSaving(true);
    try {
      const saved = await save(draft);
      // Report exactly what Android accepted — never claim success for a rejected alarm.
      if (saved.scheduleState === 'EXACT') {
        ToastAndroid.show(
          t('alarmSetIn', {
            duration: formatUntil(Date.now(), saved.nextTriggerAt, t),
          }),
          ToastAndroid.SHORT,
        );
      } else if (saved.scheduleState === 'INEXACT') {
        ToastAndroid.show(t('alarmSetInexact'), ToastAndroid.LONG);
      } else if (saved.scheduleState === 'FAILED') {
        Alert.alert(t('errorTitle'), t('alarmScheduleFailed'));
      }
      navigation.goBack();
    } catch (e) {
      Alert.alert(t('errorTitle'), e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  };

  const onDelete = () => {
    if (!existing) return;
    Alert.alert(t('deleteAlarmConfirm'), undefined, [
      { text: t('cancel'), style: 'cancel' },
      {
        text: t('delete'),
        style: 'destructive',
        onPress: async () => {
          await remove(existing.id);
          navigation.goBack();
        },
      },
    ]);
  };

  return (
    <SafeAreaView
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <View style={styles.header}>
        <Pressable
          onPress={() => navigation.goBack()}
          accessibilityRole="button"
          accessibilityLabel={t('cancel')}
          hitSlop={12}
        >
          <Icon name="close" color={theme.colors.text} />
        </Pressable>
        <Text
          style={[styles.headerTitle, { color: theme.colors.text }]}
          accessibilityRole="header"
        >
          {existing ? t('editAlarm') : t('newAlarm')}
        </Text>
        <Pressable
          testID="save-alarm"
          onPress={onSave}
          disabled={saving}
          accessibilityRole="button"
          accessibilityLabel={t('save')}
          hitSlop={12}
        >
          <Text style={[styles.save, { color: theme.colors.primary }]}>
            {t('save')}
          </Text>
        </Pressable>
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <Pressable
          testID="alarm-time"
          onPress={pickTime}
          accessibilityRole="button"
          accessibilityLabel={`${t('changeTime')}: ${time} ${
            period ? t(period) : ''
          }`}
          style={styles.timeBox}
        >
          <Text style={[styles.time, { color: theme.colors.text }]}>
            {time}
          </Text>
          {period && (
            <Text style={[styles.period, { color: theme.colors.secondary }]}>
              {t(period)}
            </Text>
          )}
        </Pressable>

        <SectionTitle>{t('repeat')}</SectionTitle>
        <View style={styles.days}>
          {DAY_LETTER_KEYS.map((key, i) => (
            <Chip
              key={key}
              round
              label={t(key)}
              accessibilityLabel={t(DAY_KEYS[i])}
              selected={isDaySelected(draft.repeatDays, i)}
              onPress={() =>
                update({ repeatDays: toggleDay(draft.repeatDays, i) })
              }
            />
          ))}
        </View>

        <SectionTitle>{t('label')}</SectionTitle>
        <TextInput
          testID="alarm-label"
          value={draft.label}
          onChangeText={label => update({ label })}
          placeholder={t('labelPlaceholder')}
          placeholderTextColor={theme.colors.textSecondary}
          maxLength={60}
          style={[
            styles.input,
            { backgroundColor: theme.colors.surface, color: theme.colors.text },
          ]}
          accessibilityLabel={t('label')}
        />

        <Card style={styles.card}>
          <NavRow
            icon="music"
            label={t('sound')}
            value={soundDisplayName(draft.soundUri, draft.soundName, t)}
            onPress={() => setSoundPickerOpen(true)}
          />
          <Divider />
          <ToggleRow
            label={t('vibration')}
            value={draft.vibrationEnabled}
            onValueChange={v => update({ vibrationEnabled: v })}
          />
          <Divider />
          <ToggleRow
            label={t('gradualVolume')}
            hint={t('gradualVolumeHint')}
            value={draft.gradualVolumeEnabled}
            onValueChange={v => update({ gradualVolumeEnabled: v })}
          />
        </Card>

        <SectionTitle>{t('snooze')}</SectionTitle>
        <View style={styles.wrap}>
          {SNOOZE_OPTIONS.map(m => (
            <Chip
              key={m}
              label={t('snoozeMinutes', { m })}
              selected={draft.snoozeMinutes === m}
              onPress={() => update({ snoozeMinutes: m })}
            />
          ))}
        </View>

        <Card style={styles.card}>
          <ToggleRow
            testID="challenge-toggle"
            label={t('challenge')}
            hint={t('challengeHint')}
            value={draft.challengeEnabled}
            onValueChange={v => update({ challengeEnabled: v })}
          />
          {draft.challengeEnabled && (
            <>
              <Text
                style={[styles.subLabel, { color: theme.colors.textSecondary }]}
              >
                {t('challengeType')}
              </Text>
              <View style={styles.wrap}>
                <Chip
                  label={t('challengeMath')}
                  selected={draft.challengeType === 'math'}
                  onPress={() => update({ challengeType: 'math' })}
                />
                <Chip
                  label={t('challengeQr')}
                  selected={draft.challengeType === 'qr'}
                  onPress={async () => {
                    // Ask for the camera now, not when the alarm is ringing.
                    const result = await PermissionsAndroid.request(
                      PermissionsAndroid.PERMISSIONS.CAMERA,
                    );
                    if (result === PermissionsAndroid.RESULTS.GRANTED) {
                      update({ challengeType: 'qr' });
                    }
                  }}
                />
                {nfcTagCount !== null && (
                  <Chip
                    label={t('challengeNfc')}
                    selected={draft.challengeType === 'nfc'}
                    onPress={() =>
                      nfcTagCount > 0 && update({ challengeType: 'nfc' })
                    }
                  />
                )}
              </View>
              {nfcTagCount === 0 && (
                <Text
                  style={[
                    styles.subLabel,
                    { color: theme.colors.textSecondary },
                  ]}
                >
                  {t('challengeNfcNeedsTag')}
                </Text>
              )}
              {draft.challengeType === 'qr' && (
                <Text
                  style={[
                    styles.subLabel,
                    { color: theme.colors.textSecondary },
                  ]}
                >
                  {t('challengeQrHint')}
                </Text>
              )}
              {draft.challengeType === 'math' && (
                <>
                  <Text
                    style={[
                      styles.subLabel,
                      { color: theme.colors.textSecondary },
                    ]}
                  >
                    {t('difficulty')}
                  </Text>
                  <View style={styles.wrap}>
                    {DIFFICULTIES.map(d => (
                      <Chip
                        key={d.value}
                        label={t(d.key)}
                        selected={draft.challengeDifficulty === d.value}
                        onPress={() => update({ challengeDifficulty: d.value })}
                      />
                    ))}
                  </View>
                  <QuestionStepper
                    value={draft.challengeQuestionCount}
                    onChange={n =>
                      update({ challengeQuestionCount: clampQuestions(n) })
                    }
                  />
                </>
              )}
            </>
          )}
        </Card>

        {existing && (
          <Button
            label={t('deleteAlarm')}
            variant="danger"
            icon="trash"
            onPress={onDelete}
            style={styles.delete}
          />
        )}
      </ScrollView>

      <SoundPicker
        visible={soundPickerOpen}
        selectedUri={draft.soundUri}
        onSelect={s => update({ soundUri: s.uri, soundName: s.name })}
        onClose={() => setSoundPickerOpen(false)}
      />
    </SafeAreaView>
  );
}

export function QuestionStepper({
  value,
  onChange,
}: {
  value: number;
  onChange: (n: number) => void;
}) {
  const theme = useTheme();
  const t = useT();
  const button = (delta: number, symbol: string) => (
    <Pressable
      onPress={() => onChange(value + delta)}
      accessibilityRole="button"
      accessibilityLabel={`${t('questions')} ${symbol}`}
      style={[styles.stepButton, { backgroundColor: theme.colors.elevated }]}
    >
      <Text style={[styles.stepSymbol, { color: theme.colors.text }]}>
        {symbol}
      </Text>
    </Pressable>
  );
  return (
    <View style={styles.stepper}>
      <Text style={[styles.stepLabel, { color: theme.colors.text }]}>
        {t('questions')}
      </Text>
      {button(-1, '−')}
      <Text
        style={[styles.stepValue, { color: theme.colors.text }]}
        accessibilityLiveRegion="polite"
      >
        {value}
      </Text>
      {button(1, '+')}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  header: {
    height: 60,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.lg,
    gap: spacing.lg,
  },
  headerTitle: { flex: 1, fontSize: 20, fontWeight: '600' },
  save: { fontSize: 17, fontWeight: '700' },
  content: { paddingHorizontal: spacing.lg, paddingBottom: 64 },
  timeBox: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'center',
    paddingVertical: spacing.xl,
    gap: 8,
  },
  time: { fontSize: 80, fontWeight: '200', letterSpacing: -1 },
  period: { fontSize: 24, fontWeight: '500' },
  days: { flexDirection: 'row', justifyContent: 'space-between' },
  wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  input: {
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 52,
    fontSize: 16,
  },
  card: { marginTop: spacing.xl, paddingVertical: spacing.xs },
  subLabel: { fontSize: 14, marginTop: spacing.sm, marginBottom: spacing.sm },
  stepper: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    marginTop: spacing.md,
    marginBottom: spacing.sm,
  },
  stepLabel: { flex: 1, fontSize: 16, fontWeight: '500' },
  stepButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepSymbol: { fontSize: 22, fontWeight: '500' },
  stepValue: {
    fontSize: 20,
    fontWeight: '600',
    minWidth: 28,
    textAlign: 'center',
  },
  delete: { marginTop: spacing.xxl },
});
