package dev.klleriston.fundamentals.core.structures;

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
public class BstInsertDemo implements Demo {

    private static final String SOURCE = """
            public Node insert(Node node, int value) {
                if (node == null) {
                    return new Node(value);
                }
                if (value < node.value) {
                    node.left = insert(node.left, value);
                } else if (value > node.value) {
                    node.right = insert(node.right, value);
                }
                return node;
            }""";

    @Override
    public String id() {
        return "bst-insert";
    }

    @Override
    public String titleKey() {
        return "demo.bstInsert.title";
    }

    @Override
    public Category category() {
        return Category.DATA_STRUCTURES;
    }

    @Override
    public String descriptionKey() {
        return "demo.bstInsert.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("values", "demo.bstInsert.param.values", 31, List.of(50, 30, 70, 20, 40, 60)),
                ParameterSpec.integer("value", "demo.bstInsert.param.value", -1000, 1000, 35));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        List<Integer> values = Arrays.stream(params.intArray("values")).boxed().toList();
        int value = params.integer("value");

        Tracer tracer = new Tracer();
        boolean inserted = BstInsert.insert(values, value, tracer);

        return new Trace(id(), titleKey(), SOURCE, tracer.steps(),
                new TraceResult(inserted, tracer.steps().size(), false));
    }
}
