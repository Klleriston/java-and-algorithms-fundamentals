package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.trace.ArrayView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BubbleSort {

    private BubbleSort() {
    }

    public static int[] sort(int[] a, Tracer tracer) {
        for (int i = 0; i < a.length - 1; i++) {
            for (int j = 0; j < a.length - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    int tmp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = tmp;
                    tracer.step(7, "bubbleSort.swap",
                            args("left", j, "right", j + 1, "leftValue", a[j], "rightValue", a[j + 1]),
                            vars(i, j), view(a, i, j, List.of(j, j + 1)));
                } else {
                    tracer.step(4, "bubbleSort.keep",
                            args("left", j, "right", j + 1, "leftValue", a[j], "rightValue", a[j + 1]),
                            vars(i, j), view(a, i, j, List.of()));
                }
            }
        }
        tracer.step(11, "bubbleSort.sorted", args(), vars(Math.max(a.length - 1, 0), 0),
                view(a, a.length, 0, List.of()));
        return a;
    }

    private static Map<String, Object> vars(int i, int j) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("i", i);
        vars.put("j", j);
        return vars;
    }

    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }

    private static ArrayView view(int[] a, int i, int j, List<Integer> swapped) {
        Map<String, Integer> pointers = new LinkedHashMap<>();
        pointers.put("j", j);
        pointers.put("j+1", Math.min(j + 1, Math.max(a.length - 1, 0)));

        List<ArrayView.Range> ranges = new ArrayList<>();
        int sortedFrom = a.length - i;
        if (sortedFrom < a.length) {
            ranges.add(new ArrayView.Range(sortedFrom, a.length - 1, ArrayView.Range.SORTED));
        }

        List<Integer> items = Arrays.stream(a).boxed().toList();
        return new ArrayView(items, pointers, ranges, swapped);
    }
}
