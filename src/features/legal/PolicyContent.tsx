import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import {
  PRIVACY_POLICY_EFFECTIVE,
  PRIVACY_POLICY_INTRO,
  PRIVACY_POLICY_SECTIONS,
} from '../../legal/privacyPolicy';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

/** Full privacy policy text (English), shared by the policy screen and the first-run agreement. */
export function PolicyContent() {
  const theme = useTheme();
  const body = [styles.body, { color: theme.colors.textSecondary }];
  return (
    <View style={styles.container}>
      <Text style={[styles.meta, { color: theme.colors.textSecondary }]}>
        {`Effective date: ${PRIVACY_POLICY_EFFECTIVE}`}
      </Text>
      {PRIVACY_POLICY_INTRO.map(p => (
        <Text key={p} style={body}>
          {p}
        </Text>
      ))}
      {PRIVACY_POLICY_SECTIONS.map(s => (
        <View key={s.title} style={styles.section}>
          <Text
            style={[styles.heading, { color: theme.colors.text }]}
            accessibilityRole="header"
          >
            {s.title}
          </Text>
          {s.paragraphs.map(p => (
            <Text key={p} style={body}>
              {p}
            </Text>
          ))}
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing.md },
  section: { gap: spacing.sm, marginTop: spacing.sm },
  meta: { fontSize: 13 },
  heading: { fontSize: 17, fontWeight: '600' },
  body: { fontSize: 15, lineHeight: 22 },
});
