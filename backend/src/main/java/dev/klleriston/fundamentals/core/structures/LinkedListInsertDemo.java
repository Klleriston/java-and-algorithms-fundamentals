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
public class LinkedListInsertDemo implements Demo {

    private static final String SOURCE = """
            public void insert(int position, int value) {
                if (position == 0) {
                    head = new Node(value, head);
                    return;
                }
                Node previous = head;
                for (int i = 0; i < position - 1; i++) {
                    previous = previous.next;
                }
                Node node = new Node(value, previous.next);
                previous.next = node;
                size++;
            }""";

    @Override
    public String id() {
        return "linked-list-insert";
    }

    @Override
    public String titleKey() {
        return "demo.linkedListInsert.title";
    }

    @Override
    public Category category() {
        return Category.DATA_STRUCTURES;
    }

    @Override
    public String descriptionKey() {
        return "demo.linkedListInsert.description";
    }

    @Override
    public List<ParameterSpec> parameters() {
        return List.of(
                ParameterSpec.intArray("values", "demo.linkedListInsert.param.values", 32, List.of(10, 20, 30, 40)),
                ParameterSpec.integer("position", "demo.linkedListInsert.param.position", 0, 32, 2),
                ParameterSpec.integer("value", "demo.linkedListInsert.param.value", -1000, 1000, 25));
    }

    @Override
    public String displaySource() {
        return SOURCE;
    }

    @Override
    public Trace run(DemoParams params) {
        List<Integer> values = Arrays.stream(params.intArray("values")).boxed().toList();
        int position = params.integer("position");
        int value = params.integer("value");
        if (position > values.size()) {
            throw new DemoInputException("position", "error.positionPastTheEnd",
                    Map.of("position", position, "size", values.size()));
        }

        Tracer tracer = new Tracer();
        List<Integer> result = LinkedListInsert.insert(values, position, value, tracer);

        return new Trace(id(), titleKey(), SOURCE, tracer.steps(),
                new TraceResult(result, tracer.steps().size(), false));
    }
}
