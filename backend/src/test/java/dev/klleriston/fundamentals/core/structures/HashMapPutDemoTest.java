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

    /**
     * Every narrated step must state something true of the state at the moment it narrates.
     * "bucket" claims key % capacity == bucket; "compare" claims the walked key differs from
     * "other" (true, since equality would have short-circuited into "update" instead); "update"
     * and "insert" claim a value now sits at the head of the reported bucket.
     */
    @Test
    void everyNarratedStepIsTrueAtTheMomentItNarrates() {
        Trace trace = run(Map.of("keys", List.of(5, 21, 37, 8), "key", 13, "value", 99));

        for (TraceStep step : trace.steps()) {
            Map<String, Object> args = step.messageArgs();
            HashTableView view = (HashTableView) step.view();
            switch (step.messageKey()) {
                case "hashMapPut.bucket" -> {
                    int key = (Integer) args.get("key");
                    int bucket = (Integer) args.get("bucket");
                    int capacity = (Integer) args.get("capacity");
                    assertThat(key % capacity)
                            .as("step %d claims %d %% %d = %d", step.index(), key, capacity, bucket)
                            .isEqualTo(bucket);
                    assertThat(view.activeBucket()).isEqualTo(bucket);
                }
                case "hashMapPut.compare" -> {
                    int key = (Integer) args.get("key");
                    int other = (Integer) args.get("other");
                    assertThat(key)
                            .as("step %d claims %d is not %d", step.index(), key, other)
                            .isNotEqualTo(other);
                }
                case "hashMapPut.update" -> {
                    int key = (Integer) args.get("key");
                    int value = (Integer) args.get("value");
                    assertThat(view.buckets().get(view.activeBucket()).entries())
                            .as("step %d claims key %d now holds value %d", step.index(), key, value)
                            .anySatisfy(entry -> {
                                assertThat(entry.key()).isEqualTo(key);
                                assertThat(entry.value()).isEqualTo(value);
                            });
                }
                case "hashMapPut.insert" -> {
                    int key = (Integer) args.get("key");
                    int bucket = (Integer) args.get("bucket");
                    assertThat(view.buckets().get(bucket).entries().get(0).key())
                            .as("step %d claims %d is at the head of bucket %d", step.index(), key, bucket)
                            .isEqualTo(key);
                }
                default -> throw new AssertionError("unexpected message key " + step.messageKey());
            }
        }
    }
}
