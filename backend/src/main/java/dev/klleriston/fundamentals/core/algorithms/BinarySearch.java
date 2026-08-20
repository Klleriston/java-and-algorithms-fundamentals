package dev.klleriston.fundamentals.core.algorithms;

import dev.klleriston.fundamentals.trace.ArrayView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BinarySearch {

    private BinarySearch() {
    }

    public static int search(int[] a, int target, Tracer tracer) {
        int low = 0;
        int high = a.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (a[mid] == target) {
                tracer.step(6, "a[" + mid + "] = " + a[mid] + " equals " + target + ", found at index " + mid,
                        vars(low, high, mid), view(a, low, high, mid));
                return mid;
            } else if (a[mid] < target) {
                tracer.step(8, "a[" + mid + "] = " + a[mid] + " < " + target + ", discard the left half",
                        vars(low, high, mid), view(a, low, high, mid));
                low = mid + 1;
            } else {
                tracer.step(11, "a[" + mid + "] = " + a[mid] + " > " + target + ", discard the right half",
                        vars(low, high, mid), view(a, low, high, mid));
                high = mid - 1;
            }
        }
        tracer.step(14, target + " is not in the array, return -1", vars(low, high, -1), view(a, low, high, -1));
        return -1;
    }

    private static Map<String, Object> vars(int low, int high, int mid) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("low", low);
        vars.put("high", high);
        if (mid >= 0) {
            vars.put("mid", mid);
        }
        return vars;
    }

    private static ArrayView view(int[] a, int low, int high, int mid) {
        Map<String, Integer> pointers = new LinkedHashMap<>();
        pointers.put("low", low);
        pointers.put("high", high);
        if (mid >= 0) {
            pointers.put("mid", mid);
        }

        List<ArrayView.Range> ranges = new java.util.ArrayList<>();
        if (low > 0) {
            ranges.add(new ArrayView.Range(0, low - 1, ArrayView.Range.DISCARDED));
        }
        if (high < a.length - 1) {
            ranges.add(new ArrayView.Range(high + 1, a.length - 1, ArrayView.Range.DISCARDED));
        }

        List<Integer> items = Arrays.stream(a).boxed().toList();
        return new ArrayView(items, pointers, ranges, List.of());
    }
}
