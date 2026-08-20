import type { HashTableViewPayload } from '../../types';
import './HashTableView.css';

export function HashTableView({ view }: { view: HashTableViewPayload }) {
  return (
    <div className="hash-table">
      {view.buckets.map((bucket) => (
        <div
          key={bucket.index}
          className="hash-bucket"
          data-testid={`bucket-${bucket.index}`}
          data-active={bucket.index === view.activeBucket}
        >
          <span className="hash-bucket-index">{bucket.index}</span>
          <div className="hash-chain">
            {bucket.entries.map((entry) => (
              <span key={entry.key} className="hash-entry-wrapper" data-state={entry.state}>
                <span className="hash-entry" data-state={entry.state}>
                  {entry.key}
                </span>
                {entry.value !== null && <span className="hash-entry-value">: {entry.value}</span>}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
