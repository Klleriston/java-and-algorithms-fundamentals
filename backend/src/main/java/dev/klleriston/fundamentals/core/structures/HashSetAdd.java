package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.trace.HashTableView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HashSetAdd {

    public static final int CAPACITY = 8;

    private HashSetAdd() {
    }

    public static boolean add(List<Integer> existingValues, int value, Tracer tracer) {
        List<List<Integer>> table = new ArrayList<>();
        for (int i = 0; i < CAPACITY; i++) {
            table.add(new ArrayList<>());
        }
        for (int existing : existingValues) {
            table.get(existing % CAPACITY).add(0, existing);
        }

        int index = value % CAPACITY;
        tracer.step(2, "hashSetAdd.bucket", args("value", value, "bucket", index, "capacity", CAPACITY),
                vars(value, index), view(table, index, null, -1));

        List<Integer> chain = table.get(index);
        for (int position = 0; position < chain.size(); position++) {
            if (chain.get(position).intValue() == value) {
                tracer.step(6, "hashSetAdd.duplicate", args("value", value),
                        vars(value, index), view(table, index, HashTableView.DUPLICATE, position));
                return false;
            }
            tracer.step(5, "hashSetAdd.compare", args("value", value, "other", chain.get(position)),
                    vars(value, index), view(table, index, null, position));
        }

        chain.add(0, value);
        tracer.step(10, "hashSetAdd.insert", args("value", value, "bucket", index),
                vars(value, index), view(table, index, HashTableView.INSERTED, 0));
        return true;
    }

    private static Map<String, Object> vars(int value, int index) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("value", value);
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

    private static HashTableView view(List<List<Integer>> table, int activeBucket, String outcome, int highlighted) {
        List<HashTableView.Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            List<HashTableView.Entry> entries = new ArrayList<>();
            for (int position = 0; position < table.get(i).size(); position++) {
                entries.add(new HashTableView.Entry(table.get(i).get(position), null,
                        state(i, position, activeBucket, outcome, highlighted)));
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
        if (HashTableView.DUPLICATE.equals(outcome)) {
            return HashTableView.Entry.MATCHED;
        }
        return HashTableView.Entry.PROBED;
    }
}
