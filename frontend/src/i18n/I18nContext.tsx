import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { format } from './format';
import en from './en.json';
import pt from './pt.json';

export type Lang = 'pt' | 'en';

const catalogs: Record<Lang, Record<string, string>> = { pt, en };

interface I18n {
  lang: Lang;
  setLang: (lang: Lang) => void;
  t: (key: string, args?: Record<string, unknown>) => string;
}

const I18nContext = createContext<I18n | undefined>(undefined);

export function detectLang(): Lang {
  return navigator.language.toLowerCase().startsWith('pt') ? 'pt' : 'en';
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLang] = useState<Lang>(detectLang);

  const t = useCallback(
    (key: string, args: Record<string, unknown> = {}) => {
      const template = catalogs[lang][key];
      return template === undefined ? key : format(template, args);
    },
    [lang],
  );

  const value = useMemo(() => ({ lang, setLang, t }), [lang, t]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18n {
  const context = useContext(I18nContext);
  if (context === undefined) {
    throw new Error('useI18n must be used inside I18nProvider');
  }
  return context;
}
