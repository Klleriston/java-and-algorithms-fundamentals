package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.trace.LinkedListView;
import dev.klleriston.fundamentals.trace.Tracer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LinkedListInsert {

    private LinkedListInsert() {
    }

    private static final class Node {
        final int id;
        final int value;
        Node next;

        Node(int id, int value) {
            this.id = id;
            this.value = value;
        }
    }

    public static List<Integer> insert(List<Integer> values, int position, int value, Tracer tracer) {
        List<Node> all = new ArrayList<>();
        Node head = null;
        Node tail = null;
        for (int existing : values) {
            Node node = new Node(all.size(), existing);
            all.add(node);
            if (head == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        if (position == 0) {
            Node inserted = new Node(all.size(), value);
            all.add(inserted);
            inserted.next = head;
            head = inserted;
            tracer.step(3, "linkedListInsert.insertHead", args("value", value),
                    vars(position, value), view(all, head, inserted.id,
                            List.of(new LinkedListView.Link(inserted.id, inserted.next == null ? null : inserted.next.id)),
                            inserted.id));
            return valuesOf(head);
        }

        Node previous = head;
        for (int i = 0; i < position - 1; i++) {
            previous = previous.next;
            tracer.step(8, "linkedListInsert.walk", args("index", i + 1, "value", previous.value),
                    vars(position, value), view(all, head, previous.id, List.of(), null));
        }
        tracer.step(7, "linkedListInsert.arrive", args("position", position),
                vars(position, value), view(all, head, previous.id, List.of(), null));

        Node inserted = new Node(all.size(), value);
        all.add(inserted);
        inserted.next = previous.next;
        tracer.step(10, "linkedListInsert.linkNew",
                args("value", value, "next", inserted.next == null ? "null" : inserted.next.value),
                vars(position, value), view(all, head, inserted.id,
                        List.of(new LinkedListView.Link(inserted.id, inserted.next == null ? null : inserted.next.id)),
                        inserted.id));

        previous.next = inserted;
        tracer.step(11, "linkedListInsert.relinkPrev", args("prev", previous.value, "value", value),
                vars(position, value), view(all, head, inserted.id,
                        List.of(new LinkedListView.Link(previous.id, inserted.id)), inserted.id));

        return valuesOf(head);
    }

    private static List<Integer> valuesOf(Node head) {
        List<Integer> values = new ArrayList<>();
        for (Node node = head; node != null; node = node.next) {
            values.add(node.value);
        }
        return values;
    }

    private static Map<String, Object> vars(int position, int value) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("position", position);
        vars.put("value", value);
        return vars;
    }

    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }

    private static LinkedListView view(List<Node> all, Node head, Integer cursor,
                                       List<LinkedListView.Link> changedLinks, Integer newNode) {
        List<LinkedListView.Node> nodes = new ArrayList<>();
        for (Node node = head; node != null; node = node.next) {
            nodes.add(new LinkedListView.Node(node.id, node.value, node.next == null ? null : node.next.id));
        }
        return new LinkedListView(nodes, cursor, changedLinks, newNode);
    }
}
