package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.LinkedListView;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkedListInsertDemoTest {

    private final LinkedListInsertDemo demo = new LinkedListInsertDemo();

    private Trace run(Map<String, Object> raw) {
        return demo.run(DemoParams.of(raw, demo.parameters()));
    }

    @Test
    void showsTheNewNodeBeforeItIsLinkedIn() {
        Trace trace = run(Map.of("values", List.of(10, 20, 30, 40), "position", 2, "value", 25));

        LinkedListView creating = (LinkedListView) trace.steps().get(2).view();
        assertThat(creating.newNode()).isNotNull();
        assertThat(creating.nodes()).extracting(LinkedListView.Node::value)
                .as("the created node is on screen even though nothing points at it yet")
                .contains(25);
        assertThat(creating.nodes())
                .as("nothing in the chain points at it until the next step")
                .noneMatch(node -> creating.newNode().equals(node.next()));
        assertThat(creating.nodes()).filteredOn(node -> node.value() == 25)
                .allSatisfy(node -> assertThat(node.next())
                        .as("the created node already points into the chain")
                        .isNotNull());

        LinkedListView linked = (LinkedListView) trace.steps().get(3).view();
        assertThat(linked.nodes()).anyMatch(node -> linked.newNode().equals(node.next()));
    }

    @Test
    void exposesItsIdentity() {
        assertThat(demo.id()).isEqualTo("linked-list-insert");
        assertThat(demo.category()).isEqualTo(Category.DATA_STRUCTURES);
        assertThat(demo.displaySource()).doesNotContain("tracer");
    }

    @Test
    void walksToThePositionThenRewiresTwoPointers() {
        Trace trace = run(Map.of("values", List.of(10, 20, 30, 40), "position", 2, "value", 25));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("linkedListInsert.walk", "linkedListInsert.arrive",
                        "linkedListInsert.linkNew", "linkedListInsert.relinkPrev");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(8, 7, 10, 11);

        LinkedListView last = (LinkedListView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.newNode()).isNotNull();
        assertThat(last.changedLinks()).hasSize(1);
        assertThat(valuesInOrder(last)).containsExactly(10, 20, 25, 30, 40);
    }

    @Test
    void insertsAtTheHeadWithoutWalking() {
        Trace trace = run(Map.of("values", List.of(10, 20), "position", 0, "value", 5));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("linkedListInsert.insertHead");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(3);

        LinkedListView last = (LinkedListView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(valuesInOrder(last)).containsExactly(5, 10, 20);
    }

    @Test
    void insertsAtThePositionEqualToLengthByAppending() {
        Trace trace = run(Map.of("values", List.of(10, 20), "position", 2, "value", 30));

        LinkedListView last = (LinkedListView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(valuesInOrder(last)).containsExactly(10, 20, 30);
    }

    @Test
    void rejectsAPositionPastTheEnd() {
        assertThatThrownBy(() -> run(Map.of("values", List.of(10, 20), "position", 5, "value", 1)))
                .isInstanceOf(DemoInputException.class)
                .satisfies(thrown -> assertThat(((DemoInputException) thrown).field()).isEqualTo("position"));
    }

    private static List<Integer> valuesInOrder(LinkedListView view) {
        List<Integer> values = new java.util.ArrayList<>();
        LinkedListView.Node head = view.nodes().stream()
                .filter(node -> view.nodes().stream().noneMatch(other -> other.next() != null && other.next() == node.id()))
                .findFirst()
                .orElseThrow();
        LinkedListView.Node current = head;
        while (current != null) {
            values.add(current.value());
            Integer next = current.next();
            current = next == null ? null
                    : view.nodes().stream().filter(node -> node.id() == next).findFirst().orElseThrow();
        }
        return values;
    }
}
