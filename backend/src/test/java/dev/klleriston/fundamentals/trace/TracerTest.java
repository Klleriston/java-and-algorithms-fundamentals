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
        tracer.step(1, "first", Map.of(), Map.of("i", 0), view());
        tracer.step(2, "second", Map.of(), Map.of("i", 1), view());

        assertThat(tracer.steps()).extracting(TraceStep::index).containsExactly(0, 1);
        assertThat(tracer.steps()).extracting(TraceStep::messageKey).containsExactly("first", "second");
    }

    @Test
    void rejectsStepsBeyondTheLimit() {
        Tracer tracer = new Tracer(2);
        tracer.step(1, "a", Map.of(), Map.of(), view());
        tracer.step(1, "b", Map.of(), Map.of(), view());

        assertThatThrownBy(() -> tracer.step(1, "c", Map.of(), Map.of(), view()))
                .isInstanceOf(StepLimitExceededException.class)
                .hasMessage("error.stepLimitExceeded")
                .extracting(exception -> ((StepLimitExceededException) exception).limit())
                .isEqualTo(2);
    }

    @Test
    void defaultLimitIsTenThousand() {
        assertThat(Tracer.DEFAULT_MAX_STEPS).isEqualTo(10_000);
    }

    @Test
    void returnedStepListIsUnmodifiable() {
        Tracer tracer = new Tracer();
        tracer.step(1, "a", Map.of(), Map.of(), view());

        List<TraceStep> steps = tracer.steps();
        assertThatThrownBy(() -> steps.add(steps.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
