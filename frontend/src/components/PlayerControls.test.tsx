import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { I18nProvider } from '../i18n/I18nContext';
import { PlayerControls } from './PlayerControls';

function setup(overrides: Partial<React.ComponentProps<typeof PlayerControls>> = {}) {
  const props = {
    index: 1,
    stepCount: 5,
    playing: false,
    speedMs: 600,
    onFirst: vi.fn(),
    onPrev: vi.fn(),
    onToggle: vi.fn(),
    onNext: vi.fn(),
    onLast: vi.fn(),
    onSeek: vi.fn(),
    onSpeedChange: vi.fn(),
    ...overrides,
  };
  render(
    <I18nProvider>
      <PlayerControls {...props} />
    </I18nProvider>,
  );
  return props;
}

describe('PlayerControls', () => {
  it('shows the current position', () => {
    setup();
    expect(screen.getByText('step 2 / 5')).toBeInTheDocument();
  });

  it('calls the matching handler for each button', async () => {
    const props = setup();
    const user = userEvent.setup();

    await user.click(screen.getByLabelText('first step'));
    await user.click(screen.getByLabelText('previous step'));
    await user.click(screen.getByLabelText('play'));
    await user.click(screen.getByLabelText('next step'));
    await user.click(screen.getByLabelText('last step'));

    expect(props.onFirst).toHaveBeenCalledOnce();
    expect(props.onPrev).toHaveBeenCalledOnce();
    expect(props.onToggle).toHaveBeenCalledOnce();
    expect(props.onNext).toHaveBeenCalledOnce();
    expect(props.onLast).toHaveBeenCalledOnce();
  });

  it('labels the toggle as pause while playing', () => {
    setup({ playing: true });
    expect(screen.getByLabelText('pause')).toBeInTheDocument();
  });
});
