import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchDemos } from '../api';
import { useI18n } from '../i18n/I18nContext';
import type { DemoSummary } from '../types';

export function Catalog() {
  const { t } = useI18n();
  const [demos, setDemos] = useState<DemoSummary[]>([]);
  const [error, setError] = useState<string | undefined>();

  useEffect(() => {
    fetchDemos().then(setDemos).catch(() => setError(t('error.couldNotLoadDemos')));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (error) {
    return <p role="alert">{error}</p>;
  }

  const categories = [...new Set(demos.map((demo) => demo.category))];

  return (
    <main className="catalog">
      <h1>{t('ui.appTitle')}</h1>
      {categories.map((category) => (
        <section key={category}>
          <h2>{t(`category.${category}`)}</h2>
          <ul>
            {demos
              .filter((demo) => demo.category === category)
              .map((demo) => (
                <li key={demo.id}>
                  <Link to={`/demo/${demo.id}`}>
                    {t(demo.titleKey)} — {t(demo.descriptionKey)}
                  </Link>
                </li>
              ))}
          </ul>
        </section>
      ))}
    </main>
  );
}
