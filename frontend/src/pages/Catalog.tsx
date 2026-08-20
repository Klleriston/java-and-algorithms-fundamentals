import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchDemos } from '../api';
import type { DemoSummary } from '../types';

export function Catalog() {
  const [demos, setDemos] = useState<DemoSummary[]>([]);
  const [error, setError] = useState<string | undefined>();

  useEffect(() => {
    fetchDemos().then(setDemos).catch(() => setError('Could not load demos'));
  }, []);

  if (error) {
    return <p role="alert">{error}</p>;
  }

  const categories = [...new Set(demos.map((demo) => demo.category))];

  return (
    <main className="catalog">
      <h1>Java and Algorithms Fundamentals</h1>
      {categories.map((category) => (
        <section key={category}>
          <h2>{category}</h2>
          <ul>
            {demos
              .filter((demo) => demo.category === category)
              .map((demo) => (
                <li key={demo.id}>
                  <Link to={`/demo/${demo.id}`}>
                    {demo.title} — {demo.description}
                  </Link>
                </li>
              ))}
          </ul>
        </section>
      ))}
    </main>
  );
}
