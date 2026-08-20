package dev.klleriston.fundamentals.trace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ArrayView(
        List<Integer> items,
        Map<String, Integer> pointers,
        List<Range> ranges,
        List<Integer> swapped) implements ViewPayload {

    public ArrayView {
        items = List.copyOf(items);
        pointers = Collections.unmodifiableMap(new LinkedHashMap<>(pointers));
        ranges = List.copyOf(ranges);
        swapped = List.copyOf(swapped);
    }

    @Override
    public String kind() {
        return "ARRAY";
    }

    public record Range(int from, int to, String state) {

        public static final String DISCARDED = "DISCARDED";
        public static final String SORTED = "SORTED";
        public static final String ACTIVE = "ACTIVE";
    }
}
