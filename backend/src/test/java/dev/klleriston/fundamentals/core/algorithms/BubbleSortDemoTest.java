package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.ArrayView;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceStep;
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
    void everyNarratedComparisonIsTrue() {
        Trace trace = demo.run(DemoParams.of(Map.of("array", List.of(5, 2, 9, 1, 7)), demo.parameters()));

        assertThat(trace.steps()).isNotEmpty();
        for (TraceStep step : trace.steps()) {
            Map<String, Object> args = step.messageArgs();
            if ("bubbleSort.swap".equals(step.messageKey())) {
                assertThat((Integer) args.get("leftValue"))
                        .as("swap step %d claims %s > %s", step.index(), args.get("leftValue"), args.get("rightValue"))
                        .isGreaterThan((Integer) args.get("rightValue"));
            } else if ("bubbleSort.keep".equals(step.messageKey())) {
                assertThat((Integer) args.get("leftValue"))
                        .as("keep step %d claims %s <= %s", step.index(), args.get("leftValue"), args.get("rightValue"))
                        .isLessThanOrEqualTo((Integer) args.get("rightValue"));
            }
        }
    }

    @Test
    void highlightsTheLineThatNarratesEachStep() {
        Trace trace = demo.run(DemoParams.of(Map.of("array", List.of(5, 2, 9, 1, 7)), demo.parameters()));
        List<TraceStep> steps = trace.steps();

        assertThat(steps).allSatisfy(step -> {
            ArrayView view = (ArrayView) step.view();
            if (!view.swapped().isEmpty()) {
                assertThat(step.line()).isEqualTo(7);
            }
        });
        assertThat(steps.subList(0, steps.size() - 1))
                .filteredOn(step -> ((ArrayView) step.view()).swapped().isEmpty())
                .isNotEmpty()
                .allSatisfy(step -> assertThat(step.line()).isEqualTo(4));
        assertThat(steps.get(steps.size() - 1).line()).isEqualTo(11);
    }

    @Test
    void arrayParameterIsCappedAtSixtyFour() {
        assertThat(demo.parameters().get(0).maxLength()).isEqualTo(64);
    }
}
