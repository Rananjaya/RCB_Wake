import { en } from '../src/i18n/en';
import { dictionaries, FALLBACK_KEYS, translate } from '../src/i18n';

const languages = Object.keys(dictionaries) as (keyof typeof dictionaries)[];
const placeholders = (s: string) => (s.match(/\{\w+\}/g) ?? []).sort().join();

describe('localisation', () => {
  it.each(['si', 'ta'] as const)('%s translates every key', lang => {
    for (const key of Object.keys(en)) {
      expect(dictionaries[lang][key as keyof typeof en]).toBeTruthy();
    }
  });

  it.each(languages)(
    '%s translates every key outside the Settings-only list',
    lang => {
      for (const key of Object.keys(en) as (keyof typeof en)[]) {
        if (FALLBACK_KEYS.includes(key)) continue;
        const value = dictionaries[lang][key];
        expect(typeof value).toBe('string');
        expect((value as string).length).toBeGreaterThan(0);
      }
    },
  );

  it.each(languages)('%s keeps the same placeholders as English', lang => {
    for (const [key, value] of Object.entries(dictionaries[lang])) {
      expect(placeholders(value as string)).toBe(
        placeholders(en[key as keyof typeof en]),
      );
    }
  });

  it('falls back to English for missing keys and unknown languages', () => {
    expect(translate('de', 'themeMidnight')).toBe('Midnight Purple');
    expect(translate('xx', 'save')).toBe('Save');
    expect(translate('ja', 'save')).toBe('保存');
  });
});
