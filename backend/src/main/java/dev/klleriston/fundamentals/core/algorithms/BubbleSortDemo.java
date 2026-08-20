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
