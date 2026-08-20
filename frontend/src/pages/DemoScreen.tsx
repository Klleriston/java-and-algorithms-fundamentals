import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { ApiRequestError, fetchDemos, runTrace } from '../api';
import { CodePanel } from '../components/CodePanel';
import { ParameterForm } from '../components/ParameterForm';
import { PlayerControls } from '../components/PlayerControls';
import { VariableTable } from '../components/VariableTable';
import { viewFor } from '../components/views/registry';
import { useTracePlayer } from '../hooks/useTracePlayer';
import type { DemoSummary, ParameterSpec, Trace } from '../types';
import './DemoScreen.css';

function initialValues(specs: ParameterSpec[], search: URLSearchParams): Record<string, string> {
  const values: Record<string, string> = {};
  specs.forEach((spec) => {
    const fromUrl = search.get(spec.name);
    values[spec.name] = fromUrl ?? String(spec.defaultValue ?? '');
  });
  return values;
}

function toParams(specs: ParameterSpec[], values: Record<string, string>): Record<string, unknown> {
  const params: Record<string, unknown> = {};
  specs.forEach((spec) => {
    const raw = values[spec.name] ?? '';
    params[spec.name] =
      spec.type === 'INT_ARRAY'
        ? raw.split(',').map((part) => part.trim()).filter(Boolean).map(Number)
        : spec.type === 'INT' || spec.type === 'LONG'
          ? Number(raw)
          : raw;
  });
  return params;
}

export function DemoScreen() {
  const { id = '' } = useParams();
  const [search, setSearch] = useSearchParams();
  const [demo, setDemo] = useState<DemoSummary | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [trace, setTrace] = useState<Trace | null>(null);
  const [error, setError] = useState<string | undefined>();

  const player = useTracePlayer(trace?.steps.length ?? 0);

  const execute = useCallback(
    async (specs: ParameterSpec[], nextValues: Record<string, string>) => {
      try {
        const result = await runTrace(id, toParams(specs, nextValues));
        setTrace(result);
        setError(undefined);
      } catch (caught) {
        setError(caught instanceof ApiRequestError ? caught.message : 'Request failed');
      }
    },
    [id],
  );

  useEffect(() => {
    let cancelled = false;
    fetchDemos()
      .then((demos) => {
        if (cancelled) return;
        const found = demos.find((candidate) => candidate.id === id) ?? null;
        setDemo(found);
        if (found) {
          const startValues = initialValues(found.parameters, search);
          setValues(startValues);
          void execute(found.parameters, startValues);
        }
      })
      .catch(() => setError('Could not load demos'));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const step = useMemo(() => trace?.steps[player.index], [trace, player.index]);
  const Renderer = viewFor(step?.view.kind ?? '');

  if (!demo) {
    return <p className="demo-screen__loading">{error ?? 'Loading…'}</p>;
  }

  return (
    <div className="demo-screen">
      <header className="demo-screen__header">
        <h1>{demo.title}</h1>
        <p>{demo.description}</p>
      </header>

      <ParameterForm
        specs={demo.parameters}
        values={values}
        error={error}
        onChange={(name, value) => setValues((current) => ({ ...current, [name]: value }))}
        onSubmit={() => {
          const next = new URLSearchParams(search);
          demo.parameters.forEach((spec) => next.set(spec.name, values[spec.name] ?? ''));
          setSearch(next, { replace: true });
          void execute(demo.parameters, values);
        }}
      />

      <div className="demo-screen__split">
        <section className="demo-screen__code">
          <CodePanel source={trace?.sourceCode ?? demo.sourceCode} activeLine={step?.line ?? -1} />
        </section>
        <section className="demo-screen__visual">
          <div className="demo-screen__view">{step ? <Renderer view={step.view} /> : null}</div>
          <div className="demo-screen__detail">
            <p className="demo-screen__message">{step?.message}</p>
            <VariableTable vars={step?.vars ?? {}} />
          </div>
        </section>
      </div>

      <PlayerControls
        index={player.index}
        stepCount={trace?.steps.length ?? 0}
        playing={player.playing}
        speedMs={player.speedMs}
        onFirst={player.first}
        onPrev={player.prev}
        onToggle={player.toggle}
        onNext={player.next}
        onLast={player.last}
        onSeek={player.seek}
        onSpeedChange={player.setSpeed}
      />
    </div>
  );
}
