# Phase 2 — Data Structures and Internationalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add four data structure demos and make every sentence in the app render in Portuguese or English.

**Architecture:** The backend stops producing sentences. Each trace step, demo title, parameter label and validation error carries a message key plus the arguments that fill it, and the React app renders the sentence from a catalog. Three new `ViewPayload` records — `HASH_TABLE`, `TREE`, `LINKED_LIST` — each get one renderer and one registry entry, following the `ARRAY` pattern from phase 1.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Maven, JUnit 5, AssertJ, MockMvc; React 18, TypeScript 5 strict, Vite 5, Vitest, Testing Library.

**Spec:** `docs/superpowers/specs/2026-08-20-phase-2-data-structures-i18n-design.md`

## Global Constraints

- No Jackson annotations in the `dev.klleriston.fundamentals.trace` package. Serialization details live in `api/JacksonConfig.java`.
- Defensive copies of maps use `Collections.unmodifiableMap(new LinkedHashMap<>(map))`, never `Map.copyOf`. `Map.copyOf` randomizes iteration order per JVM run and makes golden fixtures fail intermittently.
- Golden fixtures live in `frontend/src/fixtures/<demoId>.json` and are regenerated with `cd backend && mvn -B test -Dtest=GoldenTraceTest -Dgolden.update=true`. Never hand-edit them.
- Message keys match `^[a-z][A-Za-z0-9]*(\.[a-zA-Z0-9]+)+$`. No sentence, no space, no accented character ever appears in a key.
- Every key that reaches the frontend must exist in both `frontend/src/i18n/pt.json` and `frontend/src/i18n/en.json`.
- A step's highlighted line narrates that step: a decision highlights the condition just evaluated, a mutation highlights the last line of that mutation.
- Hash demos are didactic and must say so in their description: `key % capacity`, fixed table, no resize, no treeify.
- Run the full suites before committing: `cd backend && mvn -B verify` and `cd frontend && npm test && npm run build`.

## File Structure

**Backend, modified:**
- `trace/TraceStep.java` — carries `messageKey` and `messageArgs` instead of `message`
- `trace/Tracer.java` — `step` takes the key and the args
- `trace/Trace.java` — `title` becomes `titleKey`
- `trace/ViewPayload.java` — permits three more records
- `core/Demo.java` — `title()`/`description()` become `titleKey()`/`descriptionKey()`
- `core/ParameterSpec.java` — `label` becomes `labelKey`
- `core/DemoInputException.java` — carries a key and args
- `api/ApiError.java`, `api/ApiExceptionHandler.java`, `api/DemoSummary.java`
- `core/algorithms/BinarySearch.java`, `BinarySearchDemo.java`, `BubbleSort.java`, `BubbleSortDemo.java`

**Backend, created:**
- `trace/HashTableView.java`, `trace/TreeView.java`, `trace/LinkedListView.java`
- `core/structures/HashMapPut.java`, `HashMapPutDemo.java`, `HashSetAdd.java`, `HashSetAddDemo.java`, `BstInsert.java`, `BstInsertDemo.java`, `LinkedListInsert.java`, `LinkedListInsertDemo.java`

Each structure follows the phase 1 split: a plain algorithm class holding the traced code, and a `@Component` demo class holding identity, parameters and validation.

**Frontend, created:**
- `src/i18n/pt.json`, `src/i18n/en.json` — the catalogs
- `src/i18n/format.ts` — placeholder substitution
- `src/i18n/I18nContext.tsx` — language state and the `t` function
- `src/components/views/HashTableView.tsx`, `TreeView.tsx`, `LinkedListView.tsx`

**Frontend, modified:**
- `src/types.ts`, `src/api.ts`, `src/components/views/registry.tsx`, `src/pages/DemoScreen.tsx`, `src/pages/CatalogScreen.tsx`, `src/App.tsx`

---

### Task 1: Keyed narration, end to end

This task spans both sides on purpose. The golden fixtures are the contract between them: landing the backend half alone would leave `dev` with a backend emitting keys and a frontend rendering `undefined`.

**Files:**
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/trace/TraceStep.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/trace/Tracer.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/trace/Trace.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/core/Demo.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/core/ParameterSpec.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/core/DemoInputException.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/api/ApiError.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/api/ApiExceptionHandler.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/api/DemoSummary.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/core/algorithms/` — all four files
- Create: `backend/src/test/java/dev/klleriston/fundamentals/core/MessageKeyFormatTest.java`
- Create: `frontend/src/i18n/format.ts`, `format.test.ts`
- Create: `frontend/src/i18n/pt.json`, `frontend/src/i18n/en.json`
- Create: `frontend/src/i18n/I18nContext.tsx`
- Create: `frontend/src/i18n/catalog.test.ts`
- Modify: `frontend/src/types.ts`, `src/api.ts`, `src/main.tsx`, `src/pages/DemoScreen.tsx`, `src/pages/CatalogScreen.tsx`
- Regenerate: `frontend/src/fixtures/binary-search.json`, `bubble-sort.json`

**Interfaces:**
- Produces, for every later task: `Tracer.step(int line, String messageKey, Map<String, Object> messageArgs, Map<String, Object> vars, ViewPayload view)`; `Demo.titleKey()`, `Demo.descriptionKey()`; `ParameterSpec.integer(String name, String labelKey, int min, int max, Object defaultValue)` and `ParameterSpec.intArray(String name, String labelKey, int maxLength, Object defaultValue)`; `new DemoInputException(String field, String messageKey, Map<String, Object> messageArgs)`; and on the frontend `useI18n(): { lang, setLang, t }` where `t(key: string, args?: Record<string, unknown>): string`.

- [ ] **Step 1: Write the failing backend test for the key format**

Create `backend/src/test/java/dev/klleriston/fundamentals/core/MessageKeyFormatTest.java`:

```java
package dev.klleriston.fundamentals.core;

import dev.klleriston.fundamentals.trace.TraceStep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MessageKeyFormatTest {

    private static final Pattern KEY = Pattern.compile("^[a-z][A-Za-z0-9]*(\\.[a-zA-Z0-9]+)+$");

    @Autowired
    private List<Demo> demos;

    @Test
    void everyKeyLooksLikeAKeyAndNotLikeASentence() {
        assertThat(demos).isNotEmpty();
        for (Demo demo : demos) {
            assertThat(demo.titleKey()).matches(KEY);
            assertThat(demo.descriptionKey()).matches(KEY);
            demo.parameters().forEach(parameter -> assertThat(parameter.labelKey()).matches(KEY));

            for (TraceStep step : demo.run(defaults(demo)).steps()) {
                assertThat(step.messageKey())
                        .as("step %d of %s", step.index(), demo.id())
                        .matches(KEY);
            }
        }
    }

    private static DemoParams defaults(Demo demo) {
        Map<String, Object> raw = new LinkedHashMap<>();
        demo.parameters().forEach(parameter -> raw.put(parameter.name(), parameter.defaultValue()));
        return DemoParams.of(raw, demo.parameters());
    }
}
```

This test also guards every demo added later in this plan: a new demo whose default parameters throw, or whose narration is a sentence, fails here.

- [ ] **Step 2: Run it and watch it fail to compile**

Run: `cd backend && mvn -B test -Dtest=MessageKeyFormatTest`
Expected: compilation failure — `titleKey()`, `descriptionKey()`, `labelKey()` and `messageKey()` do not exist yet.

- [ ] **Step 3: Change the trace model**

`trace/TraceStep.java`:

```java
package dev.klleriston.fundamentals.trace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

`trace/Tracer.java` — only the `step` method changes:

```java
    public void step(int line, String messageKey, Map<String, Object> messageArgs,
                     Map<String, Object> vars, ViewPayload view) {
        if (steps.size() >= maxSteps) {
            throw new StepLimitExceededException(maxSteps);
        }
        steps.add(new TraceStep(steps.size(), line, messageKey, messageArgs, vars, view));
    }
```

`trace/Trace.java` — rename the second component:

```java
public record Trace(
        String demoId,
        String titleKey,
        String sourceCode,
        List<TraceStep> steps,
        TraceResult result) {

    public Trace {
        steps = List.copyOf(steps);
    }
}
```

- [ ] **Step 4: Change the demo contract**

`core/Demo.java` — replace `title()` and `description()`:

```java
    String titleKey();

    String descriptionKey();
```

`core/ParameterSpec.java` — rename the `label` component to `labelKey` and rename the parameter in all three factory methods. The record body is otherwise unchanged:

```java
public record ParameterSpec(
        String name,
        ParameterType type,
        String labelKey,
        boolean required,
        Integer min,
        Integer max,
        Integer maxLength,
        List<String> options,
        Object defaultValue) {

    public static ParameterSpec integer(String name, String labelKey, int min, int max, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.INT, labelKey, true, min, max, null, null, defaultValue);
    }

    public static ParameterSpec intArray(String name, String labelKey, int maxLength, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.INT_ARRAY, labelKey, true, null, null, maxLength, null, defaultValue);
    }

    public static ParameterSpec enumOf(String name, String labelKey, List<String> options, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.ENUM, labelKey, true, null, null, null, List.copyOf(options), defaultValue);
    }
}
```

`api/DemoSummary.java`:

```java
public record DemoSummary(
        String id,
        String titleKey,
        Category category,
        String descriptionKey,
        List<ParameterSpec> parameters,
        String sourceCode) {

    public static DemoSummary from(Demo demo) {
        return new DemoSummary(demo.id(), demo.titleKey(), demo.category(), demo.descriptionKey(),
                demo.parameters(), demo.displaySource());
    }
}
```

- [ ] **Step 5: Change the error contract**

`core/DemoInputException.java`:

```java
package dev.klleriston.fundamentals.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DemoInputException extends RuntimeException {

    private final String field;
    private final String messageKey;
    private final Map<String, Object> messageArgs;

    public DemoInputException(String field, String messageKey, Map<String, Object> messageArgs) {
        super(messageKey);
        this.field = field;
        this.messageKey = messageKey;
        this.messageArgs = Collections.unmodifiableMap(new LinkedHashMap<>(messageArgs));
    }

    public String field() {
        return field;
    }

    public String messageKey() {
        return messageKey;
    }

    public Map<String, Object> messageArgs() {
        return messageArgs;
    }
}
```

`api/ApiError.java`:

```java
package dev.klleriston.fundamentals.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ApiError(String error, String messageKey, Map<String, Object> messageArgs, String field) {

    public ApiError {
        messageArgs = Collections.unmodifiableMap(new LinkedHashMap<>(messageArgs));
    }
}
```

`api/ApiExceptionHandler.java` — every handler now names a key. `error` and `field` are machine-facing and unchanged:

```java
    @ExceptionHandler(DemoInputException.class)
    public ResponseEntity<ApiError> handleInvalidInput(DemoInputException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_INPUT", exception.messageKey(), exception.messageArgs(), exception.field()));
    }

    @ExceptionHandler(StepLimitExceededException.class)
    public ResponseEntity<ApiError> handleStepLimit(StepLimitExceededException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("STEP_LIMIT_EXCEEDED", "error.stepLimitExceeded",
                        Map.of("maxSteps", exception.limit()), null));
    }

    @ExceptionHandler(UnknownDemoException.class)
    public ResponseEntity<ApiError> handleUnknownDemo(UnknownDemoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("UNKNOWN_DEMO", "error.unknownDemo", Map.of("id", exception.demoId()), null));
    }

    @ExceptionHandler(DemoTimeoutException.class)
    public ResponseEntity<ApiError> handleTimeout(DemoTimeoutException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("EXECUTION_TIMEOUT", "error.executionTimeout",
                        Map.of("seconds", exception.limit().toSeconds()), null));
    }
```

