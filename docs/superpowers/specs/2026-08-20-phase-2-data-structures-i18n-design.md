# Phase 2 — Data structures and internationalization

**Date:** 2026-08-20
**Status:** approved
**Supersedes:** section 10 of `2026-08-19-visual-jvm-algorithms-design.md`, which
listed TREE/HASH_TABLE as phase 2, GC as phase 3 and OOP as phase 4. The
architecture, trace pipeline and testing strategy in that document still hold,
except where this one changes them.

## 1. Purpose

Phase 1 delivered the pipeline — a Spring Boot backend that traces a curated
demo step by step, a golden JSON contract, and a React player that replays it.
It shipped with two demos, both over arrays, both narrating in English.

Phase 2 keeps the focus on algorithms and data structures in Java and does
three things: it adds four demos over the structures a Java developer meets
first, it makes the app speak Portuguese and English, and it gives the learner
a way back to the catalog.

## 2. Scope

In scope:

- Four demos: `hash-map-put`, `hash-set-add`, `bst-insert`, `linked-list-insert`
- Three new view kinds: `HASH_TABLE`, `TREE`, `LINKED_LIST`
- Portuguese and English throughout, including step narration
- A back link from a demo to the catalog

Moved out, unchanged in substance:

- OOP (inheritance, polymorphism, encapsulation) becomes phase 3
- The JVM memory model and garbage collection become phase 4

## 3. Keyed narration

### 3.1 The problem this solves

Every sentence the learner reads is currently produced in Java:

```java
tracer.step(8, "a[" + mid + "] = " + a[mid] + " < " + target + ", discard the left half", vars, view);
```

The golden fixtures pin those exact strings, so the JSON contract is written in
English. Translating by rendering sentences on the server would double every
fixture and make the contract depend on a locale — weakening the tests that
caught the `Map.copyOf` ordering defect in phase 1.

### 3.2 The change

The backend stops producing sentences. It produces a key and the values that go
into the sentence; the frontend renders it.

```java
tracer.step(8, "binarySearch.discardLeft",
        Map.of("index", mid, "value", a[mid], "target", target),
        vars, view);
```

`TraceStep` becomes:

```java
public record TraceStep(
        int index,
        int line,
        String messageKey,
        Map<String, Object> messageArgs,
        Map<String, Object> vars,
        ViewPayload view) {

    public TraceStep {
        messageArgs = Collections.unmodifiableMap(new LinkedHashMap<>(messageArgs));
        vars = Collections.unmodifiableMap(new LinkedHashMap<>(vars));
    }
}
```

Both maps keep the `LinkedHashMap` defensive copy from phase 1. `Map.copyOf`
randomizes iteration order per JVM run, which makes golden fixtures fail
intermittently; the same trap applies to `messageArgs`.

`Trace.title` becomes `titleKey`. On the `Demo` interface, `title()` and
`description()` become `titleKey()` and `descriptionKey()`. On `ParameterSpec`,
`label` becomes `labelKey`. After this change no user-visible sentence exists
anywhere in `backend/`.

### 3.3 Key and argument naming

Keys are `<demoId in camelCase>.<event>`: `binarySearch.discardLeft`,
`bubbleSort.swap`, `hashMapPut.insert`. Demo metadata uses
`demo.<demoId in camelCase>.title` and `.description`; parameters use
`demo.<demoId in camelCase>.param.<name>`; shared UI strings use `ui.<name>`.

Argument names are the domain word, not the variable name: `index`, `value`,
`target`, `bucket`, `key`, `position`. The same concept keeps the same argument
name across demos so translators see a small vocabulary.

A backend test asserts every emitted key matches
`^[a-z][A-Za-z0-9]*(\.[a-zA-Z0-9]+)+$` — no spaces, no punctuation, no accented
characters. A key that is really a sentence fails the build.

## 4. Message catalogs

Two files, `frontend/src/i18n/pt.json` and `frontend/src/i18n/en.json`, each a
flat object from key to template:

```json
{
  "binarySearch.discardLeft": "a[{index}] = {value} < {target}, descarta a metade da esquerda",
  "ui.back": "Demos",
  "ui.run": "Executar"
}
```

The formatter is local to the project, roughly forty lines:

```ts
format(template: string, args: Record<string, unknown>): string
```

It replaces each `{name}` with `String(args[name])`. There are no plurals and no
gendered forms in this vocabulary, so `react-i18next` would add a dependency and
a configuration surface for what is string substitution. If a key is missing
from the catalog the formatter returns the key itself, which is ugly on screen
and therefore easy to spot — but the tests in section 9 are what actually
prevent it from reaching the screen.

