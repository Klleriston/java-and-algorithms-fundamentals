package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.trace.HashTableView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HashMapPut {

    public static final int CAPACITY = 8;

    private HashMapPut() {
    }

    /** Entry in a bucket chain. Newest first, like the didactic source shown on screen. */
    record Entry(int key, int value) {
    }

    public static String put(List<Integer> existingKeys, int key, int value, Tracer tracer) {
        List<List<Entry>> table = new ArrayList<>();
        for (int i = 0; i < CAPACITY; i++) {
            table.add(new ArrayList<>());
        }
        for (int existing : existingKeys) {
            table.get(existing % CAPACITY).add(0, new Entry(existing, existing));
        }

        int index = key % CAPACITY;
        tracer.step(2, "hashMapPut.bucket", args("key", key, "bucket", index, "capacity", CAPACITY),
                vars(key, index), view(table, index, null, -1));

        List<Entry> chain = table.get(index);
        for (int position = 0; position < chain.size(); position++) {
            Entry entry = chain.get(position);
            if (entry.key() == key) {
                chain.set(position, new Entry(key, value));
                tracer.step(6, "hashMapPut.update", args("key", key, "value", value),
                        vars(key, index), view(table, index, HashTableView.UPDATED, position));
                return HashTableView.UPDATED;
            }
            tracer.step(5, "hashMapPut.compare", args("key", key, "other", entry.key()),
                    vars(key, index), view(table, index, null, position));
        }

        chain.add(0, new Entry(key, value));
        tracer.step(11, "hashMapPut.insert", args("key", key, "bucket", index),
                vars(key, index), view(table, index, HashTableView.INSERTED, 0));
        return HashTableView.INSERTED;
    }

    private static Map<String, Object> vars(int key, int index) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("key", key);
        vars.put("index", index);
        return vars;
    }

    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }

    private static HashTableView view(List<List<Entry>> table, int activeBucket, String outcome, int highlighted) {
        List<HashTableView.Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            List<HashTableView.Entry> entries = new ArrayList<>();
            for (int position = 0; position < table.get(i).size(); position++) {
                Entry entry = table.get(i).get(position);
                entries.add(new HashTableView.Entry(entry.key(), entry.value(), state(i, position, activeBucket, outcome, highlighted)));
            }
            buckets.add(new HashTableView.Bucket(i, entries));
        }
        return new HashTableView(CAPACITY, buckets, activeBucket, outcome);
    }

    private static String state(int bucket, int position, int activeBucket, String outcome, int highlighted) {
        if (bucket != activeBucket || position != highlighted) {
            return HashTableView.Entry.NORMAL;
        }
        if (HashTableView.INSERTED.equals(outcome)) {
            return HashTableView.Entry.INSERTED;
        }
        if (HashTableView.UPDATED.equals(outcome)) {
            return HashTableView.Entry.MATCHED;
        }
        return HashTableView.Entry.PROBED;
    }
}
