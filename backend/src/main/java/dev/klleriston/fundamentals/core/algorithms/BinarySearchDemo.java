package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import dev.klleriston.fundamentals.trace.Tracer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BinarySearchDemo implements Demo {

    private static final String SOURCE = """
            public static int search(int[] a, int target) {
                int low = 0;
                int high = a.length - 1;
                while (low <= high) {
                    int mid = (low + high) / 2;
                    if (a[mid] == target) {
                        return mid;
                    } else if (a[mid] < target) {
                        low = mid + 1;
                    } else {
                        high = mid - 1;
                    }
                }
                return -1;
            }""";

    @Override
    public String id() {
        return "binary-search";
    }

    @Override
    public String title() {
        return "Binary Search";
    }

    @Override
    public Category category() {
        return Category.ALGORITHMS;
    }

    @Override
    public String description() {
        return "Halve the search range on every comparison to find a value in a sorted array.";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("array", "Sorted array", 512, List.of(2, 5, 8, 12, 20, 33)),
                ParameterSpec.integer("target", "Target value", -1000, 1000, 20));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        int[] array = params.intArray("array");
        requireSorted(array);
        int target = params.integer("target");

        Tracer tracer = new Tracer();
        int result = BinarySearch.search(array, target, tracer);

        return new Trace(id(), title(), SOURCE, tracer.steps(),
                new TraceResult(result, tracer.steps().size(), false));
    }

    private static void requireSorted(int[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i - 1] > array[i]) {
                throw new DemoInputException("array", "array must be sorted ascending");
            }
        }
    }
}
