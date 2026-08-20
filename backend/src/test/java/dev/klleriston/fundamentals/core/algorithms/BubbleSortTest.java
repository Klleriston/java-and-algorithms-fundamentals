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
