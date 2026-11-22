import React, { useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Toolbar } from '../../components/Toolbar';
import { Divider } from '../../components/ui';
import { useT } from '../../i18n';
import {
  APACHE_2_0_TEXT,
  OPEN_SOURCE_LICENSES,
} from '../../legal/openSourceLicenses';
import type { RootStackParamList } from '../../navigation/types';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

type Props = NativeStackScreenProps<RootStackParamList, 'Licenses'>;

/** Third-party libraries bundled in the app, with their licence texts. */
export function LicensesScreen({ navigation }: Props) {
  const theme = useTheme();
  const t = useT();
  const [open, setOpen] = useState<string | null>(null);
  const items = [
    ...OPEN_SOURCE_LICENSES,
    {
      name: 'Apache License 2.0 (full text)',
      version: '',
      license: 'Apache-2.0',
      text: APACHE_2_0_TEXT,
    },
  ];
  return (
    <SafeAreaView
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <Toolbar
        title={t('openSourceLicenses')}
        onBack={() => navigation.goBack()}
      />
      <FlatList
        data={items}
        keyExtractor={i => i.name}
        ItemSeparatorComponent={Divider}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <Text style={[styles.intro, { color: theme.colors.textSecondary }]}>
            {t('openSourceIntro')}
          </Text>
        }
        renderItem={({ item }) => (
          <Pressable
            onPress={() => setOpen(open === item.name ? null : item.name)}
            accessibilityRole="button"
            accessibilityState={{ expanded: open === item.name }}
            style={styles.row}
          >
            <Text style={[styles.name, { color: theme.colors.text }]}>
              {item.name}
            </Text>
            <Text style={[styles.meta, { color: theme.colors.textSecondary }]}>
              {[item.version, item.license].filter(Boolean).join(' · ')}
            </Text>
            {open === item.name && (
              <View style={styles.textBox}>
                <Text
                  style={[styles.text, { color: theme.colors.textSecondary }]}
                  selectable
                >
                  {item.text}
                </Text>
              </View>
            )}
          </Pressable>
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  list: { paddingHorizontal: spacing.xl, paddingBottom: 64 },
  intro: { fontSize: 14, lineHeight: 20, paddingVertical: spacing.md },
  row: { paddingVertical: spacing.md, gap: 2 },
  name: { fontSize: 16, fontWeight: '500' },
  meta: { fontSize: 13 },
  textBox: { marginTop: spacing.sm },
  text: { fontSize: 12, lineHeight: 17 },
});
