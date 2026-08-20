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
