import React from 'react';
import {Pressable, StyleSheet, Switch, Text, View, type StyleProp, type ViewStyle} from 'react-native';
import {useTheme} from '../theme/useTheme';
import {radius, spacing} from '../theme/themes';
import {Icon, type IconName} from './Icon';

export function Card({children, style}: {children: React.ReactNode; style?: StyleProp<ViewStyle>}) {
  const {colors} = useTheme();
  return <View style={[styles.card, {backgroundColor: colors.surface}, style]}>{children}</View>;
}

export function SectionTitle({children}: {children: string}) {
  const {colors} = useTheme();
  return (
    <Text style={[styles.sectionTitle, {color: colors.secondary}]} accessibilityRole="header">
      {children}
    </Text>
  );
}

type ChipProps = {
  label: string;
  selected: boolean;
  onPress: () => void;
  accessibilityLabel?: string;
  round?: boolean;
};

export function Chip({label, selected, onPress, accessibilityLabel, round}: ChipProps) {
  const {colors} = useTheme();
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{selected}}
      accessibilityLabel={accessibilityLabel ?? label}
      style={[
        round ? styles.roundChip : styles.chip,
        {
          backgroundColor: selected ? colors.selected : colors.elevated,
          borderColor: selected ? colors.primary : 'transparent',
        },
      ]}>
      <Text style={[styles.chipText, {color: selected ? colors.onSelected : colors.text}]}>{label}</Text>
    </Pressable>
  );
}

type ToggleRowProps = {
  label: string;
  hint?: string;
  value: boolean;
  onValueChange: (value: boolean) => void;
  testID?: string;
};

export function ToggleRow({label, hint, value, onValueChange, testID}: ToggleRowProps) {
  const {colors} = useTheme();
  return (
    <View style={styles.row}>
      <View style={styles.rowText}>
        <Text style={[styles.rowLabel, {color: colors.text}]}>{label}</Text>
        {hint ? <Text style={[styles.rowHint, {color: colors.textSecondary}]}>{hint}</Text> : null}
      </View>
      <Switch
        testID={testID}
        value={value}
        onValueChange={onValueChange}
        accessibilityLabel={label}
        trackColor={{false: colors.divider, true: colors.selected}}
        thumbColor={value ? colors.primary : colors.textSecondary}
      />
    </View>
  );
}

type NavRowProps = {label: string; value?: string; onPress: () => void; icon?: IconName};

export function NavRow({label, value, onPress, icon}: NavRowProps) {
  const {colors} = useTheme();
  return (
    <Pressable onPress={onPress} accessibilityRole="button" style={styles.row}>
      {icon && <Icon name={icon} color={colors.textSecondary} size={20} />}
      <View style={styles.rowText}>
        <Text style={[styles.rowLabel, {color: colors.text}]}>{label}</Text>
        {value ? (
          <Text style={[styles.rowHint, {color: colors.textSecondary}]} numberOfLines={1}>
            {value}
          </Text>
        ) : null}
      </View>
      <Icon name="chevronRight" color={colors.textSecondary} size={20} />
    </Pressable>
  );
}

type ButtonProps = {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
  icon?: IconName;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  testID?: string;
};

export function Button({label, onPress, variant = 'primary', icon, disabled, style, testID}: ButtonProps) {
  const {colors} = useTheme();
  const bg = variant === 'primary' ? colors.primary : colors.elevated;
  const fg = variant === 'primary' ? colors.onPrimary : variant === 'danger' ? colors.danger : colors.text;
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityState={{disabled}}
      style={({pressed}) => [
        styles.button,
        {backgroundColor: bg, opacity: disabled ? 0.4 : pressed ? 0.8 : 1},
        style,
      ]}>
      {icon && <Icon name={icon} color={fg} size={20} />}
      <Text style={[styles.buttonText, {color: fg}]}>{label}</Text>
    </Pressable>
  );
}

type BannerProps = {title: string; body: string; actionLabel: string; onAction: () => void};

export function Banner({title, body, actionLabel, onAction}: BannerProps) {
  const {colors} = useTheme();
  return (
    <View style={[styles.banner, {backgroundColor: colors.elevated, borderColor: colors.warning}]}>
      <Icon name="warning" color={colors.warning} size={20} />
      <View style={styles.rowText}>
        <Text style={[styles.rowLabel, {color: colors.text}]}>{title}</Text>
        <Text style={[styles.rowHint, {color: colors.textSecondary}]}>{body}</Text>
      </View>
      <Pressable onPress={onAction} accessibilityRole="button" hitSlop={8}>
        <Text style={[styles.bannerAction, {color: colors.primary}]}>{actionLabel}</Text>
      </Pressable>
    </View>
  );
}

export function Divider() {
  const {colors} = useTheme();
  return <View style={{height: StyleSheet.hairlineWidth, backgroundColor: colors.divider}} />;
}

const styles = StyleSheet.create({
  card: {borderRadius: radius.md, padding: spacing.lg},
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    marginTop: spacing.xl,
    marginBottom: spacing.sm,
    marginHorizontal: spacing.xs,
  },
  chip: {
    paddingHorizontal: spacing.lg,
    minHeight: 40,
    borderRadius: radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
  },
  roundChip: {
    width: 42,
    height: 42,
    borderRadius: 21,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
  },
  chipText: {fontSize: 15, fontWeight: '500'},
  row: {flexDirection: 'row', alignItems: 'center', minHeight: 56, gap: spacing.md, paddingVertical: spacing.sm},
  rowText: {flex: 1},
  rowLabel: {fontSize: 16, fontWeight: '500'},
  rowHint: {fontSize: 13, marginTop: 2},
  button: {
    minHeight: 52,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.xl,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
  },
  buttonText: {fontSize: 16, fontWeight: '600'},
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.md,
    borderRadius: radius.md,
    borderLeftWidth: 3,
    marginBottom: spacing.md,
  },
  bannerAction: {fontSize: 15, fontWeight: '700'},
});
