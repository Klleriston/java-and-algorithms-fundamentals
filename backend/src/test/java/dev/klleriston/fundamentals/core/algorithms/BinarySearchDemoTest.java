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
