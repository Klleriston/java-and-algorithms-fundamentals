# Java and Algorithms Fundamentals

An interactive web app that shows Java fundamentals running step by step:
algorithms, data structures, the JVM memory model, and garbage collection.

## Running locally

Requirements: JDK 21, Maven 3.9+, Node 20+.

Each block runs from the repository root, in its own terminal.

```bash
# terminal 1 — backend, on http://localhost:8080
cd backend && mvn spring-boot:run
```

```bash
# terminal 2 — frontend, on http://localhost:5173 (proxies /api to the backend)
cd frontend && npm install && npm run dev
```

Open http://localhost:5173 and pick a demo.

## Tests

```bash
cd backend && mvn -B verify
cd ../frontend && npm test
```

The backend golden tests compare their output against
`frontend/src/fixtures/*.json`, the same files the frontend tests consume, so a
backend change that alters the trace JSON fails the backend build. Once the new
JSON is the intended one, regenerate the fixtures with:

```bash
cd backend && mvn -B test -Dtest=GoldenTraceTest -Dgolden.update=true
```

## Documentation

- Design: `docs/superpowers/specs/2026-08-19-visual-jvm-algorithms-design.md`
- Phase 1 plan: `docs/superpowers/plans/2026-08-19-phase-1-array-pipeline.md`

## Adding a demo

1. Implement `Demo` in `backend/src/main/java/dev/klleriston/fundamentals/core/`
   and annotate it with `@Component`. It registers itself.
2. If it needs a new visual form, add a `ViewPayload` implementation, then a
   renderer in `frontend/src/components/views/` and one entry in
   `frontend/src/components/views/registry.tsx`.
3. Add a golden test case so the JSON contract is locked.