Two of these accessors already exist: `StepLimitExceededException.limit()` and
`UnknownDemoException.demoId()`. `DemoTimeoutException` keeps nothing — it
formats the duration straight into its message — so give it a field:

```java
public class DemoTimeoutException extends RuntimeException {

    private final Duration limit;

    public DemoTimeoutException(Duration limit) {
        super("error.executionTimeout");
        this.limit = limit;
    }

    public Duration limit() {
        return limit;
    }
}
```

Never parse a value back out of a message string.

Every other `throw new DemoInputException(...)` in the codebase must be updated to the three-argument form. Search for them with `grep -rn "DemoInputException(" backend/src/main`. The validation messages become `error.arrayNotSorted`, `error.arrayTooLong`, `error.valueOutOfRange`, `error.missingParameter`, `error.notAnInteger` — keep the key describing the rule that failed, and pass the values the sentence needs as arguments.

- [ ] **Step 6: Migrate binary search**

`core/algorithms/BinarySearch.java` — the four `tracer.step` calls:

```java
            if (a[mid] == target) {
                tracer.step(6, "binarySearch.found",
                        args("index", mid, "value", a[mid], "target", target),
                        vars(low, high, mid), view(a, low, high, mid));
                return mid;
            } else if (a[mid] < target) {
                tracer.step(8, "binarySearch.discardLeft",
                        args("index", mid, "value", a[mid], "target", target),
                        vars(low, high, mid), view(a, low, high, mid));
                low = mid + 1;
            } else {
                tracer.step(10, "binarySearch.discardRight",
                        args("index", mid, "value", a[mid], "target", target),
                        vars(low, high, mid), view(a, low, high, mid));
                high = mid - 1;
            }
        }
        tracer.step(14, "binarySearch.absent", args("target", target),
                vars(low, high, -1), view(a, low, high, -1));
```

Add this helper to the class, and use the same one in every algorithm class in this plan:

```java
    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }
```

`core/algorithms/BinarySearchDemo.java` — identity and parameters:

```java
    @Override
    public String titleKey() {
        return "demo.binarySearch.title";
    }

    @Override
    public String descriptionKey() {
        return "demo.binarySearch.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("array", "demo.binarySearch.param.array", 512, List.of(2, 5, 8, 12, 20, 33)),
                ParameterSpec.integer("target", "demo.binarySearch.param.target", -1000, 1000, 20));
    }
```

and in `run`, `new Trace(id(), titleKey(), SOURCE, ...)`, plus the sorted check:

```java
                throw new DemoInputException("array", "error.arrayNotSorted", Map.of("index", i));
```

- [ ] **Step 7: Migrate bubble sort**

Same treatment. Keys: `bubbleSort.swap` (args `left`, `right`, `leftValue`, `rightValue`), `bubbleSort.keep` (same args), `bubbleSort.sorted` (no args). Identity keys `demo.bubbleSort.title` and `demo.bubbleSort.description`, parameter key `demo.bubbleSort.param.array`. Do not change any line number: swaps stay on line 7, comparisons on line 4, the closing step on line 11.

- [ ] **Step 8: Run the backend suite and regenerate the fixtures**

```bash
cd backend
mvn -B test -Dtest=MessageKeyFormatTest
mvn -B test -Dtest=GoldenTraceTest -Dgolden.update=true
mvn -B verify
```

Expected: all green, and `git diff frontend/src/fixtures` shows `message` replaced by `messageKey` and `messageArgs`, and `title` replaced by `titleKey`.

- [ ] **Step 9: Write the failing formatter test**

Create `frontend/src/i18n/format.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { format } from './format';

describe('format', () => {
  it('replaces a placeholder with its argument', () => {
    expect(format('a[{index}] = {value}', { index: 2, value: 8 })).toBe('a[2] = 8');
  });

  it('replaces every occurrence of a repeated placeholder', () => {
    expect(format('{value} and {value}', { value: 7 })).toBe('7 and 7');
  });

  it('leaves a placeholder alone when the argument is missing', () => {
    expect(format('{a} {b}', { a: 1 })).toBe('1 {b}');
  });

  it('returns the template when there is nothing to replace', () => {
    expect(format('the array is sorted', {})).toBe('the array is sorted');
  });
});
```

- [ ] **Step 10: Run it and watch it fail**

Run: `cd frontend && npm test -- format`
Expected: FAIL — `./format` does not exist.

- [ ] **Step 11: Write the formatter**

Create `frontend/src/i18n/format.ts`:

```ts
export function format(template: string, args: Record<string, unknown> = {}): string {
  return template.replace(/\{(\w+)\}/g, (placeholder, name: string) =>
    name in args ? String(args[name]) : placeholder,
  );
}
```

- [ ] **Step 12: Write the catalogs**

Create `frontend/src/i18n/en.json`:

```json
{
  "ui.appTitle": "Java and Algorithms Fundamentals",
  "ui.run": "Run",
  "ui.back": "Demos",
  "ui.step": "step {current} / {total}",
  "ui.variables": "Variables",
  "ui.code": "Code",
  "ui.language": "Language",
  "category.ALGORITHMS": "Algorithms",
  "category.DATA_STRUCTURES": "Data structures",
  "category.JVM": "JVM",
  "category.OOP": "OOP",
  "demo.binarySearch.title": "Binary Search",
  "demo.binarySearch.description": "Halve the search range on every comparison to find a value in a sorted array.",
  "demo.binarySearch.param.array": "Sorted array",
  "demo.binarySearch.param.target": "Target value",
  "demo.bubbleSort.title": "Bubble Sort",
  "demo.bubbleSort.description": "Compare neighbours and swap them until the largest values bubble to the end.",
  "demo.bubbleSort.param.array": "Array",
  "binarySearch.found": "a[{index}] = {value} equals {target}, found at index {index}",
  "binarySearch.discardLeft": "a[{index}] = {value} < {target}, discard the left half",
  "binarySearch.discardRight": "a[{index}] = {value} > {target}, discard the right half",
  "binarySearch.absent": "{target} is not in the array, return -1",
  "bubbleSort.swap": "a[{left}] = {leftValue} > a[{right}] = {rightValue}, swap them",
  "bubbleSort.keep": "a[{left}] = {leftValue} <= a[{right}] = {rightValue}, keep the order",
  "bubbleSort.sorted": "The array is sorted",
  "error.arrayNotSorted": "The array must be sorted ascending",
  "error.arrayTooLong": "The array takes at most {maxLength} values",
  "error.valueOutOfRange": "The value must be between {min} and {max}",
  "error.missingParameter": "The parameter {name} is required",
  "error.notAnInteger": "The parameter {name} must be a whole number",
  "error.stepLimitExceeded": "The demo produced more than {maxSteps} steps",
  "error.unknownDemo": "There is no demo called {id}",
  "error.executionTimeout": "The demo took longer than {seconds} seconds"
}
```

Create `frontend/src/i18n/pt.json` with exactly the same keys:

```json
{
  "ui.appTitle": "Fundamentos de Java e Algoritmos",
  "ui.run": "Executar",
  "ui.back": "Demos",
  "ui.step": "passo {current} / {total}",
  "ui.variables": "Variáveis",
  "ui.code": "Código",
  "ui.language": "Idioma",
  "category.ALGORITHMS": "Algoritmos",
  "category.DATA_STRUCTURES": "Estruturas de dados",
  "category.JVM": "JVM",
  "category.OOP": "OOP",
  "demo.binarySearch.title": "Busca Binária",
  "demo.binarySearch.description": "Corta o intervalo de busca pela metade a cada comparação para achar um valor num array ordenado.",
  "demo.binarySearch.param.array": "Array ordenado",
  "demo.binarySearch.param.target": "Valor procurado",
  "demo.bubbleSort.title": "Bubble Sort",
  "demo.bubbleSort.description": "Compara vizinhos e troca de lugar até os maiores valores subirem para o fim.",
  "demo.bubbleSort.param.array": "Array",
  "binarySearch.found": "a[{index}] = {value} é igual a {target}, achou no índice {index}",
  "binarySearch.discardLeft": "a[{index}] = {value} < {target}, descarta a metade da esquerda",
  "binarySearch.discardRight": "a[{index}] = {value} > {target}, descarta a metade da direita",
  "binarySearch.absent": "{target} não está no array, retorna -1",
  "bubbleSort.swap": "a[{left}] = {leftValue} > a[{right}] = {rightValue}, troca os dois",
  "bubbleSort.keep": "a[{left}] = {leftValue} <= a[{right}] = {rightValue}, mantém a ordem",
  "bubbleSort.sorted": "O array está ordenado",
  "error.arrayNotSorted": "O array precisa estar em ordem crescente",
  "error.arrayTooLong": "O array aceita no máximo {maxLength} valores",
  "error.valueOutOfRange": "O valor precisa estar entre {min} e {max}",
  "error.missingParameter": "O parâmetro {name} é obrigatório",
  "error.notAnInteger": "O parâmetro {name} precisa ser um número inteiro",
  "error.stepLimitExceeded": "A demo passou de {maxSteps} passos",
  "error.unknownDemo": "Não existe demo chamada {id}",
  "error.executionTimeout": "A demo demorou mais de {seconds} segundos"
}
```

- [ ] **Step 13: Write the failing catalog tests**

Create `frontend/src/i18n/catalog.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import en from './en.json';
import pt from './pt.json';
import binarySearch from '../fixtures/binary-search.json';
import bubbleSort from '../fixtures/bubble-sort.json';
import type { Trace } from '../types';

const catalogs: Record<string, Record<string, string>> = { en, pt };
const traces = [binarySearch, bubbleSort] as unknown as Trace[];

function keysInUse(): { key: string; args: Record<string, unknown> }[] {
  return traces.flatMap((trace) => [
    { key: trace.titleKey, args: {} },
    ...trace.steps.map((step) => ({ key: step.messageKey, args: step.messageArgs })),
  ]);
}

describe('message catalogs', () => {
  it('define exactly the same keys in both languages', () => {
    expect(Object.keys(pt).sort()).toEqual(Object.keys(en).sort());
  });

  it('cover every key the backend emits', () => {
    for (const { key } of keysInUse()) {
      for (const [lang, catalog] of Object.entries(catalogs)) {
        expect(catalog[key], `${key} missing from ${lang}.json`).toBeDefined();
      }
    }
  });

  it('only use placeholders the backend actually sends', () => {
    for (const { key, args } of keysInUse()) {
      for (const [lang, catalog] of Object.entries(catalogs)) {
        const placeholders = [...(catalog[key] ?? '').matchAll(/\{(\w+)\}/g)].map((match) => match[1]);
        for (const placeholder of placeholders) {
          expect(args, `${key} in ${lang}.json wants {${placeholder}}`).toHaveProperty(placeholder);
        }
      }
    }
  });
});
```

The third test is the subtle one. A translation that mentions an argument the backend never sends would render a literal `{target}` on screen, and no other test would notice.

- [ ] **Step 14: Add the language context**

Create `frontend/src/i18n/I18nContext.tsx`:

