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
public class HashSetAddDemo implements Demo {

    private static final String SOURCE = """
            public boolean add(int value) {
                int index = value % table.length;
                Node node = table[index];
                while (node != null) {
                    if (node.value == value) {
                        return false;
                    }
                    node = node.next;
                }
                table[index] = new Node(value, table[index]);
                size++;
                return true;
            }""";

    @Override
    public String id() {
        return "hash-set-add";
    }

    @Override
    public String titleKey() {
        return "demo.hashSetAdd.title";
    }

    @Override
    public Category category() {
        return Category.DATA_STRUCTURES;
    }

    @Override
    public String descriptionKey() {
        return "demo.hashSetAdd.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("values", "demo.hashSetAdd.param.values", 32, List.of(5, 21, 37, 8)),
                ParameterSpec.integer("value", "demo.hashSetAdd.param.value", 0, 1000, 37));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        List<Integer> values = Arrays.stream(params.intArray("values")).boxed().toList();
        int value = params.integer("value");
        requireNonNegative(values, value);

        Tracer tracer = new Tracer();
        boolean added = HashSetAdd.add(values, value, tracer);

        return new Trace(id(), titleKey(), SOURCE, tracer.steps(),
                new TraceResult(added, tracer.steps().size(), false));
    }

    private static void requireNonNegative(List<Integer> values, int value) {
        if (value < 0) {
            throw new DemoInputException("value", "error.keyMustBeNonNegative", Map.of("value", value));
        }
        for (int existing : values) {
            if (existing < 0) {
                throw new DemoInputException("values", "error.keyMustBeNonNegative", Map.of("value", existing));
            }
        }
    }
}
