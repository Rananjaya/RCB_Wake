import React, {createContext, useCallback, useContext} from 'react';
import {useSettingsStore} from '../storage/settingsStore';
import {en, type StringKey, type Strings} from './en';
import {si} from './si';
import {ta} from './ta';

export type Language = 'en' | 'si' | 'ta';

export const dictionaries: Record<Language, Strings> = {en, si, ta};

export const languageNames: Record<Language, string> = {en: 'English', si: 'සිංහල', ta: 'தமிழ்'};

export type TranslateParams = Record<string, string | number>;

export function translate(language: string, key: StringKey, params?: TranslateParams): string {
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

export function LanguageOverride({language, children}: {language: Language; children: React.ReactNode}) {
  return React.createElement(LanguageOverrideContext.Provider, {value: language}, children);
}

export function useT(): Translate {
  const selected = useSettingsStore(s => s.settings.language);
  const override = useContext(LanguageOverrideContext);
  const language = override ?? selected;
  return useCallback((key, params) => translate(language, key, params), [language]);
}

export type {StringKey};