```tsx
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { format } from './format';
import en from './en.json';
import pt from './pt.json';

export type Lang = 'pt' | 'en';

const catalogs: Record<Lang, Record<string, string>> = { pt, en };

interface I18n {
  lang: Lang;
  setLang: (lang: Lang) => void;
  t: (key: string, args?: Record<string, unknown>) => string;
}

const I18nContext = createContext<I18n | undefined>(undefined);

export function detectLang(): Lang {
  return navigator.language.toLowerCase().startsWith('pt') ? 'pt' : 'en';
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLang] = useState<Lang>(detectLang);

  const t = useCallback(
    (key: string, args: Record<string, unknown> = {}) => {
      const template = catalogs[lang][key];
      return template === undefined ? key : format(template, args);
    },
    [lang],
  );

  const value = useMemo(() => ({ lang, setLang, t }), [lang, t]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18n {
  const context = useContext(I18nContext);
  if (context === undefined) {
    throw new Error('useI18n must be used inside I18nProvider');
  }
  return context;
}
```

A missing key renders as the key itself — visible and ugly, which is what you want when the catalog tests somehow did not catch it.

- [ ] **Step 15: Update the frontend types**

In `frontend/src/types.ts`, `TraceStep` loses `message`, `Trace` loses `title`, and `ParameterSpec` and `DemoSummary` change with them:

```ts
export interface TraceStep {
  index: number;
  line: number;
  messageKey: string;
  messageArgs: Record<string, unknown>;
  vars: Record<string, unknown>;
  view: ViewPayload;
}

export interface Trace {
  demoId: string;
  titleKey: string;
  sourceCode: string;
  steps: TraceStep[];
  result: TraceResult;
}

export interface ParameterSpec {
  name: string;
  type: ParameterType;
  labelKey: string;
  required: boolean;
  min?: number;
  max?: number;
  maxLength?: number;
  options?: string[];
  defaultValue?: unknown;
}

export interface DemoSummary {
  id: string;
  titleKey: string;
  category: 'ALGORITHMS' | 'DATA_STRUCTURES' | 'JVM' | 'OOP';
  descriptionKey: string;
  parameters: ParameterSpec[];
  sourceCode: string;
}
```

- [ ] **Step 16: Carry the key through the API client**

In `frontend/src/api.ts`, `ApiRequestError` carries the key and args instead of a rendered sentence:

```ts
export class ApiRequestError extends Error {
  readonly field?: string;
  readonly messageKey: string;
  readonly messageArgs: Record<string, unknown>;

  constructor(messageKey: string, messageArgs: Record<string, unknown> = {}, field?: string) {
    super(messageKey);
    this.name = 'ApiRequestError';
    this.messageKey = messageKey;
    this.messageArgs = messageArgs;
    this.field = field;
  }
}

async function parseOrThrow<T>(response: Response): Promise<T> {
  if (response.ok) {
    return (await response.json()) as T;
  }
  let messageKey = 'error.requestFailed';
  let messageArgs: Record<string, unknown> = { status: response.status };
  let field: string | undefined;
  try {
    const body = await response.json();
    if (typeof body?.messageKey === 'string') {
      messageKey = body.messageKey;
      messageArgs = body.messageArgs ?? {};
    }
    if (typeof body?.field === 'string') field = body.field;
  } catch {
    // response had no JSON body; keep the status key
  }
  throw new ApiRequestError(messageKey, messageArgs, field);
}
```

Add `error.requestFailed` to both catalogs: `"The request failed with status {status}"` and `"A requisição falhou com status {status}"`.

- [ ] **Step 17: Render through `t` everywhere**

Wrap the app in `frontend/src/main.tsx`:

```tsx
<I18nProvider>
  <App />
</I18nProvider>
```

Then replace every literal sentence in `CatalogScreen.tsx` and `DemoScreen.tsx` with a `t(...)` call: the demo title becomes `t(demo.titleKey)`, the description `t(demo.descriptionKey)`, a parameter label `t(spec.labelKey)`, the step narration `t(step.messageKey, step.messageArgs)`, the error `t(error.messageKey, error.messageArgs)`, the run button `t('ui.run')`, the step counter `t('ui.step', { current, total })`, the category heading `t(\`category.${category}\`)`.

Update the existing tests in `CatalogScreen.test.tsx` and `DemoScreen.test.tsx`: they must render inside `<I18nProvider>` and assert the rendered sentence in the language the test forces. Keep the regression test that returns the player to step 1 when a new run has the same number of steps.

- [ ] **Step 18: Run everything**

```bash
cd frontend && npm test && npm run build
cd ../backend && mvn -B verify
```

Expected: all green. If a catalog test fails, add the missing key rather than loosening the test.

- [ ] **Step 19: Commit**

```bash
git add backend/src frontend/src
git commit -m "feat: render every message from a key and its arguments"
```

---
### Task 2: Language toggle

**Files:**
- Modify: `frontend/src/i18n/I18nContext.tsx`
- Create: `frontend/src/components/LanguageToggle.tsx`, `LanguageToggle.test.tsx`
- Create: `frontend/src/i18n/I18nContext.test.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `useI18n(): { lang, setLang, t }` and `detectLang(): Lang` from Task 1.
- Produces: `<LanguageToggle />`, rendered once in the app header.

- [ ] **Step 1: Write the failing persistence test**

Create `frontend/src/i18n/I18nContext.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { I18nProvider, useI18n } from './I18nContext';

function Probe() {
  const { lang, setLang, t } = useI18n();
  return (
    <div>
      <span>{t('ui.run')}</span>
      <span>lang={lang}</span>
      <button onClick={() => setLang(lang === 'pt' ? 'en' : 'pt')}>switch</button>
    </div>
  );
}

describe('I18nProvider', () => {
  beforeEach(() => localStorage.clear());

  it('starts in the language stored from a previous visit', () => {
    localStorage.setItem('lang', 'pt');
    render(<I18nProvider><Probe /></I18nProvider>);
    expect(screen.getByText('Executar')).toBeInTheDocument();
  });

  it('remembers the language across a remount', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<I18nProvider><Probe /></I18nProvider>);
    await user.click(screen.getByRole('button', { name: 'switch' }));
    const chosen = screen.getByText(/lang=/).textContent;
    unmount();

    render(<I18nProvider><Probe /></I18nProvider>);
    expect(screen.getByText(/lang=/).textContent).toBe(chosen);
  });

  it('sets the document language so screen readers follow', async () => {
    const user = userEvent.setup();
    render(<I18nProvider><Probe /></I18nProvider>);
    await user.click(screen.getByRole('button', { name: 'switch' }));
    expect(document.documentElement.lang).toBe(screen.getByText(/lang=/).textContent?.replace('lang=', ''));
  });
});
```

- [ ] **Step 2: Run it and watch it fail**

Run: `cd frontend && npm test -- I18nContext`
Expected: FAIL — the provider ignores `localStorage` and never touches `document.documentElement.lang`.

- [ ] **Step 3: Make the provider persist and publish the language**

In `frontend/src/i18n/I18nContext.tsx`, replace the `useState` line and add an effect:

```tsx
const STORAGE_KEY = 'lang';

export function detectLang(): Lang {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === 'pt' || stored === 'en') {
    return stored;
  }
  return navigator.language.toLowerCase().startsWith('pt') ? 'pt' : 'en';
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(detectLang);

  const setLang = useCallback((next: Lang) => {
    localStorage.setItem(STORAGE_KEY, next);
    setLangState(next);
  }, []);

  useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);
  ...
```

Import `useEffect` from React. Everything else in the file stays as Task 1 left it.

- [ ] **Step 4: Write the failing toggle test**

Create `frontend/src/components/LanguageToggle.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { I18nProvider } from '../i18n/I18nContext';
import { LanguageToggle } from './LanguageToggle';

