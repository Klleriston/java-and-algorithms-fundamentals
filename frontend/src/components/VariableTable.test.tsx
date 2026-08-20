import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { I18nProvider, useI18n } from '../i18n/I18nContext';
import { VariableTable } from './VariableTable';

/** Switching through the context, because reading the stored language arrives with the toggle. */
function SwitchToPortuguese() {
  const { setLang } = useI18n();
  return <button onClick={() => setLang('pt')}>pt</button>;
}

function renderTable(vars: Record<string, unknown>) {
  return render(
    <I18nProvider>
      <SwitchToPortuguese />
      <VariableTable vars={vars} />
    </I18nProvider>,
  );
}

describe('VariableTable', () => {
  it('lists one row per variable', () => {
    renderTable({ low: 0, high: 5 });
    expect(screen.getAllByTestId('var-row')).toHaveLength(2);
  });

  it('says so when a step has no variables', () => {
    renderTable({});
    expect(screen.getByText('No variables at this step.')).toBeInTheDocument();
  });

  it('says so in the chosen language', async () => {
    const user = userEvent.setup();
    renderTable({});
    await user.click(screen.getByRole('button', { name: 'pt' }));
    expect(screen.getByText('Nenhuma variável neste passo.')).toBeInTheDocument();
  });
});
