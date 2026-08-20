import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { I18nProvider } from '../i18n/I18nContext';
import { LanguageToggle } from './LanguageToggle';

describe('LanguageToggle', () => {
  beforeEach(() => localStorage.clear());

  it('switches the rendered language', async () => {
    localStorage.setItem('lang', 'en');
    const user = userEvent.setup();
    render(<I18nProvider><LanguageToggle /></I18nProvider>);

    await user.click(screen.getByRole('button', { name: 'Português' }));
    expect(screen.getByRole('button', { name: 'Português' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'false');
  });
});
