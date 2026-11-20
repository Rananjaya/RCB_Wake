import React from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Icon } from '../../components/Icon';
import { SectionTitle } from '../../components/ui';
import { useT } from '../../i18n';
import { useSettingsStore } from '../../storage/settingsStore';
import {
  accentNames,
  accents,
  themes,
  type ThemeName,
} from '../../theme/themes';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

const SIZE = 44;

/** Built-in accent colours; "default" keeps the theme's own accent. */
export function AccentPicker() {
  const theme = useTheme();
  const t = useT();
  const settings = useSettingsStore(s => s.settings);
  const update = useSettingsStore(s => s.update);
  const base = themes[settings.theme as ThemeName] ?? themes.midnight;
  const options: { id: string; color: string; label: string }[] = [
    { id: 'default', color: base.colors.primary, label: t('accentDefault') },
    ...accentNames
      .map(id => ({
        id,
        color: base.dark ? accents[id].dark.primary : accents[id].light.primary,
        label: id,
      }))
      // Hide the accent that is identical to the theme's own colour.
      .filter(o => o.color.toLowerCase() !== base.colors.primary.toLowerCase()),
  ];

  return (
    <>
      <SectionTitle>{t('accentColor')}</SectionTitle>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.row}
      >
        {options.map(o => {
          const selected = settings.accentColor === o.id;
          return (
            <Pressable
              key={o.id}
              testID={`accent-${o.id}`}
              onPress={() =>
                update({ accentColor: o.id }).catch(() => undefined)
              }
              accessibilityRole="radio"
              accessibilityState={{ checked: selected }}
              accessibilityLabel={o.label}
              style={[
                styles.ring,
                selected ? { borderColor: theme.colors.text } : styles.ringOff,
              ]}
            >
              <View style={[styles.swatch, { backgroundColor: o.color }]}>
                {selected && (
                  <Icon
                    name="check"
                    color="#FFFFFF"
                    size={20}
                    strokeWidth={2.6}
                  />
                )}
              </View>
            </Pressable>
          );
        })}
      </ScrollView>
    </>
  );
}

const styles = StyleSheet.create({
  row: { gap: spacing.sm, paddingHorizontal: spacing.xs },
  ring: { padding: 3, borderRadius: SIZE, borderWidth: 2 },
  ringOff: { borderColor: 'transparent' },
  swatch: {
    width: SIZE,
    height: SIZE,
    borderRadius: SIZE / 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
