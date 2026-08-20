import { useI18n, type Lang } from '../i18n/I18nContext';

const languages: { code: Lang; label: string }[] = [
  { code: 'pt', label: 'Português' },
  { code: 'en', label: 'English' },
];

export function LanguageToggle() {
  const { lang, setLang, t } = useI18n();

  return (
    <div className="language-toggle" role="group" aria-label={t('ui.language')}>
      {languages.map((language) => (
        <button
          key={language.code}
          type="button"
          aria-pressed={lang === language.code}
          onClick={() => setLang(language.code)}
        >
          {language.label}
        </button>
      ))}
    </div>
  );
}