describe('LanguageToggle', () => {
  beforeEach(() => localStorage.clear());

  it('switches the rendered language', async () => {
    localStorage.setItem('lang', 'en');
    const user = userEvent.setup();
    render(<I18nProvider><LanguageToggle /></I18nProvider>);

    await user.click(screen.getByRole('button', { name: 'Português' }));
    expect(screen.getByRole('button', { name: 'Português' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'false');
  });
});
```

- [ ] **Step 5: Run it and watch it fail**

Run: `cd frontend && npm test -- LanguageToggle`
Expected: FAIL — `./LanguageToggle` does not exist.

- [ ] **Step 6: Write the toggle**

Create `frontend/src/components/LanguageToggle.tsx`:

```tsx
import { useI18n, type Lang } from '../i18n/I18nContext';

const languages: { code: Lang; label: string }[] = [
  { code: 'pt', label: 'Português' },
  { code: 'en', label: 'English' },
];

export function LanguageToggle() {
  const { lang, setLang, t } = useI18n();

  return (
    <div className="language-toggle" role="group" aria-label={t('ui.language')}>
      {languages.map((language) => (
        <button
          key={language.code}
          type="button"
          aria-pressed={lang === language.code}
          onClick={() => setLang(language.code)}
        >
          {language.label}
        </button>
      ))}
    </div>
  );
}
```

The language names stay in their own language — a Brazilian looking for Portuguese looks for "Português", not for "Portuguese".

- [ ] **Step 7: Put it in the header**

Render `<LanguageToggle />` in the app header in `frontend/src/App.tsx`, so it shows on both screens. Give `.language-toggle` and its buttons a rule in `frontend/src/index.css` consistent with the existing dark theme, and make the pressed one visibly selected — `button[aria-pressed='true']` gets the accent background already used for the active control.

- [ ] **Step 8: Run everything and commit**

```bash
cd frontend && npm test && npm run build
git add frontend/src
git commit -m "feat: let the learner switch between Portuguese and English"
```

---

### Task 3: Back link

**Files:**
- Modify: `frontend/src/pages/DemoScreen.tsx`
- Modify: `frontend/src/pages/DemoScreen.test.tsx`

**Interfaces:**
- Consumes: `useI18n().t` from Task 1, the `ui.back` key from both catalogs, and React Router's `Link`.

- [ ] **Step 1: Write the failing tests**

Add to `frontend/src/pages/DemoScreen.test.tsx`:

```tsx
it('offers a way back to the catalog', async () => {
  renderScreen();
  const back = await screen.findByRole('link', { name: /demos/i });
  expect(back).toHaveAttribute('href', '/');
});

it('still offers the way back when the run failed', async () => {
  server.use(
    http.post('/api/demos/binary-search/trace', () =>
      HttpResponse.json({ error: 'INVALID_INPUT', messageKey: 'error.arrayNotSorted', messageArgs: {}, field: 'array' }, { status: 400 }),
    ),
  );
  renderScreen();
  await screen.findByRole('alert');
  expect(screen.getByRole('link', { name: /demos/i })).toHaveAttribute('href', '/');
});
```

Use whatever MSW server handle the file already imports; do not create a second one.

- [ ] **Step 2: Run them and watch them fail**

Run: `cd frontend && npm test -- DemoScreen`
Expected: FAIL — no link with that name is rendered.

- [ ] **Step 3: Add the link**

In `frontend/src/pages/DemoScreen.tsx`, at the top of the screen's header, outside any block that depends on a loaded trace:

```tsx
<Link className="back-link" to="/">
  ← {t('ui.back')}
</Link>
```

Import `Link` from `react-router-dom`. It must render before the early returns for the loading and error states, otherwise the second test fails.

- [ ] **Step 4: Run everything and commit**

```bash
cd frontend && npm test && npm run build
git add frontend/src
git commit -m "feat: add a link back to the catalog from a demo"
```

---

### Task 4: `hash-map-put`

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/HashTableView.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/trace/ViewPayload.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/structures/HashMapPut.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/structures/HashMapPutDemo.java`
- Create: `backend/src/test/java/dev/klleriston/fundamentals/core/structures/HashMapPutDemoTest.java`
- Modify: `backend/src/test/java/dev/klleriston/fundamentals/api/GoldenTraceTest.java`
- Create: `frontend/src/components/views/HashTableView.tsx`, `HashTableView.test.tsx`
- Modify: `frontend/src/components/views/registry.tsx`, `frontend/src/types.ts`
- Modify: `frontend/src/i18n/pt.json`, `frontend/src/i18n/en.json`
- Generated: `frontend/src/fixtures/hash-map-put.json`

**Interfaces:**
- Consumes: `Tracer.step(int line, String messageKey, Map<String, Object> messageArgs, Map<String, Object> vars, ViewPayload view)`, `Demo.titleKey()`, `Demo.descriptionKey()`, `ParameterSpec.intArray`, `ParameterSpec.integer`, `DemoInputException(field, messageKey, messageArgs)` — all from Task 1.
- Produces: `HashTableView`, reused unchanged by Task 5.

- [ ] **Step 1: Write the failing demo test**

Create `backend/src/test/java/dev/klleriston/fundamentals/core/structures/HashMapPutDemoTest.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.HashTableView;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashMapPutDemoTest {

    private final HashMapPutDemo demo = new HashMapPutDemo();

    private Trace run(Map<String, Object> raw) {
        return demo.run(DemoParams.of(raw, demo.parameters()));
    }

    @Test
    void exposesItsIdentity() {
        assertThat(demo.id()).isEqualTo("hash-map-put");
        assertThat(demo.category()).isEqualTo(Category.DATA_STRUCTURES);
        assertThat(demo.displaySource()).doesNotContain("tracer");
    }

    @Test
    void walksTheWholeChainBeforeInserting() {
        Trace trace = run(Map.of("keys", List.of(5, 21, 37, 8), "key", 13, "value", 99));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("hashMapPut.bucket", "hashMapPut.compare", "hashMapPut.compare",
                        "hashMapPut.compare", "hashMapPut.insert");
        assertThat(trace.steps()).extracting(TraceStep::line)
                .containsExactly(2, 5, 5, 5, 11);

        HashTableView last = (HashTableView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.capacity()).isEqualTo(8);
        assertThat(last.buckets()).hasSize(8);
        assertThat(last.activeBucket()).isEqualTo(5);
        assertThat(last.outcome()).isEqualTo(HashTableView.INSERTED);
        assertThat(last.buckets().get(5).entries()).extracting(HashTableView.Entry::key)
                .containsExactly(13, 37, 21, 5);
    }

    @Test
    void updatesTheValueWhenTheKeyIsAlreadyThere() {
        Trace trace = run(Map.of("keys", List.of(5, 21, 37, 8), "key", 21, "value", 99));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("hashMapPut.bucket", "hashMapPut.compare", "hashMapPut.update");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(2, 5, 6);

        HashTableView last = (HashTableView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.outcome()).isEqualTo(HashTableView.UPDATED);
        assertThat(last.buckets().get(5).entries())
                .filteredOn(entry -> entry.key() == 21)
                .allSatisfy(entry -> {
                    assertThat(entry.value()).isEqualTo(99);
                    assertThat(entry.state()).isEqualTo(HashTableView.Entry.MATCHED);
                });
    }

    @Test
    void rejectsNegativeKeys() {
        assertThatThrownBy(() -> run(Map.of("keys", List.of(5), "key", -3, "value", 1)))
                .isInstanceOf(DemoInputException.class);
    }
}
```

The chain order in the first test is `13, 37, 21, 5`: entries go in at the head, so the newest is first and the pre-built keys appear in reverse insertion order.

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && mvn -B test -Dtest=HashMapPutDemoTest`
Expected: compilation failure — none of these classes exist.

- [ ] **Step 3: Add the view payload**

Create `backend/src/main/java/dev/klleriston/fundamentals/trace/HashTableView.java`:

```java
package dev.klleriston.fundamentals.trace;

import java.util.List;

public record HashTableView(
        int capacity,
        List<Bucket> buckets,
        Integer activeBucket,
        String outcome) implements ViewPayload {

    public static final String INSERTED = "INSERTED";
    public static final String UPDATED = "UPDATED";
    public static final String DUPLICATE = "DUPLICATE";

    public HashTableView {
        buckets = List.copyOf(buckets);
    }

    @Override
    public String kind() {
        return "HASH_TABLE";
    }

    public record Bucket(int index, List<Entry> entries) {

        public Bucket {
            entries = List.copyOf(entries);
        }
    }

    public record Entry(int key, Integer value, String state) {

        public static final String NORMAL = "NORMAL";
        public static final String PROBED = "PROBED";
        public static final String MATCHED = "MATCHED";
        public static final String INSERTED = "INSERTED";
    }
}
```

`List.copyOf` is fine here — lists keep their order. The `Map.copyOf` ban is about maps only.

Then widen `trace/ViewPayload.java`, adding only the name this task creates:

```java
public sealed interface ViewPayload permits ArrayView, HashTableView {
```

Tasks 6 and 7 add `TreeView` and `LinkedListView` to the same clause when they
create those records. Naming a record that does not exist yet does not compile.

- [ ] **Step 4: Write the traced algorithm**

Create `backend/src/main/java/dev/klleriston/fundamentals/core/structures/HashMapPut.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.trace.HashTableView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HashMapPut {

    public static final int CAPACITY = 8;

    private HashMapPut() {
    }

    /** Entry in a bucket chain. Newest first, like the didactic source shown on screen. */
    record Entry(int key, int value) {
    }

    public static String put(List<Integer> existingKeys, int key, int value, Tracer tracer) {
        List<List<Entry>> table = new ArrayList<>();
        for (int i = 0; i < CAPACITY; i++) {
            table.add(new ArrayList<>());
        }
        for (int existing : existingKeys) {
            table.get(existing % CAPACITY).add(0, new Entry(existing, existing));
        }

        int index = key % CAPACITY;
        tracer.step(2, "hashMapPut.bucket", args("key", key, "bucket", index, "capacity", CAPACITY),
                vars(key, index), view(table, index, null, -1));

        List<Entry> chain = table.get(index);
        for (int position = 0; position < chain.size(); position++) {
            Entry entry = chain.get(position);
            if (entry.key() == key) {
                chain.set(position, new Entry(key, value));
                tracer.step(6, "hashMapPut.update", args("key", key, "value", value),
                        vars(key, index), view(table, index, HashTableView.UPDATED, position));
                return HashTableView.UPDATED;
            }
            tracer.step(5, "hashMapPut.compare", args("key", key, "other", entry.key()),
                    vars(key, index), view(table, index, null, position));
        }

        chain.add(0, new Entry(key, value));
        tracer.step(11, "hashMapPut.insert", args("key", key, "bucket", index),
                vars(key, index), view(table, index, HashTableView.INSERTED, 0));
        return HashTableView.INSERTED;
    }

    private static Map<String, Object> vars(int key, int index) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("key", key);
        vars.put("index", index);
        return vars;
    }

    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }

    private static HashTableView view(List<List<Entry>> table, int activeBucket, String outcome, int highlighted) {
        List<HashTableView.Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            List<HashTableView.Entry> entries = new ArrayList<>();
            for (int position = 0; position < table.get(i).size(); position++) {
                Entry entry = table.get(i).get(position);
                entries.add(new HashTableView.Entry(entry.key(), entry.value(), state(i, position, activeBucket, outcome, highlighted)));
            }
            buckets.add(new HashTableView.Bucket(i, entries));
        }
        return new HashTableView(CAPACITY, buckets, activeBucket, outcome);
    }

    private static String state(int bucket, int position, int activeBucket, String outcome, int highlighted) {
        if (bucket != activeBucket || position != highlighted) {
            return HashTableView.Entry.NORMAL;
        }
        if (HashTableView.INSERTED.equals(outcome)) {
            return HashTableView.Entry.INSERTED;
        }
        if (HashTableView.UPDATED.equals(outcome)) {
            return HashTableView.Entry.MATCHED;
        }
        return HashTableView.Entry.PROBED;
    }
}
```

Note the order of the two steps in the loop: the comparison step is emitted **after** the equality check fails, so the `update` step replaces it when the keys match. That keeps every step's highlighted line honest — line 5 is the condition that was false, line 6 is the assignment that ran.

- [ ] **Step 5: Write the demo**

Create `backend/src/main/java/dev/klleriston/fundamentals/core/structures/HashMapPutDemo.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import dev.klleriston.fundamentals.trace.Tracer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class HashMapPutDemo implements Demo {

    private static final String SOURCE = """
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
            }""";

    @Override
    public String id() {
        return "hash-map-put";
    }

    @Override
    public String titleKey() {
        return "demo.hashMapPut.title";
    }

    @Override
    public Category category() {
        return Category.DATA_STRUCTURES;
    }

    @Override
    public String descriptionKey() {
        return "demo.hashMapPut.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("keys", "demo.hashMapPut.param.keys", 32, List.of(5, 21, 37, 8)),
                ParameterSpec.integer("key", "demo.hashMapPut.param.key", 0, 1000, 13),
                ParameterSpec.integer("value", "demo.hashMapPut.param.value", -1000, 1000, 99));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        List<Integer> keys = Arrays.stream(params.intArray("keys")).boxed().toList();
        int key = params.integer("key");
        int value = params.integer("value");
        requireNonNegative(keys, key);

        Tracer tracer = new Tracer();
        String outcome = HashMapPut.put(keys, key, value, tracer);

        return new Trace(id(), titleKey(), SOURCE, tracer.steps(),
                new TraceResult(outcome, tracer.steps().size(), false));
    }

    private static void requireNonNegative(List<Integer> keys, int key) {
        if (key < 0) {
            throw new DemoInputException("key", "error.keyMustBeNonNegative", Map.of("value", key));
        }
        for (int existing : keys) {
            if (existing < 0) {
                throw new DemoInputException("keys", "error.keyMustBeNonNegative", Map.of("value", existing));
            }
        }
    }
}
```

Keys are non-negative so the didactic `key % capacity` never returns a negative index. The real `java.util.HashMap` has no such restriction because it spreads the hash first.

- [ ] **Step 6: Run the demo test**

Run: `cd backend && mvn -B test -Dtest=HashMapPutDemoTest`
Expected: PASS, all four tests.

- [ ] **Step 7: Add the golden fixture**

Register `hash-map-put` in `GoldenTraceTest` the same way `binary-search` and `bubble-sort` are registered, then:

```bash
cd backend && mvn -B test -Dtest=GoldenTraceTest -Dgolden.update=true
```

Expected: `frontend/src/fixtures/hash-map-put.json` appears. Read it and check that bucket 5 holds four entries and bucket 0 holds one.

- [ ] **Step 8: Add the TypeScript type**

In `frontend/src/types.ts`:

```ts
export type HashEntryState = 'NORMAL' | 'PROBED' | 'MATCHED' | 'INSERTED';

export interface HashTableViewPayload {
  kind: 'HASH_TABLE';
  capacity: number;
  buckets: { index: number; entries: { key: number; value: number | null; state: HashEntryState }[] }[];
  activeBucket: number | null;
  outcome: 'INSERTED' | 'UPDATED' | 'DUPLICATE' | null;
}
```

and add `HashTableViewPayload` to the `ViewPayload` union, before `UnknownViewPayload`.

- [ ] **Step 9: Write the failing renderer test**

Create `frontend/src/components/views/HashTableView.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import fixture from '../../fixtures/hash-map-put.json';
import type { HashTableViewPayload, Trace } from '../../types';
import { HashTableView } from './HashTableView';

