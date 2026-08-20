import type { ViewPayload } from '../../types';

export function FallbackView({ view }: { view: ViewPayload }) {
  return (
    <div className="fallback-view" style={{ padding: 24, color: '#a0a0a0' }}>
      <p>
        No renderer for view kind <strong>{view.kind}</strong> yet. The step line, message, and variables are still
        shown below.
      </p>
    </div>
  );
}
