# Phase 1 — Trace Pipeline and ARRAY Demos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a working web app where a learner picks an input, presses Run, and watches `binary-search` and `bubble-sort` execute step by step with the matching Java line highlighted.

**Architecture:** A Spring Boot backend runs real Java algorithms that call a `Tracer` at the points worth seeing, producing an immutable `Trace` (a list of steps, each with a line number, a message, variables, and a view payload). One REST call returns the whole trace; a React frontend holds it in memory and plays it back locally. Demos self-register as Spring beans, and the frontend picks a renderer by `view.kind`, so later phases add files without changing existing ones.

**Tech Stack:** Java 21, Spring Boot 3.3.x, Maven, JUnit 5, MockMvc, Jackson. React 18, TypeScript 5, Vite 5, Vitest, React Testing Library, MSW, React Router 6. GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-08-19-visual-jvm-algorithms-design.md`

## Global Constraints

- Java 21. Maven (not Gradle). Spring Boot 3.3.x.
- Base Java package: `dev.klleriston.fundamentals`.
- Backend lives in `backend/`, frontend in `frontend/`. No parent POM; the two builds are independent and CI runs both.
- Layer dependency direction is one-way: `api` → `core` → `trace`. Classes in `trace` and `core` must not import Spring web types or Jackson annotations.
- Step cap per trace: 10,000. Array length cap: 512. In-process execution timeout: 5 seconds.
- Every validation rejection is HTTP 400 with a JSON body `{"error": ..., "message": ..., "field": ...}`; `field` may be `null`.
- All API JSON uses camelCase. Enum values in JSON are UPPER_SNAKE_CASE.
- All user-visible strings in code, tests, and commits are in English.
- Commit after every task, using Conventional Commits (`feat:`, `test:`, `chore:`, `docs:`, `ci:`).

---

### Task 1: Backend skeleton that starts and answers

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/FundamentalsApplication.java`
- Create: `backend/src/main/resources/application.yaml`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/FundamentalsApplicationTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: a runnable Spring Boot app on port 8080; Maven commands `mvn -B verify` from `backend/`.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/FundamentalsApplicationTest.java`:

```java
package dev.klleriston.fundamentals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FundamentalsApplicationTest {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test`
Expected: FAIL — there is no `pom.xml` yet, so Maven cannot run at all.

- [ ] **Step 3: Write minimal implementation**

`backend/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
  </parent>

  <groupId>dev.klleriston</groupId>
  <artifactId>fundamentals-backend</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <name>fundamentals-backend</name>

  <properties>
    <java.version>21</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

`backend/src/main/java/dev/klleriston/fundamentals/FundamentalsApplication.java`:

```java
package dev.klleriston.fundamentals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FundamentalsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundamentalsApplication.class, args);
    }
}
```

`backend/src/main/resources/application.yaml`:

```yaml
server:
  port: 8080
spring:
  application:
    name: fundamentals-backend
  jackson:
    default-property-inclusion: non_null
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test`
Expected: PASS — `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat: add Spring Boot backend skeleton"
```

---

### Task 2: Trace model

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/ViewPayload.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/ArrayView.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/TraceStep.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/TraceResult.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/Trace.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/trace/TraceTest.java`

**Interfaces:**
- Consumes: Task 1's project layout.
- Produces:
  - `sealed interface ViewPayload permits ArrayView { String kind(); }`
  - `record ArrayView(List<Integer> items, Map<String,Integer> pointers, List<ArrayView.Range> ranges, List<Integer> swapped)` with nested `record Range(int from, int to, String state)` and `kind()` returning `"ARRAY"`.
  - `record TraceStep(int index, int line, String message, Map<String,Object> vars, ViewPayload view)`
  - `record TraceResult(Object returnValue, int stepCount, boolean measured)`
  - `record Trace(String demoId, String title, String sourceCode, List<TraceStep> steps, TraceResult result)`

The maps are copied into a `LinkedHashMap`, not with `Map.copyOf`: `Map.copyOf` returns an immutable map whose iteration order is randomized per JVM run, which would make the serialized JSON — and therefore the golden fixtures in Task 9 — differ between runs. Insertion order is also the order the learner reads the variables in.

Note for the implementer: `ViewPayload` is `sealed` and later phases add permitted types. When Phase 2 adds `TreeView`, it is added to the `permits` clause — that is the one intentional edit to this file per phase.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/trace/TraceTest.java`:

```java
package dev.klleriston.fundamentals.trace;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceTest {

    private static ArrayView sampleView() {
        return new ArrayView(List.of(2, 5, 8), Map.of("low", 0), List.of(), List.of());
    }

    @Test
    void arrayViewReportsItsKind() {
        assertThat(sampleView().kind()).isEqualTo("ARRAY");
    }

    @Test
    void traceStepsAreUnmodifiable() {
        List<TraceStep> mutable = new ArrayList<>();
        mutable.add(new TraceStep(0, 1, "start", Map.of("low", 0), sampleView()));

        Trace trace = new Trace("binary-search", "Binary Search", "code", mutable,
                new TraceResult(1, 1, false));

        assertThatThrownBy(() -> trace.steps().add(mutable.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void traceDoesNotSeeLaterMutationsOfTheSourceList() {
        List<TraceStep> mutable = new ArrayList<>();
        mutable.add(new TraceStep(0, 1, "start", Map.of(), sampleView()));

        Trace trace = new Trace("binary-search", "Binary Search", "code", mutable,
                new TraceResult(1, 1, false));
        mutable.add(new TraceStep(1, 2, "later", Map.of(), sampleView()));

        assertThat(trace.steps()).hasSize(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest=TraceTest`
Expected: FAIL — compilation error, `ArrayView`/`TraceStep`/`Trace` do not exist.

- [ ] **Step 3: Write minimal implementation**

`ViewPayload.java`:

```java
package dev.klleriston.fundamentals.trace;

public sealed interface ViewPayload permits ArrayView {

    String kind();
}
```

`ArrayView.java`:

```java
package dev.klleriston.fundamentals.trace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ArrayView(
        List<Integer> items,
        Map<String, Integer> pointers,
        List<Range> ranges,
        List<Integer> swapped) implements ViewPayload {

    public ArrayView {
        items = List.copyOf(items);
        pointers = Collections.unmodifiableMap(new LinkedHashMap<>(pointers));
        ranges = List.copyOf(ranges);
        swapped = List.copyOf(swapped);
    }

    @Override
    public String kind() {
        return "ARRAY";
    }

    public record Range(int from, int to, String state) {

        public static final String DISCARDED = "DISCARDED";
        public static final String SORTED = "SORTED";
        public static final String ACTIVE = "ACTIVE";
    }
}
```

`TraceStep.java`:

```java
package dev.klleriston.fundamentals.trace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record TraceStep(int index, int line, String message, Map<String, Object> vars, ViewPayload view) {

    public TraceStep {
        vars = Collections.unmodifiableMap(new LinkedHashMap<>(vars));
    }
}
```

`TraceResult.java`:

```java
package dev.klleriston.fundamentals.trace;

public record TraceResult(Object returnValue, int stepCount, boolean measured) {
}
```

`Trace.java`:

```java
package dev.klleriston.fundamentals.trace;

import java.util.List;

public record Trace(
        String demoId,
        String title,
        String sourceCode,
        List<TraceStep> steps,
        TraceResult result) {

    public Trace {
        steps = List.copyOf(steps);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test -Dtest=TraceTest`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/dev/klleriston/fundamentals/trace backend/src/test/java/dev/klleriston/fundamentals/trace
git commit -m "feat: add immutable trace model with ARRAY view payload"
```

---

### Task 3: Tracer with step cap

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/Tracer.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/trace/StepLimitExceededException.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/trace/TracerTest.java`

**Interfaces:**
- Consumes: `TraceStep`, `ViewPayload` from Task 2.
- Produces:
  - `Tracer()` and `Tracer(int maxSteps)`; `void step(int line, String message, Map<String,Object> vars, ViewPayload view)`; `List<TraceStep> steps()`; constant `Tracer.DEFAULT_MAX_STEPS = 10_000`.
  - `StepLimitExceededException extends RuntimeException` with `int limit()`.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/trace/TracerTest.java`:

```java
package dev.klleriston.fundamentals.trace;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TracerTest {

    private static ArrayView view() {
        return new ArrayView(List.of(1), Map.of(), List.of(), List.of());
    }

    @Test
    void assignsSequentialIndexes() {
        Tracer tracer = new Tracer();
        tracer.step(1, "first", Map.of("i", 0), view());
        tracer.step(2, "second", Map.of("i", 1), view());

        assertThat(tracer.steps()).extracting(TraceStep::index).containsExactly(0, 1);
        assertThat(tracer.steps()).extracting(TraceStep::message).containsExactly("first", "second");
    }

    @Test
    void rejectsStepsBeyondTheLimit() {
        Tracer tracer = new Tracer(2);
        tracer.step(1, "a", Map.of(), view());
        tracer.step(1, "b", Map.of(), view());

        assertThatThrownBy(() -> tracer.step(1, "c", Map.of(), view()))
                .isInstanceOf(StepLimitExceededException.class)
                .hasMessageContaining("2");
    }

    @Test
    void defaultLimitIsTenThousand() {
        assertThat(Tracer.DEFAULT_MAX_STEPS).isEqualTo(10_000);
    }

    @Test
    void returnedStepListIsUnmodifiable() {
        Tracer tracer = new Tracer();
        tracer.step(1, "a", Map.of(), view());

        List<TraceStep> steps = tracer.steps();
        assertThatThrownBy(() -> steps.add(steps.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest=TracerTest`
Expected: FAIL — compilation error, `Tracer` does not exist.

- [ ] **Step 3: Write minimal implementation**

`StepLimitExceededException.java`:

```java
package dev.klleriston.fundamentals.trace;

public class StepLimitExceededException extends RuntimeException {

    private final int limit;

    public StepLimitExceededException(int limit) {
        super("Execution produced more than " + limit + " steps. Try a smaller input.");
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }
}
```

`Tracer.java`:

```java
package dev.klleriston.fundamentals.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Tracer {

    public static final int DEFAULT_MAX_STEPS = 10_000;

    private final int maxSteps;
    private final List<TraceStep> steps = new ArrayList<>();

    public Tracer() {
        this(DEFAULT_MAX_STEPS);
    }

    public Tracer(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public void step(int line, String message, Map<String, Object> vars, ViewPayload view) {
        if (steps.size() >= maxSteps) {
            throw new StepLimitExceededException(maxSteps);
        }
        steps.add(new TraceStep(steps.size(), line, message, vars, view));
    }

    public List<TraceStep> steps() {
        return List.copyOf(steps);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test -Dtest=TracerTest`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/dev/klleriston/fundamentals/trace backend/src/test/java/dev/klleriston/fundamentals/trace
git commit -m "feat: add Tracer with step cap"
```

---

### Task 4: Parameter schema and validated input

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/ParameterType.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/ParameterSpec.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/DemoInputException.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/DemoParams.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/core/DemoParamsTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces:
  - `enum ParameterType { INT, LONG, INT_ARRAY, STRING_ARRAY, ENUM }`
  - `record ParameterSpec(String name, ParameterType type, String label, boolean required, Integer min, Integer max, Integer maxLength, List<String> options, Object defaultValue)` with static factories `integer(name, label, min, max, defaultValue)`, `intArray(name, label, maxLength, defaultValue)`, `enumOf(name, label, options, defaultValue)`.
  - `DemoInputException extends RuntimeException` with `String field()`.
  - `DemoParams.of(Map<String,Object> raw, List<ParameterSpec> specs)`; instance methods `int[] intArray(String name)`, `int integer(String name)`, `String enumValue(String name)`.

Validation rules this task owns, all raising `DemoInputException`: unknown parameter name, missing required parameter with no default, wrong type, integer outside `[min, max]`, array longer than `maxLength`, array element that is not an integer, enum value outside `options`.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/core/DemoParamsTest.java`:

```java
package dev.klleriston.fundamentals.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoParamsTest {

    private static final List<ParameterSpec> SPECS = List.of(
            ParameterSpec.intArray("array", "Sorted array", 512, List.of(2, 5, 8)),
            ParameterSpec.integer("target", "Target", -1000, 1000, 5),
            ParameterSpec.enumOf("order", "Order", List.of("ASC", "DESC"), "ASC"));

    @Test
    void readsProvidedValues() {
        DemoParams params = DemoParams.of(
                Map.of("array", List.of(1, 3, 7), "target", 7, "order", "DESC"), SPECS);

        assertThat(params.intArray("array")).containsExactly(1, 3, 7);
        assertThat(params.integer("target")).isEqualTo(7);
        assertThat(params.enumValue("order")).isEqualTo("DESC");
    }

    @Test
    void fallsBackToDefaults() {
        DemoParams params = DemoParams.of(Map.of(), SPECS);

        assertThat(params.intArray("array")).containsExactly(2, 5, 8);
        assertThat(params.integer("target")).isEqualTo(5);
        assertThat(params.enumValue("order")).isEqualTo("ASC");
    }

    @Test
    void rejectsUnknownParameter() {
        assertThatThrownBy(() -> DemoParams.of(Map.of("nope", 1), SPECS))
                .isInstanceOf(DemoInputException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void rejectsIntegerOutOfRange() {
        assertThatThrownBy(() -> DemoParams.of(Map.of("target", 5000), SPECS))
                .isInstanceOf(DemoInputException.class)
                .extracting("field").isEqualTo("target");
    }

    @Test
    void rejectsArrayLongerThanMaxLength() {
        List<ParameterSpec> shortSpec = List.of(
                ParameterSpec.intArray("array", "Array", 2, List.of(1)));

        assertThatThrownBy(() -> DemoParams.of(Map.of("array", List.of(1, 2, 3)), shortSpec))
                .isInstanceOf(DemoInputException.class)
                .hasMessageContaining("2");
    }

    @Test
    void rejectsNonIntegerArrayElement() {
        assertThatThrownBy(() -> DemoParams.of(Map.of("array", List.of("x")), SPECS))
                .isInstanceOf(DemoInputException.class)
                .extracting("field").isEqualTo("array");
    }

    @Test
    void rejectsEnumValueOutsideOptions() {
        assertThatThrownBy(() -> DemoParams.of(Map.of("order", "SIDEWAYS"), SPECS))
                .isInstanceOf(DemoInputException.class)
                .extracting("field").isEqualTo("order");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest=DemoParamsTest`
Expected: FAIL — compilation error, `DemoParams` does not exist.

- [ ] **Step 3: Write minimal implementation**

`ParameterType.java`:

```java
package dev.klleriston.fundamentals.core;

public enum ParameterType {
    INT, LONG, INT_ARRAY, STRING_ARRAY, ENUM
}
```

`ParameterSpec.java`:

```java
package dev.klleriston.fundamentals.core;

import java.util.List;

public record ParameterSpec(
        String name,
        ParameterType type,
        String label,
        boolean required,
        Integer min,
        Integer max,
        Integer maxLength,
        List<String> options,
        Object defaultValue) {

    public static ParameterSpec integer(String name, String label, int min, int max, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.INT, label, true, min, max, null, null, defaultValue);
    }

    public static ParameterSpec intArray(String name, String label, int maxLength, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.INT_ARRAY, label, true, null, null, maxLength, null, defaultValue);
    }

    public static ParameterSpec enumOf(String name, String label, List<String> options, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.ENUM, label, true, null, null, null, List.copyOf(options), defaultValue);
    }
}
```

`DemoInputException.java`:

```java
package dev.klleriston.fundamentals.core;

public class DemoInputException extends RuntimeException {

    private final String field;

    public DemoInputException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
```

`DemoParams.java`:

```java
package dev.klleriston.fundamentals.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DemoParams {

    private final Map<String, Object> values;

    private DemoParams(Map<String, Object> values) {
        this.values = values;
    }

    public static DemoParams of(Map<String, Object> raw, List<ParameterSpec> specs) {
        Map<String, ParameterSpec> byName = new LinkedHashMap<>();
        specs.forEach(spec -> byName.put(spec.name(), spec));

        for (String provided : raw.keySet()) {
            if (!byName.containsKey(provided)) {
                throw new DemoInputException(provided, "Unknown parameter: " + provided);
            }
        }

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (ParameterSpec spec : specs) {
            Object value = raw.containsKey(spec.name()) ? raw.get(spec.name()) : spec.defaultValue();
            if (value == null) {
                if (spec.required()) {
                    throw new DemoInputException(spec.name(), "Missing required parameter: " + spec.name());
                }
                continue;
            }
            resolved.put(spec.name(), coerce(spec, value));
        }
        return new DemoParams(resolved);
    }

    private static Object coerce(ParameterSpec spec, Object value) {
        return switch (spec.type()) {
            case INT, LONG -> coerceInteger(spec, value);
            case INT_ARRAY -> coerceIntArray(spec, value);
            case ENUM, STRING_ARRAY -> coerceString(spec, value);
        };
    }

    private static int coerceInteger(ParameterSpec spec, Object value) {
        if (!(value instanceof Number number)) {
            throw new DemoInputException(spec.name(), spec.name() + " must be a number");
        }
        int intValue = number.intValue();
        if (spec.min() != null && intValue < spec.min()) {
            throw new DemoInputException(spec.name(), spec.name() + " must be at least " + spec.min());
        }
        if (spec.max() != null && intValue > spec.max()) {
            throw new DemoInputException(spec.name(), spec.name() + " must be at most " + spec.max());
        }
        return intValue;
    }

    private static int[] coerceIntArray(ParameterSpec spec, Object value) {
        if (!(value instanceof List<?> list)) {
            throw new DemoInputException(spec.name(), spec.name() + " must be an array of integers");
        }
        if (spec.maxLength() != null && list.size() > spec.maxLength()) {
            throw new DemoInputException(spec.name(),
                    spec.name() + " must have at most " + spec.maxLength() + " elements");
        }
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Number number)) {
                throw new DemoInputException(spec.name(), spec.name() + " must contain only integers");
            }
            result[i] = number.intValue();
        }
        return result;
    }

    private static String coerceString(ParameterSpec spec, Object value) {
        if (!(value instanceof String text)) {
            throw new DemoInputException(spec.name(), spec.name() + " must be a string");
        }
        if (spec.options() != null && !spec.options().contains(text)) {
            throw new DemoInputException(spec.name(),
                    spec.name() + " must be one of " + String.join(", ", spec.options()));
        }
        return text;
    }

    public int[] intArray(String name) {
        return (int[]) values.get(name);
    }

    public int integer(String name) {
        return (Integer) values.get(name);
    }

    public String enumValue(String name) {
        return (String) values.get(name);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test -Dtest=DemoParamsTest`
Expected: PASS — 7 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/dev/klleriston/fundamentals/core backend/src/test/java/dev/klleriston/fundamentals/core
git commit -m "feat: add parameter schema and validated demo input"
```

---

### Task 5: Demo contract and registry

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/Category.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/Demo.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/UnknownDemoException.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/DemoRegistry.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/core/DemoRegistryTest.java`

**Interfaces:**
- Consumes: `Trace` (Task 2), `ParameterSpec`/`DemoParams` (Task 4).
- Produces:
  - `enum Category { ALGORITHMS, DATA_STRUCTURES, JVM, OOP }`
  - `interface Demo { String id(); String title(); Category category(); String description(); List<ParameterSpec> parameters(); String displaySource(); Trace run(DemoParams params); }`
  - `UnknownDemoException extends RuntimeException` with `String demoId()`.
  - `DemoRegistry` (Spring `@Component`) with `List<Demo> all()` sorted by id and `Demo require(String id)`.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/core/DemoRegistryTest.java`:

```java
package dev.klleriston.fundamentals.core;

import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoRegistryTest {

    private static Demo stub(String id) {
        return new Demo() {
            @Override public String id() { return id; }
            @Override public String title() { return id; }
            @Override public Category category() { return Category.ALGORITHMS; }
            @Override public String description() { return "stub"; }
            @Override public List<ParameterSpec> parameters() { return List.of(); }
            @Override public String displaySource() { return "code"; }
            @Override public Trace run(DemoParams params) {
                return new Trace(id, id, "code", List.of(), new TraceResult(null, 0, false));
            }
        };
    }

    @Test
    void findsDemoById() {
        DemoRegistry registry = new DemoRegistry(List.of(stub("bubble-sort"), stub("binary-search")));

        assertThat(registry.require("binary-search").id()).isEqualTo("binary-search");
    }

    @Test
    void listsDemosSortedById() {
        DemoRegistry registry = new DemoRegistry(List.of(stub("bubble-sort"), stub("binary-search")));

        assertThat(registry.all()).extracting(Demo::id).containsExactly("binary-search", "bubble-sort");
    }

    @Test
    void rejectsUnknownId() {
        DemoRegistry registry = new DemoRegistry(List.of(stub("binary-search")));

        assertThatThrownBy(() -> registry.require("nope"))
                .isInstanceOf(UnknownDemoException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void rejectsDuplicateIds() {
        assertThatThrownBy(() -> new DemoRegistry(List.of(stub("dup"), stub("dup"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dup");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest=DemoRegistryTest`
Expected: FAIL — compilation error, `Demo` and `DemoRegistry` do not exist.

- [ ] **Step 3: Write minimal implementation**

`Category.java`:

```java
package dev.klleriston.fundamentals.core;

public enum Category {
    ALGORITHMS, DATA_STRUCTURES, JVM, OOP
}
```

`Demo.java`:

```java
package dev.klleriston.fundamentals.core;

import dev.klleriston.fundamentals.trace.Trace;

import java.util.List;

public interface Demo {

    String id();

    String title();

    Category category();

    String description();

    List<ParameterSpec> parameters();

    /** Java source shown to the learner, without tracer calls. Step line numbers refer to this text. */
    String displaySource();

    Trace run(DemoParams params);
}
```

`UnknownDemoException.java`:

```java
package dev.klleriston.fundamentals.core;

public class UnknownDemoException extends RuntimeException {

    private final String demoId;

    public UnknownDemoException(String demoId) {
        super("Unknown demo: " + demoId);
        this.demoId = demoId;
    }

    public String demoId() {
        return demoId;
    }
}
```

`DemoRegistry.java`:

```java
package dev.klleriston.fundamentals.core;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemoRegistry {

    private final Map<String, Demo> byId = new LinkedHashMap<>();

    public DemoRegistry(List<Demo> demos) {
        demos.stream()
                .sorted(Comparator.comparing(Demo::id))
                .forEach(demo -> {
                    if (byId.putIfAbsent(demo.id(), demo) != null) {
                        throw new IllegalStateException("Duplicate demo id: " + demo.id());
                    }
                });
    }

    public List<Demo> all() {
        return List.copyOf(byId.values());
    }

    public Demo require(String id) {
        Demo demo = byId.get(id);
        if (demo == null) {
            throw new UnknownDemoException(id);
        }
        return demo;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test -Dtest=DemoRegistryTest`
Expected: PASS — 4 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/dev/klleriston/fundamentals/core backend/src/test/java/dev/klleriston/fundamentals/core
git commit -m "feat: add demo contract and registry"
```

---

### Task 6: Binary search algorithm and demo

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/algorithms/BinarySearch.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/algorithms/BinarySearchDemo.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/core/algorithms/BinarySearchTest.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/core/algorithms/BinarySearchDemoTest.java`

**Interfaces:**
- Consumes: `Tracer`, `ArrayView`, `Trace`, `TraceResult` (Tasks 2-3); `Demo`, `DemoParams`, `ParameterSpec`, `DemoInputException` (Tasks 4-5).
- Produces:
  - `BinarySearch.search(int[] array, int target, Tracer tracer)` returning the index or `-1`.
  - `BinarySearchDemo` Spring `@Component` with id `binary-search`, parameters `array` (INT_ARRAY, max 512, default `[2,5,8,12,20,33]`) and `target` (INT, -1000..1000, default 20).

The demo rejects an unsorted array with `DemoInputException("array", "array must be sorted ascending")`.

`displaySource()` is this exact text, and step line numbers are 1-based line numbers **in this string**:

```
1  public static int search(int[] a, int target) {
2      int low = 0;
3      int high = a.length - 1;
4      while (low <= high) {
5          int mid = (low + high) / 2;
6          if (a[mid] == target) {
7              return mid;
8          } else if (a[mid] < target) {
9              low = mid + 1;
10         } else {
11             high = mid - 1;
12         }
13     }
14     return -1;
15 }
```

(The numbers above are for the plan reader only. The constant in code holds the source lines without the numeric prefix.)

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/core/algorithms/BinarySearchTest.java`:

```java
package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.trace.Tracer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BinarySearchTest {

    @Test
    void findsAnExistingValue() {
        assertThat(BinarySearch.search(new int[]{2, 5, 8, 12, 20, 33}, 20, new Tracer())).isEqualTo(4);
    }

    @Test
    void findsTheFirstAndLastValue() {
        int[] array = {2, 5, 8, 12, 20, 33};
        assertThat(BinarySearch.search(array, 2, new Tracer())).isZero();
        assertThat(BinarySearch.search(array, 33, new Tracer())).isEqualTo(5);
    }

    @Test
    void returnsMinusOneWhenAbsent() {
        assertThat(BinarySearch.search(new int[]{2, 5, 8}, 7, new Tracer())).isEqualTo(-1);
    }

    @Test
    void handlesEmptyAndSingleElementArrays() {
        assertThat(BinarySearch.search(new int[]{}, 1, new Tracer())).isEqualTo(-1);
        assertThat(BinarySearch.search(new int[]{1}, 1, new Tracer())).isZero();
        assertThat(BinarySearch.search(new int[]{1}, 2, new Tracer())).isEqualTo(-1);
    }

    @Test
    void recordsOneStepPerComparison() {
        Tracer tracer = new Tracer();
        BinarySearch.search(new int[]{2, 5, 8, 12, 20, 33}, 20, tracer);

        assertThat(tracer.steps()).isNotEmpty();
        assertThat(tracer.steps().get(0).vars()).containsKeys("low", "high");
        assertThat(tracer.steps().get(tracer.steps().size() - 1).message()).contains("found");
    }
}
```

`backend/src/test/java/dev/klleriston/fundamentals/core/algorithms/BinarySearchDemoTest.java`:

```java
package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.ArrayView;
import dev.klleriston.fundamentals.trace.Trace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BinarySearchDemoTest {

    private final BinarySearchDemo demo = new BinarySearchDemo();

    private Trace run(Map<String, Object> raw) {
        return demo.run(DemoParams.of(raw, demo.parameters()));
    }

    @Test
    void exposesItsIdentity() {
        assertThat(demo.id()).isEqualTo("binary-search");
        assertThat(demo.category()).isEqualTo(Category.ALGORITHMS);
        assertThat(demo.displaySource()).contains("int mid = (low + high) / 2;");
        assertThat(demo.displaySource()).doesNotContain("tracer");
    }

    @Test
    void producesTraceWithArrayViewsAndResult() {
        Trace trace = run(Map.of("array", List.of(2, 5, 8, 12, 20, 33), "target", 20));

        assertThat(trace.demoId()).isEqualTo("binary-search");
        assertThat(trace.result().returnValue()).isEqualTo(4);
        assertThat(trace.result().measured()).isFalse();
        assertThat(trace.result().stepCount()).isEqualTo(trace.steps().size());
        assertThat(trace.steps()).allSatisfy(step -> {
            assertThat(step.view()).isInstanceOf(ArrayView.class);
            assertThat(step.line()).isBetween(1, 15);
        });
    }

    @Test
    void marksDiscardedRanges() {
        Trace trace = run(Map.of("array", List.of(2, 5, 8, 12, 20, 33), "target", 20));

        ArrayView lastView = (ArrayView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(lastView.items()).containsExactly(2, 5, 8, 12, 20, 33);
        assertThat(lastView.pointers()).containsKey("mid");
        assertThat(trace.steps()).anySatisfy(step ->
                assertThat(((ArrayView) step.view()).ranges()).isNotEmpty());
    }

    @Test
    void rejectsUnsortedArray() {
        assertThatThrownBy(() -> run(Map.of("array", List.of(5, 2, 8), "target", 8)))
                .isInstanceOf(DemoInputException.class)
                .hasMessageContaining("sorted");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest='BinarySearch*Test'`
Expected: FAIL — compilation error, `BinarySearch` does not exist.

- [ ] **Step 3: Write minimal implementation**

`BinarySearch.java`:

```java
package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.trace.ArrayView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BinarySearch {

    private BinarySearch() {
    }

    public static int search(int[] a, int target, Tracer tracer) {
        int low = 0;
        int high = a.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (a[mid] == target) {
                tracer.step(6, "a[" + mid + "] = " + a[mid] + " equals " + target + ", found at index " + mid,
                        vars(low, high, mid), view(a, low, high, mid));
                return mid;
            } else if (a[mid] < target) {
                tracer.step(8, "a[" + mid + "] = " + a[mid] + " < " + target + ", discard the left half",
                        vars(low, high, mid), view(a, low, high, mid));
                low = mid + 1;
            } else {
                tracer.step(10, "a[" + mid + "] = " + a[mid] + " > " + target + ", discard the right half",
                        vars(low, high, mid), view(a, low, high, mid));
                high = mid - 1;
            }
        }
        tracer.step(14, target + " is not in the array, return -1", vars(low, high, -1), view(a, low, high, -1));
        return -1;
    }

    private static Map<String, Object> vars(int low, int high, int mid) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("low", low);
        vars.put("high", high);
        if (mid >= 0) {
            vars.put("mid", mid);
        }
        return vars;
    }

    private static ArrayView view(int[] a, int low, int high, int mid) {
        Map<String, Integer> pointers = new LinkedHashMap<>();
        pointers.put("low", low);
        pointers.put("high", high);
        if (mid >= 0) {
            pointers.put("mid", mid);
        }

        List<ArrayView.Range> ranges = new java.util.ArrayList<>();
        if (low > 0) {
            ranges.add(new ArrayView.Range(0, low - 1, ArrayView.Range.DISCARDED));
        }
        if (high < a.length - 1) {
            ranges.add(new ArrayView.Range(high + 1, a.length - 1, ArrayView.Range.DISCARDED));
        }

        List<Integer> items = Arrays.stream(a).boxed().toList();
        return new ArrayView(items, pointers, ranges, List.of());
    }
}
```

`BinarySearchDemo.java`:

```java
package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import dev.klleriston.fundamentals.trace.Tracer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BinarySearchDemo implements Demo {

    private static final String SOURCE = """
            public static int search(int[] a, int target) {
                int low = 0;
                int high = a.length - 1;
                while (low <= high) {
                    int mid = (low + high) / 2;
                    if (a[mid] == target) {
                        return mid;
                    } else if (a[mid] < target) {
                        low = mid + 1;
                    } else {
                        high = mid - 1;
                    }
                }
                return -1;
            }""";

    @Override
    public String id() {
        return "binary-search";
    }

    @Override
    public String title() {
        return "Binary Search";
    }

    @Override
    public Category category() {
        return Category.ALGORITHMS;
    }

    @Override
    public String description() {
        return "Halve the search range on every comparison to find a value in a sorted array.";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("array", "Sorted array", 512, List.of(2, 5, 8, 12, 20, 33)),
                ParameterSpec.integer("target", "Target value", -1000, 1000, 20));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        int[] array = params.intArray("array");
        requireSorted(array);
        int target = params.integer("target");

        Tracer tracer = new Tracer();
        int result = BinarySearch.search(array, target, tracer);

        return new Trace(id(), title(), SOURCE, tracer.steps(),
                new TraceResult(result, tracer.steps().size(), false));
    }

    private static void requireSorted(int[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i - 1] > array[i]) {
                throw new DemoInputException("array", "array must be sorted ascending");
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test -Dtest='BinarySearch*Test'`
Expected: PASS — 9 tests across the two classes.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/dev/klleriston/fundamentals/core/algorithms backend/src/test/java/dev/klleriston/fundamentals/core/algorithms
git commit -m "feat: add binary search demo"
```

---

### Task 7: Bubble sort demo

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/algorithms/BubbleSort.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/core/algorithms/BubbleSortDemo.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/core/algorithms/BubbleSortTest.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/core/algorithms/BubbleSortDemoTest.java`

**Interfaces:**
- Consumes: the same types as Task 6.
- Produces:
  - `BubbleSort.sort(int[] array, Tracer tracer)` returning the sorted array.
  - `BubbleSortDemo` Spring `@Component` with id `bubble-sort`, one parameter `array` (INT_ARRAY, max 64, default `[5, 2, 9, 1, 7]`).

This demo exists to prove the `ARRAY` renderer generalizes: it uses `swapped` and the `SORTED` range state, which binary search never emits. The array cap is 64 here because bubble sort is quadratic in steps.

`displaySource()`:

```
1  public static void sort(int[] a) {
2      for (int i = 0; i < a.length - 1; i++) {
3          for (int j = 0; j < a.length - 1 - i; j++) {
4              if (a[j] > a[j + 1]) {
5                  int tmp = a[j];
6                  a[j] = a[j + 1];
7                  a[j + 1] = tmp;
8              }
9          }
10     }
11 }
```

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/core/algorithms/BubbleSortTest.java`:

```java
package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.trace.Tracer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BubbleSortTest {

    @Test
    void sortsAscending() {
        assertThat(BubbleSort.sort(new int[]{5, 2, 9, 1, 7}, new Tracer()))
                .containsExactly(1, 2, 5, 7, 9);
    }

    @Test
    void leavesSortedInputUntouched() {
        assertThat(BubbleSort.sort(new int[]{1, 2, 3}, new Tracer())).containsExactly(1, 2, 3);
    }

    @Test
    void handlesEmptyAndSingleElementArrays() {
        assertThat(BubbleSort.sort(new int[]{}, new Tracer())).isEmpty();
        assertThat(BubbleSort.sort(new int[]{4}, new Tracer())).containsExactly(4);
    }

    @Test
    void recordsAStepPerComparison() {
        Tracer tracer = new Tracer();
        BubbleSort.sort(new int[]{2, 1}, tracer);

        assertThat(tracer.steps()).isNotEmpty();
        assertThat(tracer.steps().get(0).vars()).containsKeys("i", "j");
    }
}
```

`backend/src/test/java/dev/klleriston/fundamentals/core/algorithms/BubbleSortDemoTest.java`:

```java
package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.ArrayView;
import dev.klleriston.fundamentals.trace.Trace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BubbleSortDemoTest {

    private final BubbleSortDemo demo = new BubbleSortDemo();

    @Test
    void exposesItsIdentity() {
        assertThat(demo.id()).isEqualTo("bubble-sort");
        assertThat(demo.category()).isEqualTo(Category.ALGORITHMS);
        assertThat(demo.displaySource()).doesNotContain("tracer");
    }

    @Test
    void producesSortedResultAndMarksSwapsAndSortedTail() {
        Trace trace = demo.run(DemoParams.of(Map.of("array", List.of(5, 2, 9, 1, 7)), demo.parameters()));

        assertThat(trace.result().returnValue()).isEqualTo(List.of(1, 2, 5, 7, 9));
        assertThat(trace.steps()).anySatisfy(step ->
                assertThat(((ArrayView) step.view()).swapped()).hasSize(2));
        assertThat(trace.steps()).anySatisfy(step ->
                assertThat(((ArrayView) step.view()).ranges())
                        .anyMatch(range -> range.state().equals(ArrayView.Range.SORTED)));
    }

    @Test
    void arrayParameterIsCappedAtSixtyFour() {
        assertThat(demo.parameters().get(0).maxLength()).isEqualTo(64);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest='BubbleSort*Test'`
Expected: FAIL — compilation error, `BubbleSort` does not exist.

- [ ] **Step 3: Write minimal implementation**

`BubbleSort.java`:

```java
package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.trace.ArrayView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BubbleSort {

    private BubbleSort() {
    }

    public static int[] sort(int[] a, Tracer tracer) {
        for (int i = 0; i < a.length - 1; i++) {
            for (int j = 0; j < a.length - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    int tmp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = tmp;
                    tracer.step(7, "a[" + j + "] > a[" + (j + 1) + "], swap them",
                            vars(i, j), view(a, i, j, List.of(j, j + 1)));
                } else {
                    tracer.step(4, "a[" + j + "] <= a[" + (j + 1) + "], keep the order",
                            vars(i, j), view(a, i, j, List.of()));
                }
            }
        }
        tracer.step(11, "The array is sorted", vars(Math.max(a.length - 1, 0), 0),
                view(a, a.length, 0, List.of()));
        return a;
    }

    private static Map<String, Object> vars(int i, int j) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("i", i);
        vars.put("j", j);
        return vars;
    }

    private static ArrayView view(int[] a, int i, int j, List<Integer> swapped) {
        Map<String, Integer> pointers = new LinkedHashMap<>();
        pointers.put("j", j);
        pointers.put("j+1", Math.min(j + 1, Math.max(a.length - 1, 0)));

        List<ArrayView.Range> ranges = new ArrayList<>();
        int sortedFrom = a.length - i;
        if (sortedFrom < a.length) {
            ranges.add(new ArrayView.Range(sortedFrom, a.length - 1, ArrayView.Range.SORTED));
        }

        List<Integer> items = Arrays.stream(a).boxed().toList();
        return new ArrayView(items, pointers, ranges, swapped);
    }
}
```

`BubbleSortDemo.java`:

```java
package dev.klleriston.fundamentals.core.algorithms;

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
public class BubbleSortDemo implements Demo {

    private static final String SOURCE = """
            public static void sort(int[] a) {
                for (int i = 0; i < a.length - 1; i++) {
                    for (int j = 0; j < a.length - 1 - i; j++) {
                        if (a[j] > a[j + 1]) {
                            int tmp = a[j];
                            a[j] = a[j + 1];
                            a[j + 1] = tmp;
                        }
                    }
                }
            }""";

    @Override
    public String id() {
        return "bubble-sort";
    }

    @Override
    public String title() {
        return "Bubble Sort";
    }

    @Override
    public Category category() {
        return Category.ALGORITHMS;
    }

    @Override
    public String description() {
        return "Repeatedly swap adjacent values out of order until the largest bubbles to the end.";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(ParameterSpec.intArray("array", "Array to sort", 64, List.of(5, 2, 9, 1, 7)));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        int[] array = params.intArray("array");

        Tracer tracer = new Tracer();
        int[] sorted = BubbleSort.sort(array, tracer);

        return new Trace(id(), title(), SOURCE, tracer.steps(),
                new TraceResult(Arrays.stream(sorted).boxed().toList(), tracer.steps().size(), false));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test -Dtest='BubbleSort*Test'`
Expected: PASS — 7 tests across the two classes.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/dev/klleriston/fundamentals/core/algorithms backend/src/test/java/dev/klleriston/fundamentals/core/algorithms
git commit -m "feat: add bubble sort demo"
```

---

### Task 8: REST API and error handling

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/api/DemoSummary.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/api/ApiError.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/api/DemoController.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/api/ApiExceptionHandler.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/api/JacksonConfig.java`
- Test: `backend/src/test/java/dev/klleriston/fundamentals/api/DemoControllerTest.java`

**Interfaces:**
- Consumes: `DemoRegistry`, `Demo`, `DemoParams`, `DemoInputException`, `UnknownDemoException` (Tasks 4-5); `Trace` (Task 2); `StepLimitExceededException` (Task 3).
- Produces:
  - `GET /api/demos` → `List<DemoSummary>` where `DemoSummary(String id, String title, Category category, String description, List<ParameterSpec> parameters, String sourceCode)`.
  - `POST /api/demos/{id}/trace` with a JSON object body of parameters → `Trace`.
  - `record ApiError(String error, String message, String field)`.

Jackson serializes `ViewPayload` implementations by their record components, but it does **not** pick up `kind()`: auto-detection only covers `getX()`/`isX()` accessors. Since the `trace` package carries no Jackson annotations by design, the mapping lives in the `api` layer as a mix-in registered through a `Module` bean (`JacksonConfig`). The controller test asserts `view.kind` is present, because the frontend's renderer lookup depends on it.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/api/DemoControllerTest.java`:

```java
package dev.klleriston.fundamentals.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsDemosWithParameterSchema() throws Exception {
        mockMvc.perform(get("/api/demos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("binary-search"))
                .andExpect(jsonPath("$[0].category").value("ALGORITHMS"))
                .andExpect(jsonPath("$[0].parameters[0].name").value("array"))
                .andExpect(jsonPath("$[0].parameters[0].type").value("INT_ARRAY"))
                .andExpect(jsonPath("$[0].sourceCode").isNotEmpty())
                .andExpect(jsonPath("$[1].id").value("bubble-sort"));
    }

    @Test
    void returnsTraceForValidParameters() throws Exception {
        mockMvc.perform(post("/api/demos/binary-search/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"array\":[2,5,8,12,20,33],\"target\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demoId").value("binary-search"))
                .andExpect(jsonPath("$.result.returnValue").value(4))
                .andExpect(jsonPath("$.result.measured").value(false))
                .andExpect(jsonPath("$.steps[0].view.kind").value("ARRAY"))
                .andExpect(jsonPath("$.steps[0].line").isNumber())
                .andExpect(jsonPath("$.steps[0].vars.low").value(0));
    }

    @Test
    void usesDefaultsWhenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/demos/bubble-sort/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps").isNotEmpty());
    }

    @Test
    void rejectsUnsortedArrayWithFieldAndMessage() throws Exception {
        mockMvc.perform(post("/api/demos/binary-search/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"array\":[5,2,8],\"target\":8}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("array"))
                .andExpect(jsonPath("$.message").value("array must be sorted ascending"));
    }

    @Test
    void rejectsOversizedArray() throws Exception {
        StringBuilder body = new StringBuilder("{\"array\":[");
        for (int i = 0; i < 70; i++) {
            body.append(i).append(i < 69 ? "," : "");
        }
        body.append("]}");

        mockMvc.perform(post("/api/demos/bubble-sort/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("array"));
    }

    @Test
    void returnsNotFoundForUnknownDemo() throws Exception {
        mockMvc.perform(post("/api/demos/nope/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("UNKNOWN_DEMO"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest=DemoControllerTest`
Expected: FAIL — 404 on every route; no controller is mapped.

- [ ] **Step 3: Write minimal implementation**

`DemoSummary.java`:

```java
package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.ParameterSpec;

import java.util.List;

public record DemoSummary(
        String id,
        String title,
        Category category,
        String description,
        List<ParameterSpec> parameters,
        String sourceCode) {

    public static DemoSummary from(Demo demo) {
        return new DemoSummary(demo.id(), demo.title(), demo.category(), demo.description(),
                demo.parameters(), demo.displaySource());
    }
}
```

`ApiError.java`:

```java
package dev.klleriston.fundamentals.api;

public record ApiError(String error, String message, String field) {
}
```

`DemoController.java`:

```java
package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.DemoRegistry;
import dev.klleriston.fundamentals.trace.Trace;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demos")
public class DemoController {

    private final DemoRegistry registry;

    public DemoController(DemoRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<DemoSummary> list() {
        return registry.all().stream().map(DemoSummary::from).toList();
    }

    @PostMapping("/{id}/trace")
    public Trace trace(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Demo demo = registry.require(id);
        DemoParams params = DemoParams.of(body == null ? Map.of() : body, demo.parameters());
        return demo.run(params);
    }
}
```

`ApiExceptionHandler.java`:

```java
package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.UnknownDemoException;
import dev.klleriston.fundamentals.trace.StepLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DemoInputException.class)
    public ResponseEntity<ApiError> handleInvalidInput(DemoInputException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_INPUT", exception.getMessage(), exception.field()));
    }

    @ExceptionHandler(StepLimitExceededException.class)
    public ResponseEntity<ApiError> handleStepLimit(StepLimitExceededException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("STEP_LIMIT_EXCEEDED", exception.getMessage(), null));
    }

    @ExceptionHandler(UnknownDemoException.class)
    public ResponseEntity<ApiError> handleUnknownDemo(UnknownDemoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("UNKNOWN_DEMO", exception.getMessage(), null));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test -Dtest=DemoControllerTest`
Expected: PASS — 6 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/dev/klleriston/fundamentals/api backend/src/test/java/dev/klleriston/fundamentals/api
git commit -m "feat: expose demo catalog and trace endpoints"
```

---

### Task 8b: Execution timeout

**Files:**
- Create: `backend/src/main/java/dev/klleriston/fundamentals/api/DemoTimeoutException.java`
- Create: `backend/src/main/java/dev/klleriston/fundamentals/api/DemoExecutor.java`
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/api/DemoController.java` (route the call through `DemoExecutor`)
- Modify: `backend/src/main/java/dev/klleriston/fundamentals/api/ApiExceptionHandler.java` (add one handler)
- Test: `backend/src/test/java/dev/klleriston/fundamentals/api/DemoExecutorTest.java`

**Interfaces:**
- Consumes: `Demo`, `DemoParams` (Tasks 4-5); `Trace` (Task 2).
- Produces: `DemoExecutor.run(Demo demo, DemoParams params)` returning `Trace`, aborting after 5 seconds with `DemoTimeoutException`. Exceptions thrown by the demo itself (`DemoInputException`, `StepLimitExceededException`) are unwrapped and rethrown unchanged so the existing handlers still map them.

The Global Constraints promise a 5-second in-process timeout. The step cap alone does not bound wall-clock time, so this task adds the guard.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/api/DemoExecutorTest.java`:

```java
package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoExecutorTest {

    private static Demo demoRunning(Supplier<Trace> body) {
        return new Demo() {
            @Override public String id() { return "stub"; }
            @Override public String title() { return "Stub"; }
            @Override public Category category() { return Category.ALGORITHMS; }
            @Override public String description() { return "stub"; }
            @Override public List<ParameterSpec> parameters() { return List.of(); }
            @Override public String displaySource() { return "code"; }
            @Override public Trace run(DemoParams params) { return body.get(); }
        };
    }

    private static Trace emptyTrace() {
        return new Trace("stub", "Stub", "code", List.of(), new TraceResult(null, 0, false));
    }

    @Test
    void returnsTheTraceWhenTheDemoFinishesInTime() {
        DemoExecutor executor = new DemoExecutor(Duration.ofSeconds(5));

        Trace trace = executor.run(demoRunning(DemoExecutorTest::emptyTrace), DemoParams.of(java.util.Map.of(), List.of()));

        assertThat(trace.demoId()).isEqualTo("stub");
    }

    @Test
    void abortsADemoThatRunsTooLong() {
        DemoExecutor executor = new DemoExecutor(Duration.ofMillis(100));

        Demo slow = demoRunning(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return emptyTrace();
        });

        assertThatThrownBy(() -> executor.run(slow, DemoParams.of(java.util.Map.of(), List.of())))
                .isInstanceOf(DemoTimeoutException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void rethrowsDemoExceptionsUnchanged() {
        DemoExecutor executor = new DemoExecutor(Duration.ofSeconds(5));

        Demo failing = demoRunning(() -> {
            throw new DemoInputException("array", "array must be sorted ascending");
        });

        assertThatThrownBy(() -> executor.run(failing, DemoParams.of(java.util.Map.of(), List.of())))
                .isInstanceOf(DemoInputException.class)
                .hasMessage("array must be sorted ascending");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest=DemoExecutorTest`
Expected: FAIL — compilation error, `DemoExecutor` does not exist.

- [ ] **Step 3: Write minimal implementation**

`DemoTimeoutException.java`:

```java
package dev.klleriston.fundamentals.api;

import java.time.Duration;

public class DemoTimeoutException extends RuntimeException {

    public DemoTimeoutException(Duration limit) {
        super("The demo took too long to run (limit: " + limit.toSeconds() + "s). Try a smaller input.");
    }
}
```

`DemoExecutor.java`:

```java
package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.Trace;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class DemoExecutor {

    private final Duration limit;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DemoExecutor() {
        this(Duration.ofSeconds(5));
    }

    public DemoExecutor(Duration limit) {
        this.limit = limit;
    }

    public Trace run(Demo demo, DemoParams params) {
        Future<Trace> future = executor.submit(() -> demo.run(params));
        try {
            return future.get(limit.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            future.cancel(true);
            throw new DemoTimeoutException(limit);
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(cause);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
```

Note for the implementer: Spring picks the no-arg constructor because it is the only public one without arguments — annotate it with `@Autowired` only if Spring reports an ambiguity.

Modify `DemoController.java` — replace the `trace` method body so it runs through the executor:

```java
    private final DemoRegistry registry;
    private final DemoExecutor executor;

    public DemoController(DemoRegistry registry, DemoExecutor executor) {
        this.registry = registry;
        this.executor = executor;
    }

    @PostMapping("/{id}/trace")
    public Trace trace(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Demo demo = registry.require(id);
        DemoParams params = DemoParams.of(body == null ? Map.of() : body, demo.parameters());
        return executor.run(demo, params);
    }
```

Add to `ApiExceptionHandler.java`:

```java
    @ExceptionHandler(DemoTimeoutException.class)
    public ResponseEntity<ApiError> handleTimeout(DemoTimeoutException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("EXECUTION_TIMEOUT", exception.getMessage(), null));
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn -B test -Dtest='DemoExecutorTest,DemoControllerTest'`
Expected: PASS — 3 executor tests plus the 6 controller tests from Task 8, still green.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/dev/klleriston/fundamentals/api backend/src/test/java/dev/klleriston/fundamentals/api
git commit -m "feat: abort demo execution after five seconds"
```

---

### Task 9: Golden traces exported as frontend fixtures

**Files:**
- Create: `backend/src/test/java/dev/klleriston/fundamentals/api/GoldenTraceTest.java`
- Create: `frontend/src/fixtures/binary-search.json` (generated in Step 3)
- Create: `frontend/src/fixtures/bubble-sort.json` (generated in Step 3)
- Modify: `backend/pom.xml` (no change needed if `spring-boot-starter-test` is present — it brings JSONassert)

**Interfaces:**
- Consumes: `DemoRegistry` and the two demos.
- Produces: committed JSON fixtures at `frontend/src/fixtures/<demoId>.json`, byte-for-byte what the API returns for a fixed input. Frontend tests (Tasks 12-16) load these. Regenerate with `mvn -B test -Dtest=GoldenTraceTest -Dgolden.update=true`.

This is the contract lock: if a backend change alters the trace JSON, this test fails until the fixture is regenerated, and the regenerated fixture makes the frontend tests fail if the shape actually broke.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/dev/klleriston/fundamentals/api/GoldenTraceTest.java`:

```java
package dev.klleriston.fundamentals.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.DemoRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GoldenTraceTest {

    private static final Path FIXTURES = Path.of("..", "frontend", "src", "fixtures");

    @Autowired
    private DemoRegistry registry;

    /** Spring's mapper, so the fixture matches what the API actually returns (including non-null inclusion). */
    @Autowired
    private ObjectMapper springMapper;

    private ObjectMapper mapper() {
        return springMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    void binarySearchTraceMatchesFixture() throws Exception {
        assertGolden("binary-search", Map.of("array", List.of(2, 5, 8, 12, 20, 33), "target", 20));
    }

    @Test
    void bubbleSortTraceMatchesFixture() throws Exception {
        assertGolden("bubble-sort", Map.of("array", List.of(5, 2, 9, 1, 7)));
    }

    private void assertGolden(String demoId, Map<String, Object> params) throws Exception {
        Demo demo = registry.require(demoId);
        String actual = mapper().writeValueAsString(demo.run(DemoParams.of(params, demo.parameters())));

        Path fixture = FIXTURES.resolve(demoId + ".json");
        if (Boolean.getBoolean("golden.update") || !Files.exists(fixture)) {
            Files.createDirectories(FIXTURES);
            Files.writeString(fixture, actual + System.lineSeparator());
        }

        assertThat(Files.readString(fixture).trim())
                .as("Fixture %s is stale. Regenerate with -Dgolden.update=true", fixture)
                .isEqualTo(actual.trim());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -B test -Dtest=GoldenTraceTest`
Expected: On a clean checkout the fixtures do not exist, so the test writes them and passes. To see it fail as intended, first create a deliberately wrong fixture:

```bash
mkdir -p frontend/src/fixtures && echo '{"demoId":"wrong"}' > frontend/src/fixtures/binary-search.json
cd backend && mvn -B test -Dtest=GoldenTraceTest
```

Expected: FAIL with "Fixture ../frontend/src/fixtures/binary-search.json is stale".

- [ ] **Step 3: Generate the real fixtures**

```bash
cd backend && mvn -B test -Dtest=GoldenTraceTest -Dgolden.update=true
```

Then open `frontend/src/fixtures/binary-search.json` and confirm by eye that it contains `demoId`, `sourceCode`, a `steps` array whose entries carry `index`, `line`, `message`, `vars`, and `view.kind = "ARRAY"`, and a `result` object.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -B test -Dtest=GoldenTraceTest`
Expected: PASS — 2 tests, fixtures unchanged.

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/dev/klleriston/fundamentals/api/GoldenTraceTest.java frontend/src/fixtures
git commit -m "test: lock trace JSON contract with golden fixtures"
```

---

### Task 10: Frontend scaffold and typed API client

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/tsconfig.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/types.ts`
- Create: `frontend/src/api.ts`
- Test: `frontend/src/api.test.ts`
- Create: `frontend/src/test-setup.ts`

**Interfaces:**
- Consumes: the JSON shape locked in Task 9.
- Produces:
  - Types `ViewPayload`, `ArrayViewPayload`, `TraceStep`, `TraceResult`, `Trace`, `ParameterSpec`, `DemoSummary`, `ApiError`.
  - `fetchDemos(): Promise<DemoSummary[]>` and `runTrace(id: string, params: Record<string, unknown>): Promise<Trace>`, both throwing `ApiRequestError` (carrying `message` and `field`) on a non-2xx response.
  - `npm test` (Vitest) and `npm run dev` (Vite on 5173, proxying `/api` to 8080).

- [ ] **Step 1: Write the failing test**

`frontend/src/api.test.ts`:

```ts
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { ApiRequestError, fetchDemos, runTrace } from './api';
import binarySearchTrace from './fixtures/binary-search.json';

const server = setupServer(
  http.get('/api/demos', () =>
    HttpResponse.json([
      {
        id: 'binary-search',
        title: 'Binary Search',
        category: 'ALGORITHMS',
        description: 'desc',
        parameters: [
          { name: 'target', type: 'INT', label: 'Target', required: true, min: -1000, max: 1000, defaultValue: 20 },
        ],
        sourceCode: 'code',
      },
    ]),
  ),
  http.post('/api/demos/binary-search/trace', () => HttpResponse.json(binarySearchTrace)),
  http.post('/api/demos/bubble-sort/trace', () =>
    HttpResponse.json({ error: 'INVALID_INPUT', message: 'array must be sorted ascending', field: 'array' }, { status: 400 }),
  ),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('api', () => {
  it('lists demos', async () => {
    const demos = await fetchDemos();
    expect(demos).toHaveLength(1);
    expect(demos[0].parameters[0].type).toBe('INT');
  });

  it('runs a trace', async () => {
    const trace = await runTrace('binary-search', { target: 20 });
    expect(trace.demoId).toBe('binary-search');
    expect(trace.steps[0].view.kind).toBe('ARRAY');
  });

  it('throws ApiRequestError carrying message and field', async () => {
    await expect(runTrace('bubble-sort', {})).rejects.toMatchObject({
      name: 'ApiRequestError',
      message: 'array must be sorted ascending',
      field: 'array',
    });
    await expect(runTrace('bubble-sort', {})).rejects.toBeInstanceOf(ApiRequestError);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test`
Expected: FAIL — there is no `package.json` yet, so npm cannot run.

- [ ] **Step 3: Write minimal implementation**

`frontend/package.json`:

```json
{
  "name": "fundamentals-frontend",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "test": "vitest run"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.26.2"
  },
  "devDependencies": {
    "@testing-library/jest-dom": "^6.5.0",
    "@testing-library/react": "^16.0.1",
    "@testing-library/user-event": "^14.5.2",
    "@types/react": "^18.3.11",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.2",
    "jsdom": "^25.0.1",
    "msw": "^2.4.9",
    "typescript": "^5.6.3",
    "vite": "^5.4.9",
    "vitest": "^2.1.3"
  }
}
```

`frontend/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noEmit": true,
    "resolveJsonModule": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "types": ["vitest/globals", "@testing-library/jest-dom"]
  },
  "include": ["src"]
}
```

`frontend/vite.config.ts`:

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts'],
  },
});
```

`frontend/src/test-setup.ts`:

```ts
import '@testing-library/jest-dom/vitest';
```

`frontend/index.html`:

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Java and Algorithms Fundamentals</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

`frontend/src/types.ts`:

```ts
export type ArrayRangeState = 'DISCARDED' | 'SORTED' | 'ACTIVE';

export interface ArrayViewPayload {
  kind: 'ARRAY';
  items: number[];
  pointers: Record<string, number>;
  ranges: { from: number; to: number; state: ArrayRangeState }[];
  swapped: number[];
}

export interface UnknownViewPayload {
  kind: string;
  [key: string]: unknown;
}

export type ViewPayload = ArrayViewPayload | UnknownViewPayload;

export interface TraceStep {
  index: number;
  line: number;
  message: string;
  vars: Record<string, unknown>;
  view: ViewPayload;
}

export interface TraceResult {
  returnValue: unknown;
  stepCount: number;
  measured: boolean;
}

export interface Trace {
  demoId: string;
  title: string;
  sourceCode: string;
  steps: TraceStep[];
  result: TraceResult;
}

export type ParameterType = 'INT' | 'LONG' | 'INT_ARRAY' | 'STRING_ARRAY' | 'ENUM';

export interface ParameterSpec {
  name: string;
  type: ParameterType;
  label: string;
  required: boolean;
  min?: number;
  max?: number;
  maxLength?: number;
  options?: string[];
  defaultValue?: unknown;
}

export interface DemoSummary {
  id: string;
  title: string;
  category: 'ALGORITHMS' | 'DATA_STRUCTURES' | 'JVM' | 'OOP';
  description: string;
  parameters: ParameterSpec[];
  sourceCode: string;
}
```

`frontend/src/api.ts`:

```ts
import type { DemoSummary, Trace } from './types';

export class ApiRequestError extends Error {
  readonly field?: string;

  constructor(message: string, field?: string) {
    super(message);
    this.name = 'ApiRequestError';
    this.field = field;
  }
}

async function parseOrThrow<T>(response: Response): Promise<T> {
  if (response.ok) {
    return (await response.json()) as T;
  }
  let message = `Request failed with status ${response.status}`;
  let field: string | undefined;
  try {
    const body = await response.json();
    if (typeof body?.message === 'string') message = body.message;
    if (typeof body?.field === 'string') field = body.field;
  } catch {
    // response had no JSON body; keep the status message
  }
  throw new ApiRequestError(message, field);
}

export async function fetchDemos(): Promise<DemoSummary[]> {
  return parseOrThrow<DemoSummary[]>(await fetch('/api/demos'));
}

export async function runTrace(id: string, params: Record<string, unknown>): Promise<Trace> {
  const response = await fetch(`/api/demos/${id}/trace`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  });
  return parseOrThrow<Trace>(response);
}
```

`frontend/src/main.tsx` (placeholder until Task 15 adds routing):

```tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <p>Loading…</p>
  </StrictMode>,
);
```

Then install: `cd frontend && npm install`.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test`
Expected: PASS — 3 tests in `api.test.ts`.

- [ ] **Step 5: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/tsconfig.json frontend/vite.config.ts frontend/index.html frontend/src
git commit -m "feat: add frontend scaffold and typed API client"
```

---

### Task 11: useTracePlayer hook

**Files:**
- Create: `frontend/src/hooks/useTracePlayer.ts`
- Test: `frontend/src/hooks/useTracePlayer.test.ts`

**Interfaces:**
- Consumes: nothing but `stepCount`.
- Produces: `useTracePlayer(stepCount: number, speedMs?: number)` returning `{ index, playing, play, pause, toggle, next, prev, first, last, seek, setSpeed, speedMs }`. Default speed 600 ms. Playing stops automatically at the last step. `index` clamps to `[0, stepCount - 1]`, and to `0` when `stepCount` is 0.

- [ ] **Step 1: Write the failing test**

`frontend/src/hooks/useTracePlayer.test.ts`:

```ts
import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useTracePlayer } from './useTracePlayer';

describe('useTracePlayer', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('starts at the first step', () => {
    const { result } = renderHook(() => useTracePlayer(5));
    expect(result.current.index).toBe(0);
    expect(result.current.playing).toBe(false);
  });

  it('steps forward and back, clamping at both ends', () => {
    const { result } = renderHook(() => useTracePlayer(3));

    act(() => result.current.prev());
    expect(result.current.index).toBe(0);

    act(() => result.current.next());
    act(() => result.current.next());
    act(() => result.current.next());
    expect(result.current.index).toBe(2);

    act(() => result.current.prev());
    expect(result.current.index).toBe(1);
  });

  it('jumps to first and last', () => {
    const { result } = renderHook(() => useTracePlayer(4));
    act(() => result.current.last());
    expect(result.current.index).toBe(3);
    act(() => result.current.first());
    expect(result.current.index).toBe(0);
  });

  it('seeks to an arbitrary step, clamped', () => {
    const { result } = renderHook(() => useTracePlayer(4));
    act(() => result.current.seek(2));
    expect(result.current.index).toBe(2);
    act(() => result.current.seek(99));
    expect(result.current.index).toBe(3);
  });

  it('advances while playing and stops at the end', () => {
    const { result } = renderHook(() => useTracePlayer(3, 100));

    act(() => result.current.play());
    expect(result.current.playing).toBe(true);

    act(() => vi.advanceTimersByTime(100));
    expect(result.current.index).toBe(1);

    act(() => vi.advanceTimersByTime(100));
    expect(result.current.index).toBe(2);
    expect(result.current.playing).toBe(false);
  });

  it('pauses', () => {
    const { result } = renderHook(() => useTracePlayer(5, 100));
    act(() => result.current.play());
    act(() => result.current.pause());
    act(() => vi.advanceTimersByTime(500));
    expect(result.current.index).toBe(0);
  });

  it('resets to zero when the trace changes length', () => {
    const { result, rerender } = renderHook(({ count }) => useTracePlayer(count), {
      initialProps: { count: 5 },
    });
    act(() => result.current.last());
    expect(result.current.index).toBe(4);

    rerender({ count: 2 });
    expect(result.current.index).toBe(0);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- useTracePlayer`
Expected: FAIL — cannot resolve `./useTracePlayer`.

- [ ] **Step 3: Write minimal implementation**

`frontend/src/hooks/useTracePlayer.ts`:

```ts
import { useCallback, useEffect, useRef, useState } from 'react';

export const DEFAULT_SPEED_MS = 600;

export function useTracePlayer(stepCount: number, initialSpeedMs: number = DEFAULT_SPEED_MS) {
  const [index, setIndex] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [speedMs, setSpeed] = useState(initialSpeedMs);
  const lastIndex = Math.max(stepCount - 1, 0);
  const lastIndexRef = useRef(lastIndex);
  lastIndexRef.current = lastIndex;

  useEffect(() => {
    setIndex(0);
    setPlaying(false);
  }, [stepCount]);

  useEffect(() => {
    if (!playing) return;
    const timer = setInterval(() => {
      setIndex((current) => {
        const next = current + 1;
        if (next >= lastIndexRef.current) {
          setPlaying(false);
          return lastIndexRef.current;
        }
        return next;
      });
    }, speedMs);
    return () => clearInterval(timer);
  }, [playing, speedMs]);

  const clamp = useCallback((value: number) => Math.min(Math.max(value, 0), lastIndex), [lastIndex]);

  return {
    index,
    playing,
    speedMs,
    setSpeed,
    play: useCallback(() => setPlaying(true), []),
    pause: useCallback(() => setPlaying(false), []),
    toggle: useCallback(() => setPlaying((value) => !value), []),
    next: useCallback(() => setIndex((current) => clamp(current + 1)), [clamp]),
    prev: useCallback(() => setIndex((current) => clamp(current - 1)), [clamp]),
    first: useCallback(() => setIndex(0), []),
    last: useCallback(() => setIndex(lastIndex), [lastIndex]),
    seek: useCallback((value: number) => setIndex(clamp(value)), [clamp]),
  };
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- useTracePlayer`
Expected: PASS — 7 tests.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/hooks
git commit -m "feat: add trace player hook"
```

---

### Task 12: CodePanel component

**Files:**
- Create: `frontend/src/components/CodePanel.tsx`
- Create: `frontend/src/components/CodePanel.css`
- Test: `frontend/src/components/CodePanel.test.tsx`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `<CodePanel source={string} activeLine={number} />`. Renders one row per source line with a 1-based gutter number; the active line's row carries `data-active="true"` and class `code-line--active`.

- [ ] **Step 1: Write the failing test**

`frontend/src/components/CodePanel.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { CodePanel } from './CodePanel';

const SOURCE = ['int low = 0;', 'int high = a.length - 1;', 'while (low <= high) {'].join('\n');

describe('CodePanel', () => {
  it('renders every line with a gutter number', () => {
    render(<CodePanel source={SOURCE} activeLine={1} />);
    expect(screen.getByText('int high = a.length - 1;')).toBeInTheDocument();
    expect(screen.getAllByTestId('gutter').map((node) => node.textContent)).toEqual(['1', '2', '3']);
  });

  it('marks only the active line', () => {
    render(<CodePanel source={SOURCE} activeLine={2} />);
    const active = screen.getAllByTestId('code-line').filter((node) => node.dataset.active === 'true');
    expect(active).toHaveLength(1);
    expect(active[0]).toHaveTextContent('int high = a.length - 1;');
  });

  it('marks no line when activeLine is out of range', () => {
    render(<CodePanel source={SOURCE} activeLine={99} />);
    expect(screen.getAllByTestId('code-line').filter((node) => node.dataset.active === 'true')).toHaveLength(0);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- CodePanel`
Expected: FAIL — cannot resolve `./CodePanel`.

- [ ] **Step 3: Write minimal implementation**

`frontend/src/components/CodePanel.tsx`:

```tsx
import './CodePanel.css';

interface CodePanelProps {
  source: string;
  activeLine: number;
}

export function CodePanel({ source, activeLine }: CodePanelProps) {
  const lines = source.split('\n');

  return (
    <pre className="code-panel">
      {lines.map((line, position) => {
        const lineNumber = position + 1;
        const active = lineNumber === activeLine;
        return (
          <div
            key={lineNumber}
            data-testid="code-line"
            data-active={active}
            className={active ? 'code-line code-line--active' : 'code-line'}
          >
            <span className="code-line__gutter" data-testid="gutter">
              {lineNumber}
            </span>
            <code className="code-line__text">{line}</code>
          </div>
        );
      })}
    </pre>
  );
}
```

`frontend/src/components/CodePanel.css`:

```css
.code-panel {
  margin: 0;
  padding: 12px 0;
  overflow: auto;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  height: 100%;
}

.code-line {
  display: flex;
  gap: 12px;
  padding: 0 12px;
  white-space: pre;
}

.code-line--active {
  background: #2f3b52;
  box-shadow: inset 3px 0 0 #4aa3ff;
}

.code-line__gutter {
  width: 2ch;
  text-align: right;
  color: #6b6b6b;
  user-select: none;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- CodePanel`
Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components
git commit -m "feat: add code panel with active line highlighting"
```

---

### Task 13: ArrayView renderer and renderer registry

**Files:**
- Create: `frontend/src/components/views/ArrayView.tsx`
- Create: `frontend/src/components/views/ArrayView.css`
- Create: `frontend/src/components/views/FallbackView.tsx`
- Create: `frontend/src/components/views/registry.tsx`
- Test: `frontend/src/components/views/ArrayView.test.tsx`
- Test: `frontend/src/components/views/registry.test.tsx`

**Interfaces:**
- Consumes: `ArrayViewPayload`, `ViewPayload` (Task 10).
- Produces:
  - `<ArrayView view={ArrayViewPayload} />` — one cell per item with `data-testid="cell"`, `data-state` of `"default" | "DISCARDED" | "SORTED" | "ACTIVE"`, `data-swapped`, and a pointer label row listing `name` → index.
  - `<FallbackView view={ViewPayload} />` — renders the kind name and a note.
  - `viewFor(kind: string): ComponentType<{ view: any }>` — returns the registered renderer or `FallbackView`. **Phase 2 and later register new kinds only by adding an entry to the map in this file.**

- [ ] **Step 1: Write the failing test**

`frontend/src/components/views/ArrayView.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ArrayView } from './ArrayView';
import type { ArrayViewPayload } from '../../types';

const view: ArrayViewPayload = {
  kind: 'ARRAY',
  items: [2, 5, 8, 12],
  pointers: { low: 2, mid: 1, high: 3 },
  ranges: [{ from: 0, to: 1, state: 'DISCARDED' }],
  swapped: [2, 3],
};

describe('ArrayView', () => {
  it('renders one cell per item', () => {
    render(<ArrayView view={view} />);
    expect(screen.getAllByTestId('cell').map((node) => node.textContent)).toEqual(['2', '5', '8', '12']);
  });

  it('marks cells inside a range with its state', () => {
    render(<ArrayView view={view} />);
    expect(screen.getAllByTestId('cell').map((node) => node.dataset.state)).toEqual([
      'DISCARDED',
      'DISCARDED',
      'default',
      'default',
    ]);
  });

  it('marks swapped cells', () => {
    render(<ArrayView view={view} />);
    expect(screen.getAllByTestId('cell').map((node) => node.dataset.swapped)).toEqual([
      'false',
      'false',
      'true',
      'true',
    ]);
  });

  it('renders a pointer label for each pointer', () => {
    render(<ArrayView view={view} />);
    expect(screen.getByText('low')).toBeInTheDocument();
    expect(screen.getByText('mid')).toBeInTheDocument();
    expect(screen.getByText('high')).toBeInTheDocument();
  });

  it('renders an empty array without crashing', () => {
    render(<ArrayView view={{ kind: 'ARRAY', items: [], pointers: {}, ranges: [], swapped: [] }} />);
    expect(screen.queryAllByTestId('cell')).toHaveLength(0);
  });
});
```

`frontend/src/components/views/registry.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { viewFor } from './registry';
import { ArrayView } from './ArrayView';

describe('viewFor', () => {
  it('returns the ARRAY renderer', () => {
    expect(viewFor('ARRAY')).toBe(ArrayView);
  });

  it('falls back for an unknown kind without crashing', () => {
    const Renderer = viewFor('TREE');
    render(<Renderer view={{ kind: 'TREE', nodes: [] }} />);
    expect(screen.getByText(/TREE/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- views`
Expected: FAIL — cannot resolve `./ArrayView` and `./registry`.

- [ ] **Step 3: Write minimal implementation**

`frontend/src/components/views/ArrayView.tsx`:

```tsx
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
```

`frontend/src/components/views/ArrayView.css`:

```css
.array-view {
  display: flex;
  justify-content: center;
  padding: 24px 12px;
  overflow-x: auto;
}

.array-view__cells {
  display: flex;
  gap: 6px;
}

.array-view__column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.array-view__cell {
  min-width: 44px;
  padding: 10px 8px;
  border: 1px solid #3c3c3c;
  border-radius: 6px;
  background: #252526;
  color: #e6e6e6;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  text-align: center;
  transition: background 120ms ease;
}

.array-view__cell[data-state='DISCARDED'] {
  opacity: 0.35;
}

.array-view__cell[data-state='SORTED'] {
  border-color: #3f8f5f;
  background: #22352a;
}

.array-view__cell[data-state='ACTIVE'] {
  border-color: #4aa3ff;
}

.array-view__cell[data-swapped='true'] {
  background: #4a3a1f;
  border-color: #d1a34a;
}

.array-view__pointer {
  display: inline-block;
  margin: 0 2px;
  color: #4aa3ff;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.array-view__index {
  color: #6b6b6b;
  font-size: 11px;
}
```

`frontend/src/components/views/FallbackView.tsx`:

```tsx
import type { ViewPayload } from '../../types';

export function FallbackView({ view }: { view: ViewPayload }) {
  return (
    <div className="fallback-view" style={{ padding: 24, color: '#a0a0a0' }}>
      <p>
        No renderer for view kind <strong>{view.kind}</strong> yet. The step line, message, and variables are still
        shown below.
      </p>
    </div>
  );
}
```

`frontend/src/components/views/registry.tsx`:

```tsx
import type { ComponentType } from 'react';
import type { ViewPayload } from '../../types';
import { ArrayView } from './ArrayView';
import { FallbackView } from './FallbackView';

// Later phases register new kinds here and nowhere else.
const renderers: Record<string, ComponentType<{ view: any }>> = {
  ARRAY: ArrayView,
};

export function viewFor(kind: string): ComponentType<{ view: ViewPayload }> {
  return renderers[kind] ?? FallbackView;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- views`
Expected: PASS — 7 tests.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/views
git commit -m "feat: add array renderer and view registry"
```

---

### Task 14: Player controls, variable table, and parameter form

**Files:**
- Create: `frontend/src/components/PlayerControls.tsx`
- Create: `frontend/src/components/VariableTable.tsx`
- Create: `frontend/src/components/ParameterForm.tsx`
- Create: `frontend/src/components/panels.css`
- Test: `frontend/src/components/PlayerControls.test.tsx`
- Test: `frontend/src/components/ParameterForm.test.tsx`

**Interfaces:**
- Consumes: `ParameterSpec` (Task 10).
- Produces:
  - `<PlayerControls index stepCount playing onFirst onPrev onToggle onNext onLast onSeek speedMs onSpeedChange />` — buttons labelled by `aria-label`: `first step`, `previous step`, `play`/`pause`, `next step`, `last step`; a `role="slider"` range input labelled `scrub`; a text `step {index+1} / {stepCount}`.
  - `<VariableTable vars={Record<string, unknown>} />` — a row per variable with `data-testid="var-row"`.
  - `<ParameterForm specs values onChange onSubmit error />` — one input per spec, `INT_ARRAY` edited as a comma-separated text field, `ENUM` as a `<select>`; submit button labelled `Run`; parses the array text into numbers before calling `onSubmit`; shows `error` text when present.

- [ ] **Step 1: Write the failing test**

`frontend/src/components/PlayerControls.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PlayerControls } from './PlayerControls';

function setup(overrides: Partial<React.ComponentProps<typeof PlayerControls>> = {}) {
  const props = {
    index: 1,
    stepCount: 5,
    playing: false,
    speedMs: 600,
    onFirst: vi.fn(),
    onPrev: vi.fn(),
    onToggle: vi.fn(),
    onNext: vi.fn(),
    onLast: vi.fn(),
    onSeek: vi.fn(),
    onSpeedChange: vi.fn(),
    ...overrides,
  };
  render(<PlayerControls {...props} />);
  return props;
}

describe('PlayerControls', () => {
  it('shows the current position', () => {
    setup();
    expect(screen.getByText('step 2 / 5')).toBeInTheDocument();
  });

  it('calls the matching handler for each button', async () => {
    const props = setup();
    const user = userEvent.setup();

    await user.click(screen.getByLabelText('first step'));
    await user.click(screen.getByLabelText('previous step'));
    await user.click(screen.getByLabelText('play'));
    await user.click(screen.getByLabelText('next step'));
    await user.click(screen.getByLabelText('last step'));

    expect(props.onFirst).toHaveBeenCalledOnce();
    expect(props.onPrev).toHaveBeenCalledOnce();
    expect(props.onToggle).toHaveBeenCalledOnce();
    expect(props.onNext).toHaveBeenCalledOnce();
    expect(props.onLast).toHaveBeenCalledOnce();
  });

  it('labels the toggle as pause while playing', () => {
    setup({ playing: true });
    expect(screen.getByLabelText('pause')).toBeInTheDocument();
  });
});
```

`frontend/src/components/ParameterForm.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ParameterForm } from './ParameterForm';
import type { ParameterSpec } from '../types';

const specs: ParameterSpec[] = [
  { name: 'array', type: 'INT_ARRAY', label: 'Sorted array', required: true, maxLength: 512 },
  { name: 'target', type: 'INT', label: 'Target value', required: true, min: -1000, max: 1000 },
];

describe('ParameterForm', () => {
  it('renders one labelled input per spec', () => {
    render(<ParameterForm specs={specs} values={{ array: '2,5,8', target: '5' }} onChange={vi.fn()} onSubmit={vi.fn()} />);
    expect(screen.getByLabelText('Sorted array')).toHaveValue('2,5,8');
    expect(screen.getByLabelText('Target value')).toHaveValue('5');
  });

  it('submits parsed values', async () => {
    const onSubmit = vi.fn();
    render(<ParameterForm specs={specs} values={{ array: '2, 5, 8', target: '5' }} onChange={vi.fn()} onSubmit={onSubmit} />);

    await userEvent.setup().click(screen.getByRole('button', { name: 'Run' }));

    expect(onSubmit).toHaveBeenCalledWith({ array: [2, 5, 8], target: 5 });
  });

  it('shows an error message when given one', () => {
    render(
      <ParameterForm specs={specs} values={{ array: '', target: '' }} onChange={vi.fn()} onSubmit={vi.fn()} error="array must be sorted ascending" />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('array must be sorted ascending');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- PlayerControls ParameterForm`
Expected: FAIL — cannot resolve `./PlayerControls` and `./ParameterForm`.

- [ ] **Step 3: Write minimal implementation**

`frontend/src/components/PlayerControls.tsx`:

```tsx
import './panels.css';

interface PlayerControlsProps {
  index: number;
  stepCount: number;
  playing: boolean;
  speedMs: number;
  onFirst: () => void;
  onPrev: () => void;
  onToggle: () => void;
  onNext: () => void;
  onLast: () => void;
  onSeek: (index: number) => void;
  onSpeedChange: (speedMs: number) => void;
}

export function PlayerControls({
  index,
  stepCount,
  playing,
  speedMs,
  onFirst,
  onPrev,
  onToggle,
  onNext,
  onLast,
  onSeek,
  onSpeedChange,
}: PlayerControlsProps) {
  return (
    <div className="player-controls">
      <button type="button" aria-label="first step" onClick={onFirst}>⏮</button>
      <button type="button" aria-label="previous step" onClick={onPrev}>◀</button>
      <button type="button" aria-label={playing ? 'pause' : 'play'} onClick={onToggle}>
        {playing ? '⏸' : '▶'}
      </button>
      <button type="button" aria-label="next step" onClick={onNext}>▶|</button>
      <button type="button" aria-label="last step" onClick={onLast}>⏭</button>

      <input
        type="range"
        aria-label="scrub"
        min={0}
        max={Math.max(stepCount - 1, 0)}
        value={index}
        onChange={(event) => onSeek(Number(event.target.value))}
      />

      <span className="player-controls__counter">{`step ${stepCount === 0 ? 0 : index + 1} / ${stepCount}`}</span>

      <select aria-label="speed" value={speedMs} onChange={(event) => onSpeedChange(Number(event.target.value))}>
        <option value={1200}>0.5x</option>
        <option value={600}>1x</option>
        <option value={300}>2x</option>
        <option value={120}>5x</option>
      </select>
    </div>
  );
}
```

`frontend/src/components/VariableTable.tsx`:

```tsx
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
```

`frontend/src/components/ParameterForm.tsx`:

```tsx
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
```

`frontend/src/components/panels.css`:

```css
.player-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-top: 1px solid #2c2c2c;
  background: #1b1b1b;
  color: #d4d4d4;
}

.player-controls button {
  min-width: 36px;
  padding: 6px 8px;
  border: 1px solid #3c3c3c;
  border-radius: 6px;
  background: #262626;
  color: #e6e6e6;
  cursor: pointer;
}

.player-controls input[type='range'] {
  flex: 1;
}

.player-controls__counter {
  min-width: 110px;
  font-variant-numeric: tabular-nums;
}

.variable-table {
  width: 100%;
  border-collapse: collapse;
  color: #d4d4d4;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
}

.variable-table th,
.variable-table td {
  padding: 4px 10px;
  border-bottom: 1px solid #2c2c2c;
  text-align: left;
}

.variable-table__empty {
  padding: 8px 10px;
  color: #8a8a8a;
}

.parameter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #2c2c2c;
  background: #1b1b1b;
  color: #d4d4d4;
}

.parameter-form__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
}

.parameter-form__field input,
.parameter-form__field select {
  padding: 6px 8px;
  border: 1px solid #3c3c3c;
  border-radius: 6px;
  background: #262626;
  color: #e6e6e6;
}

.parameter-form button[type='submit'] {
  padding: 7px 16px;
  border: none;
  border-radius: 6px;
  background: #2f6feb;
  color: #fff;
  cursor: pointer;
}

.parameter-form__error {
  flex-basis: 100%;
  margin: 0;
  color: #ff7b72;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- PlayerControls ParameterForm`
Expected: PASS — 6 tests.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components
git commit -m "feat: add player controls, variable table, and parameter form"
```

---

### Task 15: Demo screen and catalog wired to the API

**Files:**
- Create: `frontend/src/pages/DemoScreen.tsx`
- Create: `frontend/src/pages/DemoScreen.css`
- Create: `frontend/src/pages/Catalog.tsx`
- Create: `frontend/src/App.tsx`
- Modify: `frontend/src/main.tsx`
- Create: `frontend/src/index.css`
- Test: `frontend/src/pages/DemoScreen.test.tsx`
- Test: `frontend/src/pages/Catalog.test.tsx`

**Interfaces:**
- Consumes: `fetchDemos`, `runTrace`, `ApiRequestError` (Task 10); `useTracePlayer` (Task 11); `CodePanel` (Task 12); `viewFor` (Task 13); `PlayerControls`, `VariableTable`, `ParameterForm` (Task 14).
- Produces: routes `/` (catalog) and `/demo/:id` (demo screen), with parameters mirrored in the query string.

Behavior the tests pin down: on mount the screen loads the demo schema, runs the trace with default parameters, and shows step 1. Pressing Run re-requests with the form values and writes them to the query string. A rejected request shows the server message and leaves the previous trace on screen.

- [ ] **Step 1: Write the failing test**

`frontend/src/pages/DemoScreen.test.tsx`:

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { DemoScreen } from './DemoScreen';
import binarySearchTrace from '../fixtures/binary-search.json';

const demos = [
  {
    id: 'binary-search',
    title: 'Binary Search',
    category: 'ALGORITHMS',
    description: 'desc',
    parameters: [
      { name: 'array', type: 'INT_ARRAY', label: 'Sorted array', required: true, maxLength: 512, defaultValue: [2, 5, 8, 12, 20, 33] },
      { name: 'target', type: 'INT', label: 'Target value', required: true, min: -1000, max: 1000, defaultValue: 20 },
    ],
    sourceCode: binarySearchTrace.sourceCode,
  },
];

let failNext = false;

const server = setupServer(
  http.get('/api/demos', () => HttpResponse.json(demos)),
  http.post('/api/demos/binary-search/trace', () => {
    if (failNext) {
      failNext = false;
      return HttpResponse.json(
        { error: 'INVALID_INPUT', message: 'array must be sorted ascending', field: 'array' },
        { status: 400 },
      );
    }
    return HttpResponse.json(binarySearchTrace);
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  failNext = false;
});
afterAll(() => server.close());

function renderScreen() {
  return render(
    <MemoryRouter initialEntries={['/demo/binary-search']}>
      <Routes>
        <Route path="/demo/:id" element={<DemoScreen />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('DemoScreen', () => {
  it('loads the demo and shows the first step', async () => {
    renderScreen();

    expect(await screen.findByText('Binary Search')).toBeInTheDocument();
    expect(await screen.findByText(`step 1 / ${binarySearchTrace.steps.length}`)).toBeInTheDocument();
    expect(screen.getAllByTestId('cell')).toHaveLength(binarySearchTrace.steps[0].view.items.length);
  });

  it('advances the highlighted line when stepping forward', async () => {
    renderScreen();
    const user = userEvent.setup();

    await screen.findByText(`step 1 / ${binarySearchTrace.steps.length}`);
    await user.click(screen.getByLabelText('next step'));

    await waitFor(() => expect(screen.getByText('step 2 / ' + binarySearchTrace.steps.length)).toBeInTheDocument());
    const active = screen.getAllByTestId('code-line').filter((node) => node.dataset.active === 'true');
    expect(active).toHaveLength(1);
  });

  it('shows the step message and variables', async () => {
    renderScreen();
    await screen.findByText(binarySearchTrace.steps[0].message);
    expect(screen.getAllByTestId('var-row').length).toBeGreaterThan(0);
  });

  it('keeps the previous trace and shows the server message when a run fails', async () => {
    renderScreen();
    const user = userEvent.setup();
    await screen.findByText(`step 1 / ${binarySearchTrace.steps.length}`);

    failNext = true;
    await user.click(screen.getByRole('button', { name: 'Run' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('array must be sorted ascending');
    expect(screen.getByText(`step 1 / ${binarySearchTrace.steps.length}`)).toBeInTheDocument();
  });
});
```

`frontend/src/pages/Catalog.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { Catalog } from './Catalog';

const server = setupServer(
  http.get('/api/demos', () =>
    HttpResponse.json([
      { id: 'binary-search', title: 'Binary Search', category: 'ALGORITHMS', description: 'Halve the range', parameters: [], sourceCode: '' },
      { id: 'bubble-sort', title: 'Bubble Sort', category: 'ALGORITHMS', description: 'Swap neighbours', parameters: [], sourceCode: '' },
    ]),
  ),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Catalog', () => {
  it('lists demos as links grouped by category', async () => {
    render(
      <MemoryRouter>
        <Catalog />
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'ALGORITHMS' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Binary Search/ })).toHaveAttribute('href', '/demo/binary-search');
    expect(screen.getByRole('link', { name: /Bubble Sort/ })).toHaveAttribute('href', '/demo/bubble-sort');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- pages`
Expected: FAIL — cannot resolve `./DemoScreen` and `./Catalog`.

- [ ] **Step 3: Write minimal implementation**

`frontend/src/pages/DemoScreen.tsx`:

```tsx
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
```

`frontend/src/pages/DemoScreen.css`:

```css
.demo-screen {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #141414;
  color: #d4d4d4;
}

.demo-screen__header {
  padding: 12px 16px;
  border-bottom: 1px solid #2c2c2c;
}

.demo-screen__header h1 {
  margin: 0;
  font-size: 18px;
}

.demo-screen__header p {
  margin: 4px 0 0;
  color: #9a9a9a;
  font-size: 13px;
}

.demo-screen__split {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 1fr);
  flex: 1;
  min-height: 0;
}

.demo-screen__code {
  border-right: 1px solid #2c2c2c;
  min-height: 0;
  overflow: auto;
}

.demo-screen__visual {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.demo-screen__view {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.demo-screen__detail {
  border-top: 1px solid #2c2c2c;
  padding: 10px 12px;
  max-height: 40%;
  overflow: auto;
}

.demo-screen__message {
  margin: 0 0 8px;
  color: #cfcfcf;
  font-size: 13px;
}

.demo-screen__loading {
  padding: 24px;
  color: #9a9a9a;
}

@media (max-width: 900px) {
  .demo-screen__split {
    grid-template-columns: 1fr;
  }
}
```

`frontend/src/pages/Catalog.tsx`:

```tsx
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
```

`frontend/src/App.tsx`:

```tsx
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Catalog } from './pages/Catalog';
import { DemoScreen } from './pages/DemoScreen';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Catalog />} />
        <Route path="/demo/:id" element={<DemoScreen />} />
      </Routes>
    </BrowserRouter>
  );
}
```

`frontend/src/main.tsx` (replace the placeholder):

```tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
```

`frontend/src/index.css`:

```css
:root {
  color-scheme: dark;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: #141414;
  color: #d4d4d4;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

a {
  color: #6fb3ff;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test`
Expected: PASS — every suite green, including the 5 new page tests.

- [ ] **Step 5: Verify by hand, then commit**

Run the backend (`cd backend && mvn spring-boot:run`) and the frontend (`cd frontend && npm run dev`), open `http://localhost:5173/demo/binary-search`, press Run, and step through. Confirm the highlighted line moves with the array pointers.

```bash
git add frontend/src
git commit -m "feat: add demo screen and catalog"
```

---

### Task 16: CI and README

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `README.md`

**Interfaces:**
- Consumes: the Maven and npm builds from every earlier task.
- Produces: a CI run on push and pull request that fails if either build or any test fails.

- [ ] **Step 1: Write the workflow**

`.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [main, dev]
  pull_request:

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Build and test backend
        working-directory: backend
        run: mvn -B verify

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Install
        working-directory: frontend
        run: npm ci
      - name: Test
        working-directory: frontend
        run: npm test
      - name: Build
        working-directory: frontend
        run: npm run build
```

- [ ] **Step 2: Verify both jobs pass locally**

Run:

```bash
cd backend && mvn -B verify
cd ../frontend && npm ci && npm test && npm run build
```

Expected: both complete with no failures. The frontend `build` step also type-checks via `tsc -b`.

- [ ] **Step 3: Write the README**

`README.md`:

```markdown
# Java and Algorithms Fundamentals

An interactive web app that shows Java fundamentals running step by step:
algorithms, data structures, the JVM memory model, and garbage collection.

## Running locally

Requirements: JDK 21, Maven 3.9+, Node 20+.

```bash
# backend, on http://localhost:8080
cd backend && mvn spring-boot:run

# frontend, on http://localhost:5173 (proxies /api to the backend)
cd frontend && npm install && npm run dev
```

Open http://localhost:5173 and pick a demo.

## Tests

```bash
cd backend && mvn -B verify
cd frontend && npm test
```

The backend golden tests write `frontend/src/fixtures/*.json`, which the
frontend tests consume. If a backend change alters the trace JSON, regenerate
them with:

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
```

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/ci.yml README.md
git commit -m "ci: run backend and frontend builds on push and pull request"
```

---

## Definition of done for Phase 1

- `mvn -B verify` in `backend/` and `npm test && npm run build` in `frontend/` both pass.
- `http://localhost:5173/` lists Binary Search and Bubble Sort.
- Each demo runs with default parameters on load, plays through, steps back, and scrubs without any further network request.
- Changing the array and pressing Run produces a new trace and updates the query string.
- An unsorted array on Binary Search shows `array must be sorted ascending` and leaves the previous trace visible.
- Adding a demo requires no edit to `DemoController`, and adding a view kind requires no edit to an existing renderer.