const trace = fixture as unknown as Trace;

describe('HashTableView', () => {
  it('draws every bucket, including the empty ones', () => {
    const view = trace.steps[0].view as HashTableViewPayload;
    render(<HashTableView view={view} />);
    expect(screen.getAllByTestId(/^bucket-/)).toHaveLength(view.capacity);
  });

  it('marks the bucket the key hashed to', () => {
    const view = trace.steps[0].view as HashTableViewPayload;
    render(<HashTableView view={view} />);
    expect(screen.getByTestId(`bucket-${view.activeBucket}`)).toHaveAttribute('data-active', 'true');
  });

  it('marks the inserted entry on the last step', () => {
    const view = trace.steps[trace.steps.length - 1].view as HashTableViewPayload;
    render(<HashTableView view={view} />);
    expect(screen.getByText('13')).toHaveAttribute('data-state', 'INSERTED');
  });
});
```

Driving the test from the real fixture is deliberate: a handmade object would keep passing after the backend changed shape.

- [ ] **Step 10: Run it and watch it fail**

Run: `cd frontend && npm test -- HashTableView`
Expected: FAIL — `./HashTableView` does not exist.

- [ ] **Step 11: Write the renderer**

Create `frontend/src/components/views/HashTableView.tsx`:

```tsx
import type { HashTableViewPayload } from '../../types';

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
              <span key={entry.key} className="hash-entry" data-state={entry.state}>
                {entry.key}
                {entry.value === null ? '' : `: ${entry.value}`}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
```

Register it in `frontend/src/components/views/registry.tsx`:

```ts
const renderers: Record<string, ComponentType<{ view: any }>> = {
  ARRAY: ArrayView,
  HASH_TABLE: HashTableView,
};
```

Style `.hash-table`, `.hash-bucket`, `.hash-entry` in `frontend/src/index.css`, following the dark theme already there. A bucket is a row: its index on the left, its chain to the right. `[data-active='true']` and each `[data-state]` get a distinct colour, and the chain must scroll horizontally rather than push the page wider.

- [ ] **Step 12: Add the catalog entries**

Add to both `frontend/src/i18n/en.json` and `pt.json`, with the same keys:

English:

```json
  "demo.hashMapPut.title": "HashMap put",
  "demo.hashMapPut.description": "Find the bucket for a key, walk its chain, then insert or update. Didactic: the bucket is key % capacity, the table never resizes and chains never become trees — java.util.HashMap spreads the hash and does both.",
  "demo.hashMapPut.param.keys": "Keys already in the map",
  "demo.hashMapPut.param.key": "Key to put",
  "demo.hashMapPut.param.value": "Value to put",
  "hashMapPut.bucket": "{key} % {capacity} = {bucket}, look in bucket {bucket}",
  "hashMapPut.compare": "{key} is not {other}, keep walking the chain",
  "hashMapPut.update": "{key} is already here, replace its value with {value}",
  "hashMapPut.insert": "the chain has no {key}, insert it at the head of bucket {bucket}",
  "error.keyMustBeNonNegative": "Keys must be zero or greater, and {value} is not"
```

Portuguese:

```json
  "demo.hashMapPut.title": "HashMap put",
  "demo.hashMapPut.description": "Acha o bucket da chave, percorre a corrente e então insere ou atualiza. Didático: o bucket é key % capacidade, a tabela nunca redimensiona e correntes nunca viram árvores — o java.util.HashMap espalha o hash e faz as duas coisas.",
  "demo.hashMapPut.param.keys": "Chaves já no mapa",
  "demo.hashMapPut.param.key": "Chave a inserir",
  "demo.hashMapPut.param.value": "Valor a inserir",
  "hashMapPut.bucket": "{key} % {capacity} = {bucket}, procura no bucket {bucket}",
  "hashMapPut.compare": "{key} não é {other}, segue andando na corrente",
  "hashMapPut.update": "{key} já está aqui, troca o valor por {value}",
  "hashMapPut.insert": "a corrente não tem {key}, insere na cabeça do bucket {bucket}",
  "error.keyMustBeNonNegative": "As chaves precisam ser zero ou mais, e {value} não é"
```

The description carries the honesty clause required by the spec: the learner is told where this differs from the JDK, in their own language.

- [ ] **Step 13: Run everything and commit**

```bash
cd backend && mvn -B verify
cd ../frontend && npm test && npm run build
git add backend/src frontend/src
git commit -m "feat: add the hash map put demo"
```

The catalog test from Task 1 now also covers this demo's keys — it reads every fixture, so `hash-map-put.json` joins automatically once the fixture exists. Add the new fixture to the list it imports.

---
### Task 5: `hash-set-add`

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/structures/HashSetAdd.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/structures/HashSetAddDemo.java`
- Create: `backend/src/test/java/dev/klleriston/fundamentals/core/structures/HashSetAddDemoTest.java`
- Modify: `backend/src/test/java/dev/klleriston/fundamentals/api/GoldenTraceTest.java`
- Modify: `frontend/src/i18n/pt.json`, `frontend/src/i18n/en.json`, `frontend/src/i18n/catalog.test.ts`
- Generated: `frontend/src/fixtures/hash-set-add.json`

**Interfaces:**
- Consumes: `HashTableView`, `HashTableView.Bucket`, `HashTableView.Entry` and the constants `INSERTED`, `DUPLICATE` from Task 4; the `Tracer.step` and `Demo` signatures from Task 1.
- Produces: nothing new. This task adds no view kind and no renderer — it reuses `HASH_TABLE` exactly as Task 4 built it.

- [ ] **Step 1: Write the failing demo test**

Create `backend/src/test/java/dev/klleriston/fundamentals/core/structures/HashSetAddDemoTest.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.HashTableView;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HashSetAddDemoTest {

    private final HashSetAddDemo demo = new HashSetAddDemo();

    private Trace run(Map<String, Object> raw) {
        return demo.run(DemoParams.of(raw, demo.parameters()));
    }

    @Test
    void exposesItsIdentity() {
        assertThat(demo.id()).isEqualTo("hash-set-add");
        assertThat(demo.category()).isEqualTo(Category.DATA_STRUCTURES);
        assertThat(demo.displaySource()).doesNotContain("tracer");
    }

    @Test
    void refusesAValueThatIsAlreadyInTheSet() {
        Trace trace = run(Map.of("values", List.of(5, 21, 37, 8), "value", 37));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("hashSetAdd.bucket", "hashSetAdd.duplicate");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(2, 6);

        HashTableView last = (HashTableView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.outcome()).isEqualTo(HashTableView.DUPLICATE);
        assertThat(trace.result().returnValue()).isEqualTo(false);
    }

    @Test
    void addsAValueThatIsNotThereYet() {
        Trace trace = run(Map.of("values", List.of(5, 21, 37, 8), "value", 13));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("hashSetAdd.bucket", "hashSetAdd.compare", "hashSetAdd.compare",
                        "hashSetAdd.compare", "hashSetAdd.insert");
        assertThat(trace.result().returnValue()).isEqualTo(true);

        HashTableView last = (HashTableView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.buckets().get(5).entries()).extracting(HashTableView.Entry::key)
                .containsExactly(13, 37, 21, 5);
    }

    @Test
    void showsKeysWithoutValues() {
        Trace trace = run(Map.of("values", List.of(5), "value", 13));

        HashTableView first = (HashTableView) trace.steps().get(0).view();
        assertThat(first.buckets()).flatExtracting(HashTableView.Bucket::entries)
                .allSatisfy(entry -> assertThat(entry.value()).isNull());
    }
}
```

The last test pins the difference from Task 4: a set draws keys only, so every `value` is null and the renderer written in Task 4 prints no colon.

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && mvn -B test -Dtest=HashSetAddDemoTest`
Expected: compilation failure — `HashSetAddDemo` does not exist.

- [ ] **Step 3: Write the traced algorithm**

Create `backend/src/main/java/dev/klleriston/fundamentals/core/structures/HashSetAdd.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.trace.HashTableView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HashSetAdd {

    public static final int CAPACITY = 8;

    private HashSetAdd() {
    }

    public static boolean add(List<Integer> existingValues, int value, Tracer tracer) {
        List<List<Integer>> table = new ArrayList<>();
        for (int i = 0; i < CAPACITY; i++) {
            table.add(new ArrayList<>());
        }
        for (int existing : existingValues) {
            table.get(existing % CAPACITY).add(0, existing);
        }

        int index = value % CAPACITY;
        tracer.step(2, "hashSetAdd.bucket", args("value", value, "bucket", index, "capacity", CAPACITY),
                vars(value, index), view(table, index, null, -1));

        List<Integer> chain = table.get(index);
        for (int position = 0; position < chain.size(); position++) {
            if (chain.get(position).intValue() == value) {
                tracer.step(6, "hashSetAdd.duplicate", args("value", value),
                        vars(value, index), view(table, index, HashTableView.DUPLICATE, position));
                return false;
            }
            tracer.step(5, "hashSetAdd.compare", args("value", value, "other", chain.get(position)),
                    vars(value, index), view(table, index, null, position));
        }

        chain.add(0, value);
        tracer.step(10, "hashSetAdd.insert", args("value", value, "bucket", index),
                vars(value, index), view(table, index, HashTableView.INSERTED, 0));
        return true;
    }

    private static Map<String, Object> vars(int value, int index) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("value", value);
        vars.put("index", index);
        return vars;
    }

    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }

    private static HashTableView view(List<List<Integer>> table, int activeBucket, String outcome, int highlighted) {
        List<HashTableView.Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            List<HashTableView.Entry> entries = new ArrayList<>();
            for (int position = 0; position < table.get(i).size(); position++) {
                entries.add(new HashTableView.Entry(table.get(i).get(position), null,
                        state(i, position, activeBucket, outcome, highlighted)));
            }
            buckets.add(new HashTableView.Bucket(i, entries));
        }
        return new HashTableView(CAPACITY, buckets, activeBucket, outcome);
    }

    private static String state(int bucket, int position, int activeBucket, String outcome, int highlighted) {
        if (bucket != activeBucket || position != highlighted) {
            return HashTableView.Entry.NORMAL;
        }
        if (HashTableView.INSERTED.equals(outcome)) {
            return HashTableView.Entry.INSERTED;
        }
        if (HashTableView.DUPLICATE.equals(outcome)) {
            return HashTableView.Entry.MATCHED;
        }
        return HashTableView.Entry.PROBED;
    }
}
```

Note `.intValue()` in the comparison. `chain.get(position)` is an `Integer`, and
comparing it to an `int` with `==` unboxes correctly — but writing
`chain.get(position) == someInteger` would compare references, and values above
127 fall outside the `Integer` cache and silently stop matching. Keep the
explicit unboxing so the next person does not have to reason about it.

- [ ] **Step 4: Write the demo**

Create `backend/src/main/java/dev/klleriston/fundamentals/core/structures/HashSetAddDemo.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import dev.klleriston.fundamentals.trace.Tracer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class HashSetAddDemo implements Demo {

    private static final String SOURCE = """
            public boolean add(int value) {
                int index = value % table.length;
                Node node = table[index];
                while (node != null) {
                    if (node.value == value) {
                        return false;
                    }
                    node = node.next;
                }
                table[index] = new Node(value, table[index]);
                size++;
                return true;
            }""";

    @Override
    public String id() {
        return "hash-set-add";
    }

    @Override
    public String titleKey() {
        return "demo.hashSetAdd.title";
    }

    @Override
    public Category category() {
        return Category.DATA_STRUCTURES;
    }

    @Override
    public String descriptionKey() {
        return "demo.hashSetAdd.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("values", "demo.hashSetAdd.param.values", 32, List.of(5, 21, 37, 8)),
                ParameterSpec.integer("value", "demo.hashSetAdd.param.value", 0, 1000, 37));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        List<Integer> values = Arrays.stream(params.intArray("values")).boxed().toList();
        int value = params.integer("value");
        requireNonNegative(values, value);

        Tracer tracer = new Tracer();
        boolean added = HashSetAdd.add(values, value, tracer);

        return new Trace(id(), titleKey(), SOURCE, tracer.steps(),
                new TraceResult(added, tracer.steps().size(), false));
    }

    private static void requireNonNegative(List<Integer> values, int value) {
        if (value < 0) {
            throw new DemoInputException("value", "error.keyMustBeNonNegative", Map.of("value", value));
        }
        for (int existing : values) {
            if (existing < 0) {
                throw new DemoInputException("values", "error.keyMustBeNonNegative", Map.of("value", existing));
            }
        }
    }
}
```

- [ ] **Step 5: Run the test, add the fixture, add the catalog entries**

```bash
cd backend
mvn -B test -Dtest=HashSetAddDemoTest
```

Then register `hash-set-add` in `GoldenTraceTest`, run `mvn -B test -Dtest=GoldenTraceTest -Dgolden.update=true`, and add `hash-set-add.json` to the fixture list imported by `frontend/src/i18n/catalog.test.ts`.

Add to both catalogs, English:

```json
  "demo.hashSetAdd.title": "HashSet add",
  "demo.hashSetAdd.description": "A set is a map that stores keys and no values. Adding a value that is already there changes nothing. Didactic: the bucket is value % capacity, with no resizing.",
  "demo.hashSetAdd.param.values": "Values already in the set",
  "demo.hashSetAdd.param.value": "Value to add",
  "hashSetAdd.bucket": "{value} % {capacity} = {bucket}, look in bucket {bucket}",
  "hashSetAdd.compare": "{value} is not {other}, keep walking the chain",
  "hashSetAdd.duplicate": "{value} is already in the set, return false and change nothing",
  "hashSetAdd.insert": "the chain has no {value}, add it to bucket {bucket}"
