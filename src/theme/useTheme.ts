import { useMemo } from 'react';
import { useSettingsStore } from '../storage/settingsStore';
import { themes, withAccent, type Theme, type ThemeName } from './themes';

export function useTheme(): Theme {
  const name = useSettingsStore(s => s.settings.theme) as ThemeName;
  const accent = useSettingsStore(s => s.settings.accentColor);
  return useMemo(
    () => withAccent(themes[name] ?? themes.midnight, accent),
    [name, accent],
  );
}
