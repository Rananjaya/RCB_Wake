import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Floral } from '../../components/Floral';
import { Icon } from '../../components/Icon';
import { Toolbar } from '../../components/Toolbar';
import { Card, SectionTitle } from '../../components/ui';
import { useT } from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import { useAlarmStore } from '../../storage/alarmStore';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

type Props = NativeStackScreenProps<RootStackParamList, 'About'>;

export function AboutScreen({ navigation }: Props) {
  const theme = useTheme();
  const t = useT();
  const status = useAlarmStore(s => s.status);
  const body = [styles.body, { color: theme.colors.textSecondary }];
  return (
    <SafeAreaView
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <Toolbar title={t('about')} onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.hero}>
          {theme.floral ? (
            <Floral size={64} />
          ) : (
            <Icon
              name="alarm"
              color={theme.colors.primary}
              size={56}
              strokeWidth={1.6}
            />
          )}
          <Text style={[styles.name, { color: theme.colors.text }]}>
            {t('appName')}
          </Text>
          <Text style={[styles.tagline, { color: theme.colors.secondary }]}>
            {t('tagline')}
          </Text>
          {status && (
            <Text
              style={[styles.version, { color: theme.colors.textSecondary }]}
            >
              {t('version', {
                v: `${status.versionName} (${status.versionCode})`,
              })}
            </Text>
          )}
        </View>
        <Card>
          <Text style={body}>{t('aboutBody')}</Text>
        </Card>
        <SectionTitle>{t('privacy')}</SectionTitle>
        <Card>
          <Text style={body}>{t('privacyBody')}</Text>
        </Card>
        <SectionTitle>{t('license')}</SectionTitle>
        <Card>
          <Text style={body}>{t('licenseBody')}</Text>
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { paddingHorizontal: spacing.lg, paddingBottom: 64 },
  hero: { alignItems: 'center', paddingVertical: spacing.xxl, gap: spacing.sm },
  name: { fontSize: 28, fontWeight: '600', marginTop: spacing.sm },
  tagline: { fontSize: 16 },
  version: { fontSize: 14 },
  body: { fontSize: 15, lineHeight: 22 },
});
