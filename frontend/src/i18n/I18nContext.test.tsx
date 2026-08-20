import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { I18nProvider, useI18n } from './I18nContext';

function Probe() {
  const { lang, setLang, t } = useI18n();
  return (
    <div>
      <span>{t('ui.run')}</span>
      <span>lang={lang}</span>
      <button onClick={() => setLang(lang === 'pt' ? 'en' : 'pt')}>switch</button>
    </div>
  );
}

describe('I18nProvider', () => {
  beforeEach(() => localStorage.clear());

  it('starts in the language stored from a previous visit', () => {
    localStorage.setItem('lang', 'pt');
    render(<I18nProvider><Probe /></I18nProvider>);
    expect(screen.getByText('Executar')).toBeInTheDocument();
  });

  it('remembers the language across a remount', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<I18nProvider><Probe /></I18nProvider>);
    await user.click(screen.getByRole('button', { name: 'switch' }));
    const chosen = screen.getByText(/lang=/).textContent;
    unmount();

    render(<I18nProvider><Probe /></I18nProvider>);
    expect(screen.getByText(/lang=/).textContent).toBe(chosen);
  });

  it('sets the document language so screen readers follow', async () => {
    const user = userEvent.setup();
    render(<I18nProvider><Probe /></I18nProvider>);
    await user.click(screen.getByRole('button', { name: 'switch' }));
    expect(document.documentElement.lang).toBe(screen.getByText(/lang=/).textContent?.replace('lang=', ''));
  });
});
