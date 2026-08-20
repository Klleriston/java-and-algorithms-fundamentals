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
        mutable.add(new TraceStep(0, 1, "start", Map.of(), Map.of("low", 0), sampleView()));

        Trace trace = new Trace("binary-search", "demo.binarySearch.title", "code", mutable,
                new TraceResult(1, 1, false));

        assertThatThrownBy(() -> trace.steps().add(mutable.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void traceDoesNotSeeLaterMutationsOfTheSourceList() {
        List<TraceStep> mutable = new ArrayList<>();
        mutable.add(new TraceStep(0, 1, "start", Map.of(), Map.of(), sampleView()));

        Trace trace = new Trace("binary-search", "demo.binarySearch.title", "code", mutable,
                new TraceResult(1, 1, false));
        mutable.add(new TraceStep(1, 2, "later", Map.of(), Map.of(), sampleView()));

        assertThat(trace.steps()).hasSize(1);
    }
}
