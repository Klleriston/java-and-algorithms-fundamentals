import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ParameterForm } from './ParameterForm';
import type { ParameterSpec } from '../types';

const specs: ParameterSpec[] = [
  { name: 'array', type: 'INT_ARRAY', label: 'Sorted array', required: true, maxLength: 512 },
  { name: 'target', type: 'INT', label: 'Target value', required: true, min: -1000, max: 1000 },
];

describe('ParameterForm', () => {
  it('renders one labelled input per spec', () => {
    render(<ParameterForm specs={specs} values={{ array: '2,5,8', target: '5' }} onChange={vi.fn()} onSubmit={vi.fn()} />);
    expect(screen.getByLabelText('Sorted array')).toHaveValue('2,5,8');
    expect(screen.getByLabelText('Target value')).toHaveValue('5');
  });

  it('submits parsed values', async () => {
    const onSubmit = vi.fn();
    render(<ParameterForm specs={specs} values={{ array: '2, 5, 8', target: '5' }} onChange={vi.fn()} onSubmit={onSubmit} />);

    await userEvent.setup().click(screen.getByRole('button', { name: 'Run' }));

    expect(onSubmit).toHaveBeenCalledWith({ array: [2, 5, 8], target: 5 });
  });

  it('shows an error message when given one', () => {
    render(
      <ParameterForm specs={specs} values={{ array: '', target: '' }} onChange={vi.fn()} onSubmit={vi.fn()} error="array must be sorted ascending" />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('array must be sorted ascending');
  });
});
