import './panels.css';

interface PlayerControlsProps {
  index: number;
  stepCount: number;
  playing: boolean;
  speedMs: number;
  onFirst: () => void;
  onPrev: () => void;
  onToggle: () => void;
  onNext: () => void;
  onLast: () => void;
  onSeek: (index: number) => void;
  onSpeedChange: (speedMs: number) => void;
}

export function PlayerControls({
  index,
  stepCount,
  playing,
  speedMs,
  onFirst,
  onPrev,
  onToggle,
  onNext,
  onLast,
  onSeek,
  onSpeedChange,
}: PlayerControlsProps) {
  return (
    <div className="player-controls">
      <button type="button" aria-label="first step" onClick={onFirst}>⏮</button>
      <button type="button" aria-label="previous step" onClick={onPrev}>◀</button>
      <button type="button" aria-label={playing ? 'pause' : 'play'} onClick={onToggle}>
        {playing ? '⏸' : '▶'}
      </button>
      <button type="button" aria-label="next step" onClick={onNext}>▶|</button>
      <button type="button" aria-label="last step" onClick={onLast}>⏭</button>

      <input
        type="range"
        aria-label="scrub"
        min={0}
        max={Math.max(stepCount - 1, 0)}
        value={index}
        onChange={(event) => onSeek(Number(event.target.value))}
      />

      <span className="player-controls__counter">{`step ${stepCount === 0 ? 0 : index + 1} / ${stepCount}`}</span>

      <select aria-label="speed" value={speedMs} onChange={(event) => onSpeedChange(Number(event.target.value))}>
        <option value={1200}>0.5x</option>
        <option value={600}>1x</option>
        <option value={300}>2x</option>
        <option value={120}>5x</option>
      </select>
    </div>
  );
}