Language selection: the initial language comes from `navigator.language`
(anything starting with `pt` gives Portuguese, everything else English), a
toggle in the header switches it, and the choice persists in `localStorage`
under `lang`. Switching updates `document.documentElement.lang` and re-renders
the current step without re-running the trace — the trace carries no language,
so there is nothing to fetch again.

## 5. New view kinds

`ViewPayload` gains three permitted implementations. Each is a record with a
defensive copy, mirroring `ArrayView`.

```java
public record HashTableView(
        int capacity,
        List<Bucket> buckets,
        Integer activeBucket,
        String outcome) implements ViewPayload {

    public record Bucket(int index, List<Entry> entries) {}

    public record Entry(int key, Integer value, String state) {
        public static final String NORMAL = "NORMAL";
        public static final String PROBED = "PROBED";
        public static final String MATCHED = "MATCHED";
        public static final String INSERTED = "INSERTED";
    }

    public static final String INSERTED = "INSERTED";
    public static final String UPDATED = "UPDATED";
    public static final String DUPLICATE = "DUPLICATE";
}
```

`outcome` is `null` while the algorithm is still walking, then `INSERTED`,
`UPDATED` or `DUPLICATE`. `buckets` always has `capacity` entries, including the
empty ones, so the renderer draws a table of fixed width. `value` is null for
set entries, which have keys only.

```java
public record TreeView(
        List<Node> nodes,
        Integer activeNode,
        List<Integer> path,
        Integer insertedNode) implements ViewPayload {

    public record Node(int id, int value, Integer left, Integer right) {}
}
```

Nodes are a flat list with stable ids and child references, so the JSON stays
flat. The renderer computes the layout: horizontal position from the in-order
rank, vertical from the depth. `path` is the ids visited so far, which the
renderer draws as the descent.

```java
public record LinkedListView(
        List<Node> nodes,
        Integer cursor,
        List<Link> changedLinks,
        Integer newNode) implements ViewPayload {

    public record Node(int id, int value, Integer next) {}

    public record Link(Integer from, Integer to) {}
}
```

`changedLinks` is what makes insertion legible: the two pointers being rewired
are highlighted at the step that rewires them.

The TypeScript mirrors go in `frontend/src/types.ts` as members of the
`ViewPayload` union, and each kind gets one entry in
`frontend/src/components/views/registry.tsx`. An unknown kind keeps falling back
to the existing placeholder renderer.

## 6. The four demos

Each traces one operation over a structure built from the input. Keys are int,
non-negative, so a didactic `key % capacity` never goes negative.

### 6.1 `hash-map-put`

Parameters: `keys` (int array, max 32, default `[5, 21, 37, 8]`), `key`
(int, min 0, default 13), `value` (int, default 99). Capacity is fixed at 8 and
visible in the displayed source.

Those defaults are chosen so the first thing the learner sees is the
interesting case: 5, 21 and 37 all land in bucket 5, and inserting 13 lands
there too, so the trace walks the whole chain before inserting. Typing 21
instead reaches the update path, and 8 reaches a bucket with a single entry.

```java
public void put(int key, int value) {
    int index = key % table.length;
    Node node = table[index];
    while (node != null) {
        if (node.key == key) {
            node.value = value;
            return;
        }
        node = node.next;
    }
    table[index] = new Node(key, value, table[index]);
    size++;
}
```

Steps: `hashMapPut.bucket` (args `key`, `bucket`, `capacity`),
`hashMapPut.compare` (`key`, `other`), `hashMapPut.update` (`key`, `value`),
`hashMapPut.insert` (`key`, `bucket`).

**This is not `java.util.HashMap`, and the app says so.** The real one spreads
the hash with `h ^ (h >>> 16)`, resizes at a load factor of 0.75 and turns long
chains into red-black trees. This demo does none of that: `key % capacity`, a
fixed table, chains only. The demo description carries that sentence, in both
languages, so nobody leaves believing the JDK works this way.

### 6.2 `hash-set-add`

Parameters: `values` (int array, max 32, default `[5, 21, 37, 8]`), `value`
(int, min 0, default 37). Same table drawing, `value` null on every entry.

Steps: `hashSetAdd.bucket`, `hashSetAdd.compare`, `hashSetAdd.duplicate`,
`hashSetAdd.insert`. The duplicate step is the point of the demo: the value
lands in the right bucket, meets an equal one, and is refused.

### 6.3 `bst-insert`

Parameters: `values` (int array, max 31, default `[50, 30, 70, 20, 40, 60]`,
inserted in order to build the tree), `value` (int, default 35).

