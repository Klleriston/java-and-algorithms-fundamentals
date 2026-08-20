package dev.klleriston.fundamentals.trace;

import java.util.List;

public record HashTableView(
        int capacity,
        List<Bucket> buckets,
        Integer activeBucket,
        String outcome) implements ViewPayload {

    public static final String INSERTED = "INSERTED";
    public static final String UPDATED = "UPDATED";
    public static final String DUPLICATE = "DUPLICATE";

    public HashTableView {
        buckets = List.copyOf(buckets);
    }

    @Override
    public String kind() {
        return "HASH_TABLE";
    }

    public record Bucket(int index, List<Entry> entries) {

        public Bucket {
            entries = List.copyOf(entries);
        }
    }

    public record Entry(int key, Integer value, String state) {

        public static final String NORMAL = "NORMAL";
        public static final String PROBED = "PROBED";
        public static final String MATCHED = "MATCHED";
        public static final String INSERTED = "INSERTED";
    }
}
