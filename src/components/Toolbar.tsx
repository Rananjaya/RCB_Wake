import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useT } from '../i18n';
import { useTheme } from '../theme/useTheme';
import { spacing } from '../theme/themes';
import { Floral } from './Floral';
import { Icon } from './Icon';

type Props = {
  title: string;
  onSettings?: () => void;
  onBack?: () => void;
  right?: React.ReactNode;
};

export function Toolbar({ title, onSettings, onBack, right }: Props) {
  const theme = useTheme();
  const t = useT();
  return (
    <View style={styles.bar}>
      {theme.floral && <Floral style={styles.floral} />}
      {onBack && (
        <Pressable
          onPress={onBack}
          accessibilityRole="button"
          accessibilityLabel={t('back')}
          hitSlop={12}
          style={styles.iconButton}
        >
          <Icon name="back" color={theme.colors.text} />
        </Pressable>
      )}
      <Text
        style={[styles.title, { color: theme.colors.text }]}
        accessibilityRole="header"
        numberOfLines={1}
      >
        {title}
      </Text>
      {right}
      {onSettings && (
        <Pressable
          onPress={onSettings}
          accessibilityRole="button"
          accessibilityLabel={t('settings')}
          hitSlop={12}
          style={styles.iconButton}
        >
          <Icon name="settings" color={theme.colors.textSecondary} />
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    height: 64,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.lg,
    gap: spacing.sm,
  },
  title: { flex: 1, fontSize: 24, fontWeight: '600', letterSpacing: 0.2 },
  iconButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  floral: { position: 'absolute', right: 56, top: 6 },
});
