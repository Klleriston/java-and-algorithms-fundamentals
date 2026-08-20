import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { CodePanel } from './CodePanel';

const SOURCE = ['int low = 0;', 'int high = a.length - 1;', 'while (low <= high) {'].join('\n');

describe('CodePanel', () => {
  it('renders every line with a gutter number', () => {
    render(<CodePanel source={SOURCE} activeLine={1} />);
    expect(screen.getByText('int high = a.length - 1;')).toBeInTheDocument();
    expect(screen.getAllByTestId('gutter').map((node) => node.textContent)).toEqual(['1', '2', '3']);
  });

  it('marks only the active line', () => {
    render(<CodePanel source={SOURCE} activeLine={2} />);
    const active = screen.getAllByTestId('code-line').filter((node) => node.dataset.active === 'true');
    expect(active).toHaveLength(1);
    expect(active[0]).toHaveTextContent('int high = a.length - 1;');
  });

  it('marks no line when activeLine is out of range', () => {
    render(<CodePanel source={SOURCE} activeLine={99} />);
    expect(screen.getAllByTestId('code-line').filter((node) => node.dataset.active === 'true')).toHaveLength(0);
  });
});
