import React from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Toolbar } from '../../components/Toolbar';
import { useT } from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';
import { PolicyContent } from './PolicyContent';

type Props = NativeStackScreenProps<RootStackParamList, 'PrivacyPolicy'>;

export function PrivacyPolicyScreen({ navigation }: Props) {
  const theme = useTheme();
  const t = useT();
  return (
    <SafeAreaView
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <Toolbar title={t('privacyPolicy')} onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.content}>
        <PolicyContent />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { paddingHorizontal: spacing.xl, paddingBottom: 64 },
});
