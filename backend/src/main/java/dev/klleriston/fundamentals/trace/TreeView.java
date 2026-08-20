package dev.klleriston.fundamentals.trace;

import java.util.List;

public record TreeView(
        List<Node> nodes,
        Integer activeNode,
        List<Integer> path,
        Integer insertedNode) implements ViewPayload {

    public TreeView {
        nodes = List.copyOf(nodes);
        path = List.copyOf(path);
    }

    @Override
    public String kind() {
        return "TREE";
    }

    public record Node(int id, int value, Integer left, Integer right) {
    }
}
