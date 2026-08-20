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

    /**
     * Every narrated step must state something true of the state at the moment it narrates.
     * "bucket" claims value % capacity == bucket; "compare" claims the walked value differs
     * from "other" (true, since equality would have short-circuited into "duplicate" instead);
     * "duplicate" claims the reported value is already in the reported bucket; "insert" claims
     * a value now sits at the head of the reported bucket.
     */
    @Test
    void everyNarratedStepIsTrueAtTheMomentItNarrates() {
        Trace trace = run(Map.of("values", List.of(5, 21, 37, 8), "value", 13));

        for (TraceStep step : trace.steps()) {
            Map<String, Object> args = step.messageArgs();
            HashTableView view = (HashTableView) step.view();
            switch (step.messageKey()) {
                case "hashSetAdd.bucket" -> {
                    int value = (Integer) args.get("value");
                    int bucket = (Integer) args.get("bucket");
                    int capacity = (Integer) args.get("capacity");
                    assertThat(value % capacity)
                            .as("step %d claims %d %% %d = %d", step.index(), value, capacity, bucket)
                            .isEqualTo(bucket);
                    assertThat(view.activeBucket()).isEqualTo(bucket);
                }
                case "hashSetAdd.compare" -> {
                    int value = (Integer) args.get("value");
                    int other = (Integer) args.get("other");
                    assertThat(value)
                            .as("step %d claims %d is not %d", step.index(), value, other)
                            .isNotEqualTo(other);
                }
                case "hashSetAdd.duplicate" -> {
                    int value = (Integer) args.get("value");
                    assertThat(view.buckets().get(view.activeBucket()).entries())
                            .as("step %d claims %d is already in bucket %d", step.index(), value, view.activeBucket())
                            .anySatisfy(entry -> assertThat(entry.key()).isEqualTo(value));
                }
                case "hashSetAdd.insert" -> {
                    int value = (Integer) args.get("value");
                    int bucket = (Integer) args.get("bucket");
                    assertThat(view.buckets().get(bucket).entries().get(0).key())
                            .as("step %d claims %d is at the head of bucket %d", step.index(), value, bucket)
                            .isEqualTo(value);
                }
                default -> throw new AssertionError("unexpected message key " + step.messageKey());
            }
        }

        Trace duplicateTrace = run(Map.of("values", List.of(5, 21, 37, 8), "value", 37));
        for (TraceStep step : duplicateTrace.steps()) {
            Map<String, Object> args = step.messageArgs();
            HashTableView view = (HashTableView) step.view();
            if ("hashSetAdd.duplicate".equals(step.messageKey())) {
                int value = (Integer) args.get("value");
                assertThat(view.buckets().get(view.activeBucket()).entries())
                        .as("step %d claims %d is already in bucket %d", step.index(), value, view.activeBucket())
                        .anySatisfy(entry -> assertThat(entry.key()).isEqualTo(value));
            }
        }
    }
}