```

Portuguese:

```json
  "demo.hashSetAdd.title": "HashSet add",
  "demo.hashSetAdd.description": "Um set é um mapa que guarda chaves e nenhum valor. Adicionar um valor que já está lá não muda nada. Didático: o bucket é value % capacidade, sem redimensionar.",
  "demo.hashSetAdd.param.values": "Valores já no set",
  "demo.hashSetAdd.param.value": "Valor a adicionar",
  "hashSetAdd.bucket": "{value} % {capacity} = {bucket}, procura no bucket {bucket}",
  "hashSetAdd.compare": "{value} não é {other}, segue andando na corrente",
  "hashSetAdd.duplicate": "{value} já está no set, retorna false e não muda nada",
  "hashSetAdd.insert": "a corrente não tem {value}, adiciona no bucket {bucket}"
```

- [ ] **Step 6: Run everything and commit**

```bash
cd backend && mvn -B verify
cd ../frontend && npm test && npm run build
git add backend/src frontend/src
git commit -m "feat: add the hash set add demo"
```

---

### Task 6: `bst-insert`

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/TreeView.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/trace/ViewPayload.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/structures/BstInsert.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/structures/BstInsertDemo.java`
- Create: `backend/src/test/java/dev/klleriston/fundamentals/core/structures/BstInsertDemoTest.java`
- Modify: `backend/src/test/java/dev/klleriston/fundamentals/api/GoldenTraceTest.java`
- Create: `frontend/src/components/views/TreeView.tsx`, `TreeView.test.tsx`
- Modify: `frontend/src/components/views/registry.tsx`, `frontend/src/types.ts`
- Modify: `frontend/src/i18n/pt.json`, `en.json`, `catalog.test.ts`
- Generated: `frontend/src/fixtures/bst-insert.json`

**Interfaces:**
- Consumes: `Tracer.step`, `Demo`, `ParameterSpec` and `DemoInputException` as defined in Task 1.
- Produces: `TreeView` with `Node(int id, int value, Integer left, Integer right)`, used by no later task.

- [ ] **Step 1: Write the failing demo test**

Create `backend/src/test/java/dev/klleriston/fundamentals/core/structures/BstInsertDemoTest.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceStep;
import dev.klleriston.fundamentals.trace.TreeView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BstInsertDemoTest {

    private final BstInsertDemo demo = new BstInsertDemo();

    private Trace run(Map<String, Object> raw) {
        return demo.run(DemoParams.of(raw, demo.parameters()));
    }

    @Test
    void exposesItsIdentity() {
        assertThat(demo.id()).isEqualTo("bst-insert");
        assertThat(demo.category()).isEqualTo(Category.DATA_STRUCTURES);
        assertThat(demo.displaySource()).doesNotContain("tracer");
    }

    @Test
    void descendsComparingUntilItFindsAnEmptySpot() {
        Trace trace = run(Map.of("values", List.of(50, 30, 70, 20, 40, 60), "value", 35));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("bstInsert.compareLess", "bstInsert.compareGreater",
                        "bstInsert.compareLess", "bstInsert.attachLeft");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(5, 7, 5, 3);

        TreeView last = (TreeView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.path()).hasSize(3);
        assertThat(last.insertedNode()).isNotNull();
        assertThat(last.nodes()).extracting(TreeView.Node::value).contains(35);
    }

    @Test
    void changesNothingWhenTheValueIsAlreadyInTheTree() {
        Trace trace = run(Map.of("values", List.of(50, 30, 70), "value", 30));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("bstInsert.compareLess", "bstInsert.duplicate");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(5, 7);

        TreeView last = (TreeView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.insertedNode()).isNull();
        assertThat(last.nodes()).hasSize(3);
    }

    @Test
    void attachesToTheRootWhenTheTreeIsEmpty() {
        Trace trace = run(Map.of("values", List.of(), "value", 42));

        assertThat(trace.steps()).extracting(TraceStep::messageKey).containsExactly("bstInsert.attachRoot");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(3);
    }

    @Test
    void everyNodeReferencesChildrenThatExist() {
        Trace trace = run(Map.of("values", List.of(50, 30, 70, 20, 40, 60), "value", 35));

        TreeView last = (TreeView) trace.steps().get(trace.steps().size() - 1).view();
        List<Integer> ids = last.nodes().stream().map(TreeView.Node::id).toList();
        assertThat(last.nodes()).allSatisfy(node -> {
            if (node.left() != null) {
                assertThat(ids).contains(node.left());
            }
            if (node.right() != null) {
                assertThat(ids).contains(node.right());
            }
        });
    }
}
```

The last test guards the flat-list encoding: a dangling child id would render as a missing node and no other test would see it.

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && mvn -B test -Dtest=BstInsertDemoTest`
Expected: compilation failure — `TreeView` and `BstInsertDemo` do not exist.

- [ ] **Step 3: Add the view payload**

Create `backend/src/main/java/dev/klleriston/fundamentals/trace/TreeView.java`:

```java
package dev.klleriston.fundamentals.trace;

import java.util.List;

public record TreeView(
        List<Node> nodes,
        Integer activeNode,
        List<Integer> path,
        Integer insertedNode) implements ViewPayload {

    public TreeView {
        nodes = List.copyOf(nodes);
        path = List.copyOf(path);
    }

    @Override
    public String kind() {
        return "TREE";
    }

    public record Node(int id, int value, Integer left, Integer right) {
    }
}
```

Add `TreeView` to the `permits` clause of `trace/ViewPayload.java`.

- [ ] **Step 4: Write the traced algorithm**

Create `backend/src/main/java/dev/klleriston/fundamentals/core/structures/BstInsert.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.trace.Tracer;
import dev.klleriston.fundamentals.trace.TreeView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BstInsert {

    private BstInsert() {
    }

    private static final class Node {
        final int id;
        final int value;
        Node left;
        Node right;

        Node(int id, int value) {
            this.id = id;
            this.value = value;
        }
    }

    public static boolean insert(List<Integer> values, int value, Tracer tracer) {
        List<Node> all = new ArrayList<>();
        Node root = null;
        for (int existing : values) {
            root = attach(root, existing, all);
        }

        List<Integer> path = new ArrayList<>();
        Node current = root;
        while (current != null) {
            path.add(current.id);
            if (value < current.value) {
                tracer.step(5, "bstInsert.compareLess", args("value", value, "node", current.value),
                        vars(value, current.value), view(all, current.id, path, null));
                if (current.left == null) {
                    Node inserted = attachTo(current, value, all, true);
                    tracer.step(3, "bstInsert.attachLeft", args("value", value, "parent", current.value),
                            vars(value, current.value), view(all, inserted.id, path, inserted.id));
                    return true;
                }
                current = current.left;
            } else if (value > current.value) {
                tracer.step(7, "bstInsert.compareGreater", args("value", value, "node", current.value),
                        vars(value, current.value), view(all, current.id, path, null));
                if (current.right == null) {
                    Node inserted = attachTo(current, value, all, false);
                    tracer.step(3, "bstInsert.attachRight", args("value", value, "parent", current.value),
                            vars(value, current.value), view(all, inserted.id, path, inserted.id));
                    return true;
                }
                current = current.right;
            } else {
                tracer.step(7, "bstInsert.duplicate", args("value", value),
                        vars(value, current.value), view(all, current.id, path, null));
                return false;
            }
        }

        Node inserted = attach(null, value, all);
        tracer.step(3, "bstInsert.attachRoot", args("value", value),
                vars(value, value), view(all, inserted.id, List.of(inserted.id), inserted.id));
        return true;
    }

    private static Node attach(Node root, int value, List<Node> all) {
        if (root == null) {
            Node node = new Node(all.size(), value);
            all.add(node);
            return node;
        }
        Node current = root;
        while (true) {
            if (value < current.value) {
                if (current.left == null) {
                    attachTo(current, value, all, true);
                    return root;
                }
                current = current.left;
            } else if (value > current.value) {
                if (current.right == null) {
                    attachTo(current, value, all, false);
                    return root;
                }
                current = current.right;
            } else {
                return root;
            }
        }
    }

    private static Node attachTo(Node parent, int value, List<Node> all, boolean left) {
        Node node = new Node(all.size(), value);
        all.add(node);
        if (left) {
            parent.left = node;
        } else {
            parent.right = node;
        }
        return node;
    }

    private static Map<String, Object> vars(int value, int node) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("value", value);
        vars.put("node", node);
        return vars;
    }

    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }

    private static TreeView view(List<Node> all, Integer activeNode, List<Integer> path, Integer insertedNode) {
        List<TreeView.Node> nodes = all.stream()
                .map(node -> new TreeView.Node(node.id, node.value,
                        node.left == null ? null : node.left.id,
                        node.right == null ? null : node.right.id))
                .toList();
        return new TreeView(nodes, activeNode, List.copyOf(path), insertedNode);
    }
}
```

Node ids are assigned in creation order and never reused, so the frontend can key React elements by id and the fixture stays stable between runs.

- [ ] **Step 5: Write the demo**

Create `backend/src/main/java/dev/klleriston/fundamentals/core/structures/BstInsertDemo.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import dev.klleriston.fundamentals.trace.Tracer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class BstInsertDemo implements Demo {

    private static final String SOURCE = """
            public Node insert(Node node, int value) {
                if (node == null) {
                    return new Node(value);
                }
                if (value < node.value) {
                    node.left = insert(node.left, value);
                } else if (value > node.value) {
                    node.right = insert(node.right, value);
                }
                return node;
            }""";

    @Override
    public String id() {
        return "bst-insert";
    }

    @Override
    public String titleKey() {
        return "demo.bstInsert.title";
    }

    @Override
    public Category category() {
        return Category.DATA_STRUCTURES;
    }

    @Override
    public String descriptionKey() {
        return "demo.bstInsert.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("values", "demo.bstInsert.param.values", 31, List.of(50, 30, 70, 20, 40, 60)),
                ParameterSpec.integer("value", "demo.bstInsert.param.value", -1000, 1000, 35));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        List<Integer> values = Arrays.stream(params.intArray("values")).boxed().toList();
        int value = params.integer("value");

        Tracer tracer = new Tracer();
        boolean inserted = BstInsert.insert(values, value, tracer);

        return new Trace(id(), titleKey(), SOURCE, tracer.steps(),
                new TraceResult(inserted, tracer.steps().size(), false));
    }
}
```

- [ ] **Step 6: Run the test and add the fixture**

```bash
cd backend
mvn -B test -Dtest=BstInsertDemoTest
```

Register `bst-insert` in `GoldenTraceTest`, regenerate with `-Dgolden.update=true`, and add `bst-insert.json` to the fixture list in `frontend/src/i18n/catalog.test.ts`.

- [ ] **Step 7: Add the TypeScript type**

In `frontend/src/types.ts`:

```ts
export interface TreeViewPayload {
  kind: 'TREE';
  nodes: { id: number; value: number; left: number | null; right: number | null }[];
  activeNode: number | null;
  path: number[];
  insertedNode: number | null;
}
```

Add it to the `ViewPayload` union before `UnknownViewPayload`.

- [ ] **Step 8: Write the failing renderer test**

Create `frontend/src/components/views/TreeView.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import fixture from '../../fixtures/bst-insert.json';
import type { Trace, TreeViewPayload } from '../../types';
import { TreeView } from './TreeView';

