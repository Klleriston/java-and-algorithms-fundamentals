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
