import './CodePanel.css';

interface CodePanelProps {
  source: string;
  activeLine: number;
}

export function CodePanel({ source, activeLine }: CodePanelProps) {
  const lines = source.split('\n');

  return (
    <pre className="code-panel">
      {lines.map((line, position) => {
        const lineNumber = position + 1;
        const active = lineNumber === activeLine;
        return (
          <div
            key={lineNumber}
            data-testid="code-line"
            data-active={active}
            className={active ? 'code-line code-line--active' : 'code-line'}
          >
            <span className="code-line__gutter" data-testid="gutter">
              {lineNumber}
            </span>
            <code className="code-line__text">{line}</code>
          </div>
        );
      })}
    </pre>
  );
}
