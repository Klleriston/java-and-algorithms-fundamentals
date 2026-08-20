import type { FormEvent } from 'react';
import type { ParameterSpec } from '../types';
import './panels.css';

interface ParameterFormProps {
  specs: ParameterSpec[];
  values: Record<string, string>;
  onChange: (name: string, value: string) => void;
  onSubmit: (params: Record<string, unknown>) => void;
  error?: string;
}

function parse(spec: ParameterSpec, raw: string): unknown {
  switch (spec.type) {
    case 'INT_ARRAY':
      return raw
        .split(',')
        .map((part) => part.trim())
        .filter((part) => part.length > 0)
        .map(Number);
    case 'INT':
    case 'LONG':
      return Number(raw);
    default:
      return raw;
  }
}

export function ParameterForm({ specs, values, onChange, onSubmit, error }: ParameterFormProps) {
  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const params: Record<string, unknown> = {};
    specs.forEach((spec) => {
      params[spec.name] = parse(spec, values[spec.name] ?? '');
    });
    onSubmit(params);
  }

  return (
    <form className="parameter-form" onSubmit={handleSubmit}>
      {specs.map((spec) => (
        <label key={spec.name} className="parameter-form__field">
          <span>{spec.label}</span>
          {spec.type === 'ENUM' ? (
            <select value={values[spec.name] ?? ''} onChange={(event) => onChange(spec.name, event.target.value)}>
              {(spec.options ?? []).map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          ) : (
            <input
              type="text"
              value={values[spec.name] ?? ''}
              onChange={(event) => onChange(spec.name, event.target.value)}
            />
          )}
        </label>
      ))}

      <button type="submit">Run</button>

      {error ? (
        <p role="alert" className="parameter-form__error">
          {error}
        </p>
      ) : null}
    </form>
  );
}
