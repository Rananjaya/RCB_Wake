import React, { createContext, useCallback, useContext } from 'react';
import { useSettingsStore } from '../storage/settingsStore';
import { de } from './de';
import { en, type StringKey, type Translation } from './en';
import { es } from './es';
import { fr } from './fr';
import { hi } from './hi';
import { ja } from './ja';
import { pt } from './pt';
import { ru } from './ru';
import { si } from './si';
import { ta } from './ta';
import { zh } from './zh';

export type Language =
  | 'en'
  | 'si'
  | 'ta'
  | 'de'
  | 'ru'
  | 'es'
  | 'fr'
  | 'pt'
  | 'hi'
  | 'zh'
  | 'ja';

export const dictionaries: Record<Language, Translation> = {
  en,
  si,
  ta,
  de,
  ru,
  es,
  fr,
  pt,
  hi,
  zh,
  ja,
};

export const languageNames: Record<Language, string> = {
  en: 'English',
  si: 'සිංහල',
  ta: 'தமிழ்',
  de: 'Deutsch',
  es: 'Español',
  fr: 'Français',
  pt: 'Português',
  ru: 'Русский',
  hi: 'हिन्दी',
  zh: '中文（简体）',
  ja: '日本語',
};

/**
 * Keys a language may leave out; English is shown instead. These are only used on the Settings
 * screen (always English) or by the NFC challenge (on hold).
 */
export const FALLBACK_KEYS: StringKey[] = [
  'formatSystem',
  'appIcon',
  'appIconHint',
  'agreementTitle',
  'agreementIntro',
  'agreementAgree',
  'agreementExit',
  'agreementVersion',
  'privacyPointLocal',
  'privacyPointOffline',
  'privacyPointTracking',
  'privacyPointBackup',
  'privacyPointOptional',
  'privacyPointPermissions',
  'privacyPointLicense',
  'showTutorial',
  'privacyBody',
  'privacyPolicy',
  'readPrivacyPolicy',
  'openSourceLicenses',
  'openSourceIntro',

  'widgetCalendar',
  'widgetCalendarToggle',
  'widgetCalendarHint',
  'widgetCalendarNone',
  'widgetWeather',
  'weatherToggle',
  'weatherHint',
  'weatherCity',
  'weatherUseLocation',
  'weatherUseLocationHint',
  'weatherFallbackCity',
  'currentLocation',
  'temperatureUnit',
  'celsius',
  'fahrenheit',
  'weatherNow',
  'weatherSent',
  'weatherMatched',
  'weatherUpdated',
  'weatherRefreshNow',
  'placeSearching',
  'placeSearchHint',
  'weatherNotYet',
  'weatherAttribution',
  'widgetSection',
  'widgetAdd',
  'widgetClockName',
  'widgetGlanceName',
  'widgetTextColor',
  'widgetTextColorHint',
  'widgetColorWhite',
  'widgetColorMaterial',
  'widgetColorAccent',
  'widgetShowPlace',
  'widgetShowPlaceHint',
  'widgetShowHeadphones',
  'widgetShowHeadphonesHint',
  'weatherRefresh',
  'hoursShort',
  'weatherProvider',
  'providerOpenMeteo',
  'providerOpenWeatherMap',
  'owmApiKey',
  'owmApiKeyHint',
  'owmApiKeyInvalid',
  'owmSave',
  'owmNoWeather',
  'owmAttribution',
  'sleepReminder',
  'sleepReminderToggle',
  'sleepReminderHint',
  'sleepGoal',
  'sleepReminderLead',
  'atBedtime',
  'sleepReminderPreview',
  'sleepReminderNone',
  'appearance',
  'theme',
  'themeMidnight',
  'themePink',
  'accentColor',
  'accentDefault',
  'timeFormat',
  'format12',
  'format24',
  'language',
  'alarmDefaults',
  'defaultSound',
  'defaultSnooze',
  'permissions',
  'exactAlarms',
  'notifications',
  'fullScreenAlerts',
  'granted',
  'notGranted',
  'reliabilityNote',
  'nfcTags',
  'nfcTagsHint',
  'nfcAddTag',
  'nfcHoldNear',
  'nfcTagAdded',
  'nfcScanTimeout',
  'nfcOff',
  'challengeType',
  'challengeMath',
  'challengeNfc',
  'challengeNfcNeedsTag',
  'nfcChallengeTitle',
  'nfcChallengeBody',
  'nfcChallengeOff',
  'nfcTurnOn',
];

export type TranslateParams = Record<string, string | number>;

export function translate(
  language: string,
  key: StringKey,
  params?: TranslateParams,
): string {
  const dict = dictionaries[language as Language] ?? en;
  const template = dict[key] ?? en[key];
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (match, name: string) =>
    name in params ? String(params[name]) : match,
  );
}

export type Translate = (key: StringKey, params?: TranslateParams) => string;

/**
 * Forces a fixed language for a subtree. The Settings screen is always shown in English so the
 * language option can be found again whatever language is selected.
 */
const LanguageOverrideContext = createContext<Language | null>(null);

export function LanguageOverride({
  language,
  children,
}: {
  language: Language;
  children: React.ReactNode;
}) {
  return React.createElement(
    LanguageOverrideContext.Provider,
    { value: language },
    children,
  );
}

export function useT(): Translate {
  const selected = useSettingsStore(s => s.settings.language);
  const override = useContext(LanguageOverrideContext);
  const language = override ?? selected;
  return useCallback(
    (key, params) => translate(language, key, params),
    [language],
  );
}

export type { StringKey };
