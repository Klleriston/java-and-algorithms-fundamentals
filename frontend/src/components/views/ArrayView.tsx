import type { ArrayViewPayload } from '../../types';
import './ArrayView.css';

interface ArrayViewProps {
  view: ArrayViewPayload;
}

function stateFor(index: number, ranges: ArrayViewPayload['ranges']): string {
  const range = ranges.find((candidate) => index >= candidate.from && index <= candidate.to);
  return range ? range.state : 'default';
}

export function ArrayView({ view }: ArrayViewProps) {
  const pointersByIndex = new Map<number, string[]>();
  Object.entries(view.pointers).forEach(([name, index]) => {
    pointersByIndex.set(index, [...(pointersByIndex.get(index) ?? []), name]);
  });

  return (
    <div className="array-view">
      <div className="array-view__cells">
        {view.items.map((item, index) => (
          <div key={index} className="array-view__column">
            <div
              data-testid="cell"
              className="array-view__cell"
              data-state={stateFor(index, view.ranges)}
              data-swapped={view.swapped.includes(index)}
            >
              {item}
            </div>
            <div className="array-view__pointers">
              {(pointersByIndex.get(index) ?? []).map((name) => (
                <span key={name} className="array-view__pointer">
                  {name}
                </span>
              ))}
            </div>
            <div className="array-view__index">{index}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