const trace = fixture as unknown as Trace;

describe('TreeView', () => {
  it('draws one element per node', () => {
    const view = trace.steps[0].view as TreeViewPayload;
    render(<TreeView view={view} />);
    expect(screen.getAllByTestId(/^node-/)).toHaveLength(view.nodes.length);
  });

  it('marks the nodes on the path taken so far', () => {
    const view = trace.steps[trace.steps.length - 1].view as TreeViewPayload;
    render(<TreeView view={view} />);
    for (const id of view.path) {
      expect(screen.getByTestId(`node-${id}`)).toHaveAttribute('data-on-path', 'true');
    }
  });

  it('marks the inserted node', () => {
    const view = trace.steps[trace.steps.length - 1].view as TreeViewPayload;
    render(<TreeView view={view} />);
    expect(screen.getByTestId(`node-${view.insertedNode}`)).toHaveAttribute('data-inserted', 'true');
  });
});
```

- [ ] **Step 9: Run it and watch it fail**

Run: `cd frontend && npm test -- TreeView`
Expected: FAIL — `./TreeView` does not exist.

- [ ] **Step 10: Write the renderer**

Create `frontend/src/components/views/TreeView.tsx`. The backend sends a flat list, so the renderer computes the layout: the horizontal position is the node's rank in an in-order walk, the vertical position is its depth.

```tsx
import type { TreeViewPayload } from '../../types';

interface Placed {
  id: number;
  value: number;
  column: number;
  depth: number;
}

function place(view: TreeViewPayload): Placed[] {
  const byId = new Map(view.nodes.map((node) => [node.id, node]));
  const root = view.nodes.find((node) => !view.nodes.some((other) => other.left === node.id || other.right === node.id));
  const placed: Placed[] = [];
  let column = 0;

  const walk = (id: number | null, depth: number) => {
    if (id === null) return;
    const node = byId.get(id);
    if (node === undefined) return;
    walk(node.left, depth + 1);
    placed.push({ id: node.id, value: node.value, column: column++, depth });
    walk(node.right, depth + 1);
  };

  walk(root?.id ?? null, 0);
  return placed;
}

export function TreeView({ view }: { view: TreeViewPayload }) {
  const placed = place(view);
  const depth = Math.max(0, ...placed.map((node) => node.depth));

  return (
    <div className="tree" style={{ height: `${(depth + 1) * 64}px` }}>
      {placed.map((node) => (
        <span
          key={node.id}
          className="tree-node"
          data-testid={`node-${node.id}`}
          data-on-path={view.path.includes(node.id)}
          data-active={node.id === view.activeNode}
          data-inserted={node.id === view.insertedNode}
          style={{ left: `${node.column * 56}px`, top: `${node.depth * 64}px` }}
        >
          {node.value}
        </span>
      ))}
    </div>
  );
}
```

Register `TREE: TreeView` in `frontend/src/components/views/registry.tsx`, and style `.tree` as `position: relative` with `.tree-node` absolutely positioned, in the existing dark palette. The container scrolls horizontally when the tree is wide.

- [ ] **Step 11: Add the catalog entries**

English:

```json
  "demo.bstInsert.title": "Binary search tree insert",
  "demo.bstInsert.description": "Every value smaller than a node goes left and every larger one goes right, so inserting is a walk down from the root.",
  "demo.bstInsert.param.values": "Values already in the tree, in insertion order",
  "demo.bstInsert.param.value": "Value to insert",
  "bstInsert.compareLess": "{value} < {node}, go left",
  "bstInsert.compareGreater": "{value} > {node}, go right",
  "bstInsert.duplicate": "{value} is already in the tree, change nothing",
  "bstInsert.attachLeft": "there is nothing to the left of {parent}, attach {value} there",
  "bstInsert.attachRight": "there is nothing to the right of {parent}, attach {value} there",
  "bstInsert.attachRoot": "the tree is empty, {value} becomes the root"
```

Portuguese:

```json
  "demo.bstInsert.title": "Inserção em árvore binária de busca",
  "demo.bstInsert.description": "Todo valor menor que um nó vai para a esquerda e todo maior vai para a direita, então inserir é descer a partir da raiz.",
  "demo.bstInsert.param.values": "Valores já na árvore, na ordem de inserção",
  "demo.bstInsert.param.value": "Valor a inserir",
  "bstInsert.compareLess": "{value} < {node}, vai para a esquerda",
  "bstInsert.compareGreater": "{value} > {node}, vai para a direita",
  "bstInsert.duplicate": "{value} já está na árvore, não muda nada",
  "bstInsert.attachLeft": "não há nada à esquerda de {parent}, prende {value} ali",
  "bstInsert.attachRight": "não há nada à direita de {parent}, prende {value} ali",
  "bstInsert.attachRoot": "a árvore está vazia, {value} vira a raiz"
```

- [ ] **Step 12: Run everything and commit**

```bash
cd backend && mvn -B verify
cd ../frontend && npm test && npm run build
git add backend/src frontend/src
git commit -m "feat: add the binary search tree insert demo"
```

---

### Task 7: `linked-list-insert`

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/LinkedListView.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/trace/ViewPayload.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/structures/LinkedListInsert.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/structures/LinkedListInsertDemo.java`
- Create: `backend/src/test/java/dev/klleriston/fundamentals/core/structures/LinkedListInsertDemoTest.java`
- Modify: `backend/src/test/java/dev/klleriston/fundamentals/api/GoldenTraceTest.java`
- Create: `frontend/src/components/views/LinkedListView.tsx`, `LinkedListView.test.tsx`
- Modify: `frontend/src/components/views/registry.tsx`, `frontend/src/types.ts`
- Modify: `frontend/src/i18n/pt.json`, `en.json`, `catalog.test.ts`
- Generated: `frontend/src/fixtures/linked-list-insert.json`

**Interfaces:**
- Consumes: `Tracer.step`, `Demo`, `ParameterSpec`, `DemoInputException` from Task 1.
- Produces: `LinkedListView` with `Node(int id, int value, Integer next)` and `Link(Integer from, Integer to)`.

- [ ] **Step 1: Write the failing demo test**

Create `backend/src/test/java/dev/klleriston/fundamentals/core/structures/LinkedListInsertDemoTest.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.LinkedListView;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkedListInsertDemoTest {

    private final LinkedListInsertDemo demo = new LinkedListInsertDemo();

    private Trace run(Map<String, Object> raw) {
        return demo.run(DemoParams.of(raw, demo.parameters()));
    }

    @Test
    void exposesItsIdentity() {
        assertThat(demo.id()).isEqualTo("linked-list-insert");
        assertThat(demo.category()).isEqualTo(Category.DATA_STRUCTURES);
        assertThat(demo.displaySource()).doesNotContain("tracer");
    }

    @Test
    void walksToThePositionThenRewiresTwoPointers() {
        Trace trace = run(Map.of("values", List.of(10, 20, 30, 40), "position", 2, "value", 25));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("linkedListInsert.walk", "linkedListInsert.arrive",
                        "linkedListInsert.linkNew", "linkedListInsert.relinkPrev");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(8, 7, 10, 11);

        LinkedListView last = (LinkedListView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.newNode()).isNotNull();
        assertThat(last.changedLinks()).hasSize(1);
        assertThat(valuesInOrder(last)).containsExactly(10, 20, 25, 30, 40);
    }

    @Test
    void insertsAtTheHeadWithoutWalking() {
        Trace trace = run(Map.of("values", List.of(10, 20), "position", 0, "value", 5));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("linkedListInsert.insertHead");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(3);

        LinkedListView last = (LinkedListView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(valuesInOrder(last)).containsExactly(5, 10, 20);
    }

    @Test
    void rejectsAPositionPastTheEnd() {
        assertThatThrownBy(() -> run(Map.of("values", List.of(10, 20), "position", 5, "value", 1)))
                .isInstanceOf(DemoInputException.class)
                .satisfies(thrown -> assertThat(((DemoInputException) thrown).field()).isEqualTo("position"));
    }

    private static List<Integer> valuesInOrder(LinkedListView view) {
        List<Integer> values = new java.util.ArrayList<>();
        LinkedListView.Node head = view.nodes().stream()
                .filter(node -> view.nodes().stream().noneMatch(other -> other.next() != null && other.next() == node.id()))
                .findFirst()
                .orElseThrow();
        LinkedListView.Node current = head;
        while (current != null) {
            values.add(current.value());
            Integer next = current.next();
            current = next == null ? null
                    : view.nodes().stream().filter(node -> node.id() == next).findFirst().orElseThrow();
        }
        return values;
    }
}
```

`valuesInOrder` walks the `next` pointers rather than trusting list order — that is the whole point of a linked list, and it catches a rewiring bug that a positional assertion would miss.

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && mvn -B test -Dtest=LinkedListInsertDemoTest`
Expected: compilation failure — `LinkedListView` and `LinkedListInsertDemo` do not exist.

- [ ] **Step 3: Add the view payload**

Create `backend/src/main/java/dev/klleriston/fundamentals/trace/LinkedListView.java`:

```java
package dev.klleriston.fundamentals.trace;

import java.util.List;

