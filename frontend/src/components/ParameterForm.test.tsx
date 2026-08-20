import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { I18nProvider } from '../i18n/I18nContext';
import { ParameterForm } from './ParameterForm';
import type { ParameterSpec } from '../types';

const specs: ParameterSpec[] = [
  { name: 'array', type: 'INT_ARRAY', labelKey: 'Sorted array', required: true, maxLength: 512 },
  { name: 'target', type: 'INT', labelKey: 'Target value', required: true, min: -1000, max: 1000 },
];

function renderForm(props: Partial<React.ComponentProps<typeof ParameterForm>> = {}) {
  return render(
    <I18nProvider>
      <ParameterForm
        specs={specs}
        values={{ array: '2,5,8', target: '5' }}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        {...props}
      />
    </I18nProvider>,
  );
}

describe('ParameterForm', () => {
  it('renders one labelled input per spec', () => {
    renderForm({ values: { array: '2,5,8', target: '5' } });
    expect(screen.getByLabelText('Sorted array')).toHaveValue('2,5,8');
    expect(screen.getByLabelText('Target value')).toHaveValue('5');
  });

  it('submits parsed values', async () => {
    const onSubmit = vi.fn();
    renderForm({ values: { array: '2, 5, 8', target: '5' }, onSubmit });

    await userEvent.setup().click(screen.getByRole('button', { name: 'Run' }));

    expect(onSubmit).toHaveBeenCalledWith({ array: [2, 5, 8], target: 5 });
  });

  it('shows an error message when given one', () => {
    renderForm({ values: { array: '', target: '' }, error: 'array must be sorted ascending' });
    expect(screen.getByRole('alert')).toHaveTextContent('array must be sorted ascending');
  });
});
