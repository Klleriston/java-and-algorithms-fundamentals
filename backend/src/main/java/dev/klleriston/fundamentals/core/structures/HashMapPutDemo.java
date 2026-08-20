package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import dev.klleriston.fundamentals.trace.Tracer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class HashMapPutDemo implements Demo {

    private static final String SOURCE = """
            public void put(int key, int value) {
                int index = key % table.length;
                Node node = table[index];
                while (node != null) {
                    if (node.key == key) {
                        node.value = value;
                        return;
                    }
                    node = node.next;
                }
                table[index] = new Node(key, value, table[index]);
                size++;
            }""";

    @Override
    public String id() {
        return "hash-map-put";
    }

    @Override
    public String titleKey() {
        return "demo.hashMapPut.title";
    }

    @Override
    public Category category() {
        return Category.DATA_STRUCTURES;
    }

    @Override
    public String descriptionKey() {
        return "demo.hashMapPut.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("keys", "demo.hashMapPut.param.keys", 32, List.of(5, 21, 37, 8)),
                ParameterSpec.integer("key", "demo.hashMapPut.param.key", 0, 1000, 13),
                ParameterSpec.integer("value", "demo.hashMapPut.param.value", -1000, 1000, 99));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        List<Integer> keys = Arrays.stream(params.intArray("keys")).boxed().toList();
        int key = params.integer("key");
        int value = params.integer("value");
        requireNonNegative(keys, key);

        Tracer tracer = new Tracer();
        String outcome = HashMapPut.put(keys, key, value, tracer);

        return new Trace(id(), titleKey(), SOURCE, tracer.steps(),
                new TraceResult(outcome, tracer.steps().size(), false));
    }

    private static void requireNonNegative(List<Integer> keys, int key) {
        if (key < 0) {
            throw new DemoInputException("key", "error.keyMustBeNonNegative", Map.of("value", key));
        }
        for (int existing : keys) {
            if (existing < 0) {
                throw new DemoInputException("keys", "error.keyMustBeNonNegative", Map.of("value", existing));
            }
        }
    }
}
