import './panels.css';

export function VariableTable({ vars }: { vars: Record<string, unknown> }) {
  const entries = Object.entries(vars);
  if (entries.length === 0) {
    return <p className="variable-table__empty">No variables at this step.</p>;
  }
  return (
    <table className="variable-table">
      <tbody>
        {entries.map(([name, value]) => (
          <tr key={name} data-testid="var-row">
            <th scope="row">{name}</th>
            <td>{String(value)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
