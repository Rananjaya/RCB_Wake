import React, { useEffect } from 'react';
import { BackHandler, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Floral } from '../../components/Floral';
import { Icon } from '../../components/Icon';
import { Button } from '../../components/ui';
import { LanguageOverride, useT, type StringKey } from '../../i18n';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';
import { PRIVACY_AGREEMENT_VERSION } from './onboarding';

export const PRIVACY_POINTS: StringKey[] = [
  'privacyPointLocal',
  'privacyPointOffline',
  'privacyPointTracking',
  'privacyPointBackup',
  'privacyPointOptional',
  'privacyPointPermissions',
  'privacyPointLicense',
];

/** Accurate privacy statements, shared by the agreement and the About screen. */
export function PrivacyPoints() {
  const theme = useTheme();
  const t = useT();
  return (
    <View style={styles.points}>
      {PRIVACY_POINTS.map(key => (
        <View key={key} style={styles.point}>
          <View
            style={[styles.bullet, { backgroundColor: theme.colors.primary }]}
          />
          <Text style={[styles.pointText, { color: theme.colors.text }]}>
            {t(key)}
          </Text>
        </View>
      ))}
    </View>
  );
}

/** First-launch privacy agreement, always in English. There is no way past it without agreeing. */
export function PrivacyAgreementScreen({ onAgree }: { onAgree: () => void }) {
  return (
    <LanguageOverride language="en">
      <AgreementContent onAgree={onAgree} />
    </LanguageOverride>
  );
}

function AgreementContent({ onAgree }: { onAgree: () => void }) {
  const theme = useTheme();
  const t = useT();
  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      BackHandler.exitApp();
      return true;
    });
    return () => sub.remove();
  }, []);
  return (
    <SafeAreaView
      testID="privacy-agreement"
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <ScrollView contentContainerStyle={styles.content}>
        {theme.floral ? (
          <Floral size={56} />
        ) : (
          <Icon
            name="alarm"
            color={theme.colors.primary}
            size={48}
            strokeWidth={1.6}
          />
        )}
        <Text
          style={[styles.title, { color: theme.colors.text }]}
          accessibilityRole="header"
        >
          {t('agreementTitle')}
        </Text>
        <Text style={[styles.intro, { color: theme.colors.textSecondary }]}>
          {t('agreementIntro')}
        </Text>
        <PrivacyPoints />
        <Text style={[styles.version, { color: theme.colors.textSecondary }]}>
          {t('agreementVersion', { v: PRIVACY_AGREEMENT_VERSION })}
        </Text>
      </ScrollView>
      <View style={styles.buttons}>
        <Button
          testID="agreement-agree"
          label={t('agreementAgree')}
          onPress={onAgree}
        />
        <Button
          label={t('agreementExit')}
          variant="secondary"
          onPress={() => BackHandler.exitApp()}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: spacing.xl, gap: spacing.md },
  title: { fontSize: 26, fontWeight: '700', marginTop: spacing.sm },
  intro: { fontSize: 16, lineHeight: 23, marginBottom: spacing.sm },
  points: { gap: spacing.md },
  point: { flexDirection: 'row', gap: spacing.md },
  bullet: { width: 6, height: 6, borderRadius: 3, marginTop: 9 },
  pointText: { flex: 1, fontSize: 15, lineHeight: 22 },
  version: { fontSize: 12, marginTop: spacing.md },
  buttons: {
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.md,
    gap: spacing.sm,
  },
});
