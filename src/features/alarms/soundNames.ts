import type { StringKey, Translate } from '../../i18n';

const BUILT_IN_NAME_KEYS: Record<string, StringKey> = {
  'builtin:dawn': 'soundDawn',
  'builtin:chime': 'soundChime',
  'builtin:pulse': 'soundPulse',
  'builtin:classic': 'soundClassic',
  'system:default': 'systemDefaultSound',
};

/** Localised name for bundled sounds; custom files keep their file name. */
export function soundDisplayName(
  uri: string,
  storedName: string,
  t: Translate,
): string {
  const key = BUILT_IN_NAME_KEYS[uri];
  return key ? t(key) : storedName;
}
