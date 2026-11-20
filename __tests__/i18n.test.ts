import { en } from '../src/i18n/en';
import { dictionaries, translate } from '../src/i18n';

describe('localisation', () => {
  it.each(Object.keys(dictionaries))(
    '%s has every key with a non-empty value',
    lang => {
      const dict = dictionaries[lang as keyof typeof dictionaries] as Record<
        string,
        string
      >;
      for (const key of Object.keys(en)) {
        expect(typeof dict[key]).toBe('string');
        expect(dict[key].length).toBeGreaterThan(0);
      }
    },
  );

  it.each(Object.keys(dictionaries))(
    '%s keeps the same placeholders as English',
    lang => {
      const dict = dictionaries[lang as keyof typeof dictionaries] as Record<
        string,
        string
      >;
      const placeholders = (s: string) =>
        (s.match(/\{\w+\}/g) ?? []).sort().join();
      for (const [key, value] of Object.entries(en))
        expect(placeholders(dict[key])).toBe(placeholders(value));
    },
  );

  it('interpolates parameters and falls back to English', () => {
    expect(translate('en', 'nextAlarmIn', { duration: '5 min' })).toBe(
      'Next alarm in 5 min',
    );
    expect(translate('xx', 'save')).toBe('Save');
  });
});
