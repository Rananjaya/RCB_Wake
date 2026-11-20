import {useSettingsStore} from '../storage/settingsStore';
import {themes, type Theme, type ThemeName} from './themes';

export function useTheme(): Theme {
  const name = useSettingsStore(s => s.settings.theme) as ThemeName;
  return themes[name] ?? themes.midnight;
}