Steps: `bstInsert.compareLess` and `bstInsert.compareGreater` (args `value`,
`node`) for each comparison on the way down, then one of
`bstInsert.attachLeft`, `bstInsert.attachRight`, `bstInsert.attachRoot`
(args `value`, `parent` where applicable), or `bstInsert.duplicate` when the
value is already there and the insert is a no-op.

### 6.4 `linked-list-insert`

Parameters: `values` (int array, max 32, default `[10, 20, 30, 40]`),
`position` (int, min 0, default 2), `value` (int, default 25).

Steps: `linkedListInsert.walk` (args `index`, `value` of the node under the
cursor) once per hop, `linkedListInsert.arrive` (`position`), then
`linkedListInsert.linkNew` (`value`, `next`) and
`linkedListInsert.relinkPrev` (`prev`, `value`) — or
`linkedListInsert.insertHead` when the position is 0, where there is no previous
node to rewire.

`position` greater than the list length is rejected as invalid input, with
`field: "position"`, following the phase 1 error contract.

### 6.5 Highlighted lines

All four follow the convention fixed in section 4.1 of the phase 1 spec: the
highlighted line narrates the step, and matches the state drawn beside it. A
step reporting a decision highlights the condition just evaluated; a step
reporting a mutation highlights the last line of that mutation.

## 7. Back link

The demo screen gets a link to `/` in its header, labelled `ui.back`. It renders
whether or not a trace is loaded, including when the run failed — the failed
state is exactly when the learner wants to leave.

## 8. Validation and limits

Unchanged from phase 1: parameters are validated against their `ParameterSpec`,
invalid input returns HTTP 400 with `error`, `message` and `field`, and the step
cap and the five-second timeout still apply. The new demos add three rules:
hash keys must be non-negative, `position` must be within the list, and the tree
and hash inputs cap at the array lengths given in section 6.

The error body changes shape along with the trace. `message` held an English
sentence; it becomes a key and its arguments, resolved by the frontend exactly
like a step message:

```json
{
  "error": "INVALID_INPUT",
  "messageKey": "error.arrayNotSorted",
  "messageArgs": {},
  "field": "array"
}
```

`error` and `field` are machine-facing identifiers and stay as they are — they
are not shown to the learner. Every `error.*` key is covered by the same catalog
tests as the step keys, so an untranslated validation message fails the build
too.

## 9. Testing

Backend:

- One test class per demo, asserting the step keys, the arguments and the
  highlighted lines — the line convention is pinned by test, as in phase 1
- One golden fixture per demo in `frontend/src/fixtures/`, so six in total:
  the two from phase 1, regenerated in keyed form, plus the four new ones
- The key-format test from section 3.3, run over every demo in the registry

Frontend:

- One test per new renderer, driven by the real fixture rather than a handmade
  object
- Formatter tests: substitution, repeated placeholder, missing key
- **Catalog coverage:** a test walks every fixture, collects every `messageKey`,
  and fails if any is absent from `pt.json` or from `en.json`. A forgotten
  translation is a red build, not a raw key on someone's screen.
- **Placeholder coverage:** for every fixture step, every `{name}` in the
  matching template — in both languages — must be present in that step's
  `messageArgs`. This catches the subtler bug: a translation that mentions an
  argument the backend never sends, which would render a literal `{target}`.
- Language toggle: switching re-renders the current step in the other language
  without a new request
- Back link: clicking it lands on the catalog

## 10. Delivery order

The first item is one issue, not two, even though it spans both sides. The
fixtures are the contract between them: splitting it would leave `dev` with a
backend emitting keys and a frontend rendering sentences.

1. Keyed narration end to end — trace model, both existing demos, catalogs,
   formatter, regenerated fixtures, key-format and coverage tests
2. Language toggle, persistence, `document.documentElement.lang`
3. Back link
4. `hash-map-put` — `HASH_TABLE` payload, renderer, registry entry, demo
5. `hash-set-add` — reuses `HASH_TABLE`, so it follows 4
6. `bst-insert` — `TREE` payload and renderer
7. `linked-list-insert` — `LINKED_LIST` payload and renderer

Items 6 and 7 touch disjoint files and can run in parallel. Item 1 blocks
everything: any demo written before it lands would be written in English and
rewritten afterwards.

## 11. Non-goals

- No resize, no `h ^ (h >>> 16)`, no treeify — the hash demos stay didactic,
  and say where they differ
- No `get`, `contains` or `remove` demos; one operation per structure
- No third language, and no server-side rendering of any sentence
- No arbitrary code execution; parameters remain the only input
- OOP and the JVM memory model stay out until phases 3 and 4
