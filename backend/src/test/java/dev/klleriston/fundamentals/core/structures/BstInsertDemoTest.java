package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceStep;
import dev.klleriston.fundamentals.trace.TreeView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BstInsertDemoTest {

    private final BstInsertDemo demo = new BstInsertDemo();

    private Trace run(Map<String, Object> raw) {
        return demo.run(DemoParams.of(raw, demo.parameters()));
    }

    @Test
    void exposesItsIdentity() {
        assertThat(demo.id()).isEqualTo("bst-insert");
        assertThat(demo.category()).isEqualTo(Category.DATA_STRUCTURES);
        assertThat(demo.displaySource()).doesNotContain("tracer");
    }

    @Test
    void descendsComparingUntilItFindsAnEmptySpot() {
        Trace trace = run(Map.of("values", List.of(50, 30, 70, 20, 40, 60), "value", 35));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("bstInsert.compareLess", "bstInsert.compareGreater",
                        "bstInsert.compareLess", "bstInsert.attachLeft");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(5, 7, 5, 3);

        TreeView last = (TreeView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.path()).hasSize(3);
        assertThat(last.insertedNode()).isNotNull();
        assertThat(last.nodes()).extracting(TreeView.Node::value).contains(35);
    }

    @Test
    void changesNothingWhenTheValueIsAlreadyInTheTree() {
        Trace trace = run(Map.of("values", List.of(50, 30, 70), "value", 30));

        assertThat(trace.steps()).extracting(TraceStep::messageKey)
                .containsExactly("bstInsert.compareLess", "bstInsert.duplicate");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(5, 7);

        TreeView last = (TreeView) trace.steps().get(trace.steps().size() - 1).view();
        assertThat(last.insertedNode()).isNull();
        assertThat(last.nodes()).hasSize(3);
    }

    @Test
    void attachesToTheRootWhenTheTreeIsEmpty() {
        Trace trace = run(Map.of("values", List.of(), "value", 42));

        assertThat(trace.steps()).extracting(TraceStep::messageKey).containsExactly("bstInsert.attachRoot");
        assertThat(trace.steps()).extracting(TraceStep::line).containsExactly(3);
    }

    @Test
    void everyNodeReferencesChildrenThatExist() {
        Trace trace = run(Map.of("values", List.of(50, 30, 70, 20, 40, 60), "value", 35));

        TreeView last = (TreeView) trace.steps().get(trace.steps().size() - 1).view();
        List<Integer> ids = last.nodes().stream().map(TreeView.Node::id).toList();
        assertThat(last.nodes()).allSatisfy(node -> {
            if (node.left() != null) {
                assertThat(ids).contains(node.left());
            }
            if (node.right() != null) {
                assertThat(ids).contains(node.right());
            }
        });
    }

    @Test
    void everyNarratedComparisonIsActuallyTrue() {
        Trace trace = run(Map.of("values", List.of(50, 30, 70, 20, 40, 60), "value", 35));

        for (TraceStep step : trace.steps()) {
            Object value = step.messageArgs().get("value");
            Object node = step.messageArgs().get("node");
            if (step.messageKey().equals("bstInsert.compareLess")) {
                assertThat((Integer) value)
                        .as("compareLess step must have value < node")
                        .isLessThan((Integer) node);
            } else if (step.messageKey().equals("bstInsert.compareGreater")) {
                assertThat((Integer) value)
                        .as("compareGreater step must have value > node")
                        .isGreaterThan((Integer) node);
            }
        }
    }
}