public record LinkedListView(
        List<Node> nodes,
        Integer cursor,
        List<Link> changedLinks,
        Integer newNode) implements ViewPayload {

    public LinkedListView {
        nodes = List.copyOf(nodes);
        changedLinks = List.copyOf(changedLinks);
    }

    @Override
    public String kind() {
        return "LINKED_LIST";
    }

    public record Node(int id, int value, Integer next) {
    }

    public record Link(Integer from, Integer to) {
    }
}
```

Add `LinkedListView` to the `permits` clause of `trace/ViewPayload.java`.

- [ ] **Step 4: Write the traced algorithm**

Create `backend/src/main/java/dev/klleriston/fundamentals/core/structures/LinkedListInsert.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.trace.LinkedListView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LinkedListInsert {

    private LinkedListInsert() {
    }

    private static final class Node {
        final int id;
        final int value;
        Node next;

        Node(int id, int value) {
            this.id = id;
            this.value = value;
        }
    }

    public static List<Integer> insert(List<Integer> values, int position, int value, Tracer tracer) {
        List<Node> all = new ArrayList<>();
        Node head = null;
        Node tail = null;
        for (int existing : values) {
            Node node = new Node(all.size(), existing);
            all.add(node);
            if (head == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        if (position == 0) {
            Node inserted = new Node(all.size(), value);
            all.add(inserted);
            inserted.next = head;
            head = inserted;
            tracer.step(3, "linkedListInsert.insertHead", args("value", value),
                    vars(position, value), view(all, head, inserted.id,
                            List.of(new LinkedListView.Link(inserted.id, inserted.next == null ? null : inserted.next.id)),
                            inserted.id));
            return valuesOf(head);
        }

        Node previous = head;
        for (int i = 0; i < position - 1; i++) {
            previous = previous.next;
            tracer.step(8, "linkedListInsert.walk", args("index", i + 1, "value", previous.value),
                    vars(position, value), view(all, head, previous.id, List.of(), null));
        }
        tracer.step(7, "linkedListInsert.arrive", args("position", position),
                vars(position, value), view(all, head, previous.id, List.of(), null));

        Node inserted = new Node(all.size(), value);
        all.add(inserted);
        inserted.next = previous.next;
        tracer.step(10, "linkedListInsert.linkNew",
                args("value", value, "next", inserted.next == null ? "null" : inserted.next.value),
                vars(position, value), view(all, head, inserted.id,
                        List.of(new LinkedListView.Link(inserted.id, inserted.next == null ? null : inserted.next.id)),
                        inserted.id));

        previous.next = inserted;
        tracer.step(11, "linkedListInsert.relinkPrev", args("prev", previous.value, "value", value),
                vars(position, value), view(all, head, inserted.id,
                        List.of(new LinkedListView.Link(previous.id, inserted.id)), inserted.id));

        return valuesOf(head);
    }

    private static List<Integer> valuesOf(Node head) {
        List<Integer> values = new ArrayList<>();
        for (Node node = head; node != null; node = node.next) {
            values.add(node.value);
        }
        return values;
    }

    private static Map<String, Object> vars(int position, int value) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("position", position);
        vars.put("value", value);
        return vars;
    }

    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }

    private static LinkedListView view(List<Node> all, Node head, Integer cursor,
                                       List<LinkedListView.Link> changedLinks, Integer newNode) {
        List<LinkedListView.Node> nodes = new ArrayList<>();
        for (Node node = head; node != null; node = node.next) {
            nodes.add(new LinkedListView.Node(node.id, node.value, node.next == null ? null : node.next.id));
        }
        return new LinkedListView(nodes, cursor, changedLinks, newNode);
    }
}
```

The view walks from the head rather than iterating `all`, so a node that is not yet linked in does not appear on screen before the step that links it.

- [ ] **Step 5: Write the demo**

Create `backend/src/main/java/dev/klleriston/fundamentals/core/structures/LinkedListInsertDemo.java`:

```java
package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import dev.klleriston.fundamentals.trace.Tracer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class LinkedListInsertDemo implements Demo {

    private static final String SOURCE = """
            public void insert(int position, int value) {
                if (position == 0) {
                    head = new Node(value, head);
                    return;
                }
                Node previous = head;
                for (int i = 0; i < position - 1; i++) {
                    previous = previous.next;
                }
                Node node = new Node(value, previous.next);
                previous.next = node;
                size++;
            }""";

    @Override
    public String id() {
        return "linked-list-insert";
    }

    @Override
    public String titleKey() {
        return "demo.linkedListInsert.title";
    }

    @Override
    public Category category() {
        return Category.DATA_STRUCTURES;
    }

    @Override
    public String descriptionKey() {
        return "demo.linkedListInsert.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("values", "demo.linkedListInsert.param.values", 32, List.of(10, 20, 30, 40)),
                ParameterSpec.integer("position", "demo.linkedListInsert.param.position", 0, 32, 2),
                ParameterSpec.integer("value", "demo.linkedListInsert.param.value", -1000, 1000, 25));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        List<Integer> values = Arrays.stream(params.intArray("values")).boxed().toList();
        int position = params.integer("position");
        int value = params.integer("value");
        if (position > values.size()) {
            throw new DemoInputException("position", "error.positionPastTheEnd",
                    Map.of("position", position, "size", values.size()));
        }

        Tracer tracer = new Tracer();
        List<Integer> result = LinkedListInsert.insert(values, position, value, tracer);

        return new Trace(id(), titleKey(), SOURCE, tracer.steps(),
                new TraceResult(result, tracer.steps().size(), false));
    }
}
```

Inserting at `position == values.size()` is valid — that appends. Only a position past that is rejected.

- [ ] **Step 6: Run the test and add the fixture**

```bash
cd backend
mvn -B test -Dtest=LinkedListInsertDemoTest
```

Register `linked-list-insert` in `GoldenTraceTest`, regenerate with `-Dgolden.update=true`, and add `linked-list-insert.json` to the fixture list in `frontend/src/i18n/catalog.test.ts`.

- [ ] **Step 7: Add the TypeScript type**

In `frontend/src/types.ts`:

```ts
export interface LinkedListViewPayload {
  kind: 'LINKED_LIST';
  nodes: { id: number; value: number; next: number | null }[];
  cursor: number | null;
  changedLinks: { from: number | null; to: number | null }[];
  newNode: number | null;
}
```

Add it to the `ViewPayload` union before `UnknownViewPayload`.

- [ ] **Step 8: Write the failing renderer test**

Create `frontend/src/components/views/LinkedListView.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import fixture from '../../fixtures/linked-list-insert.json';
import type { LinkedListViewPayload, Trace } from '../../types';
import { LinkedListView } from './LinkedListView';

const trace = fixture as unknown as Trace;

describe('LinkedListView', () => {
  it('draws the nodes in the order the pointers say', () => {
    const view = trace.steps[trace.steps.length - 1].view as LinkedListViewPayload;
    render(<LinkedListView view={view} />);
    const rendered = screen.getAllByTestId(/^list-node-/).map((node) => node.textContent);
    expect(rendered).toEqual(view.nodes.map((node) => String(node.value)));
  });

  it('marks the node under the cursor', () => {
    const view = trace.steps[0].view as LinkedListViewPayload;
    render(<LinkedListView view={view} />);
    expect(screen.getByTestId(`list-node-${view.cursor}`)).toHaveAttribute('data-cursor', 'true');
  });

  it('marks the rewired link on the last step', () => {
    const view = trace.steps[trace.steps.length - 1].view as LinkedListViewPayload;
    render(<LinkedListView view={view} />);
    expect(screen.getAllByTestId(/^link-/)).not.toHaveLength(0);
    for (const link of view.changedLinks) {
      expect(screen.getByTestId(`link-${link.from}`)).toHaveAttribute('data-changed', 'true');
    }
  });
});
```

- [ ] **Step 9: Run it and watch it fail**

Run: `cd frontend && npm test -- LinkedListView`
Expected: FAIL — `./LinkedListView` does not exist.

- [ ] **Step 10: Write the renderer**

Create `frontend/src/components/views/LinkedListView.tsx`:

```tsx
import type { LinkedListViewPayload } from '../../types';

export function LinkedListView({ view }: { view: LinkedListViewPayload }) {
  const changed = new Set(view.changedLinks.map((link) => link.from));

  return (
    <div className="linked-list">
      {view.nodes.map((node) => (
        <div key={node.id} className="linked-list-cell">
          <span
            className="linked-list-node"
            data-testid={`list-node-${node.id}`}
            data-cursor={node.id === view.cursor}
            data-new={node.id === view.newNode}
          >
            {node.value}
          </span>
          {node.next !== null && (
            <span
              className="linked-list-link"
              data-testid={`link-${node.id}`}
              data-changed={changed.has(node.id)}
              aria-hidden="true"
            >
              →
            </span>
          )}
        </div>
      ))}
    </div>
  );
}
```

Register `LINKED_LIST: LinkedListView` in `frontend/src/components/views/registry.tsx`, and style `.linked-list` as a horizontally scrolling row with `.linked-list-node[data-new='true']` and `.linked-list-link[data-changed='true']` picked out in the accent colour.

- [ ] **Step 11: Add the catalog entries**

English:

```json
  "demo.linkedListInsert.title": "Linked list insert",
  "demo.linkedListInsert.description": "There is no index to jump to: reaching a position means following next pointers one at a time, and inserting means rewiring two of them.",
  "demo.linkedListInsert.param.values": "Values already in the list",
  "demo.linkedListInsert.param.position": "Position to insert at",
  "demo.linkedListInsert.param.value": "Value to insert",
  "linkedListInsert.walk": "follow next to node {index}, which holds {value}",
  "linkedListInsert.arrive": "this is the node before position {position}, stop walking",
  "linkedListInsert.linkNew": "point the new node holding {value} at {next}",
  "linkedListInsert.relinkPrev": "point {prev} at {value}, and the insertion is done",
  "linkedListInsert.insertHead": "position 0 needs no walking: {value} becomes the new head",
  "error.positionPastTheEnd": "Position {position} is past the end of a list with {size} values"
```

Portuguese:

```json
  "demo.linkedListInsert.title": "Inserção em lista ligada",
  "demo.linkedListInsert.description": "Não existe índice para pular direto: chegar numa posição é seguir os ponteiros next um a um, e inserir é religar dois deles.",
  "demo.linkedListInsert.param.values": "Valores já na lista",
  "demo.linkedListInsert.param.position": "Posição onde inserir",
  "demo.linkedListInsert.param.value": "Valor a inserir",
  "linkedListInsert.walk": "segue o next até o nó {index}, que guarda {value}",
  "linkedListInsert.arrive": "este é o nó antes da posição {position}, para de andar",
  "linkedListInsert.linkNew": "aponta o nó novo com {value} para {next}",
  "linkedListInsert.relinkPrev": "aponta {prev} para {value}, e a inserção acabou",
  "linkedListInsert.insertHead": "posição 0 não precisa andar: {value} vira a nova cabeça",
  "error.positionPastTheEnd": "A posição {position} passa do fim de uma lista com {size} valores"
```

- [ ] **Step 12: Run everything and commit**

```bash
cd backend && mvn -B verify
cd ../frontend && npm test && npm run build
git add backend/src frontend/src
git commit -m "feat: add the linked list insert demo"
```

---

## Dependencies

Task 1 blocks everything: it changes `Tracer.step`, `Demo` and `ParameterSpec`, which every other task calls. Task 5 depends on Task 4 for `HashTableView` and its renderer. Tasks 2, 3, 6 and 7 touch disjoint files once Task 1 has landed and can run in parallel.

```
Task 1 ──┬── Task 2
         ├── Task 3
         ├── Task 4 ── Task 5
         ├── Task 6
         └── Task 7
```
