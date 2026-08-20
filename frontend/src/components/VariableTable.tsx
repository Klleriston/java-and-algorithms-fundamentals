import { useI18n } from '../i18n/I18nContext';
import './panels.css';

export function VariableTable({ vars }: { vars: Record<string, unknown> }) {
  const { t } = useI18n();
  const entries = Object.entries(vars);
  if (entries.length === 0) {
    return <p className="variable-table__empty">{t('ui.noVariables')}</p>;
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
