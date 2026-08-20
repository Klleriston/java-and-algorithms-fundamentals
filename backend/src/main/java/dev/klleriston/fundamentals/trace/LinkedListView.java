package dev.klleriston.fundamentals.trace;

import java.util.List;

public record LinkedListView(
        List<Node> nodes,
        Integer cursor,
        List<Link> changedLinks,
        Integer newNode) implements ViewPayload {

    public LinkedListView {
        nodes = List.copyOf(nodes);
        changedLinks = List.copyOf(changedLinks);
    }

    @Override
    public String kind() {
        return "LINKED_LIST";
    }

    public record Node(int id, int value, Integer next) {
    }

    public record Link(Integer from, Integer to) {
    }
}
