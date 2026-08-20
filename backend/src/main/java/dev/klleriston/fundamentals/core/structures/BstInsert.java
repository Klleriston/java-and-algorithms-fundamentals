package dev.klleriston.fundamentals.core.structures;

import dev.klleriston.fundamentals.trace.Tracer;
import dev.klleriston.fundamentals.trace.TreeView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BstInsert {

    private BstInsert() {
    }

    private static final class Node {
        final int id;
        final int value;
        Node left;
        Node right;

        Node(int id, int value) {
            this.id = id;
            this.value = value;
        }
    }

    public static boolean insert(List<Integer> values, int value, Tracer tracer) {
        List<Node> all = new ArrayList<>();
        Node root = null;
        for (int existing : values) {
            root = attach(root, existing, all);
        }

        List<Integer> path = new ArrayList<>();
        Node current = root;
        while (current != null) {
            path.add(current.id);
            if (value < current.value) {
                tracer.step(5, "bstInsert.compareLess", args("value", value, "node", current.value),
                        vars(value, current.value), view(all, current.id, path, null));
                if (current.left == null) {
                    Node inserted = attachTo(current, value, all, true);
                    tracer.step(3, "bstInsert.attachLeft", args("value", value, "parent", current.value),
                            vars(value, current.value), view(all, inserted.id, path, inserted.id));
                    return true;
                }
                current = current.left;
            } else if (value > current.value) {
                tracer.step(7, "bstInsert.compareGreater", args("value", value, "node", current.value),
                        vars(value, current.value), view(all, current.id, path, null));
                if (current.right == null) {
                    Node inserted = attachTo(current, value, all, false);
                    tracer.step(3, "bstInsert.attachRight", args("value", value, "parent", current.value),
                            vars(value, current.value), view(all, inserted.id, path, inserted.id));
                    return true;
                }
                current = current.right;
            } else {
                tracer.step(7, "bstInsert.duplicate", args("value", value),
                        vars(value, current.value), view(all, current.id, path, null));
                return false;
            }
        }

        Node inserted = attach(null, value, all);
        tracer.step(3, "bstInsert.attachRoot", args("value", value),
                vars(value, value), view(all, inserted.id, List.of(inserted.id), inserted.id));
        return true;
    }

    private static Node attach(Node root, int value, List<Node> all) {
        if (root == null) {
            Node node = new Node(all.size(), value);
            all.add(node);
            return node;
        }
        Node current = root;
        while (true) {
            if (value < current.value) {
                if (current.left == null) {
                    attachTo(current, value, all, true);
                    return root;
                }
                current = current.left;
            } else if (value > current.value) {
                if (current.right == null) {
                    attachTo(current, value, all, false);
                    return root;
                }
                current = current.right;
            } else {
                return root;
            }
        }
    }

    private static Node attachTo(Node parent, int value, List<Node> all, boolean left) {
        Node node = new Node(all.size(), value);
        all.add(node);
        if (left) {
            parent.left = node;
        } else {
            parent.right = node;
        }
        return node;
    }

    private static Map<String, Object> vars(int value, int node) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("value", value);
        vars.put("node", node);
        return vars;
    }

    private static Map<String, Object> args(Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            args.put((String) pairs[i], pairs[i + 1]);
        }
        return args;
    }

    private static TreeView view(List<Node> all, Integer activeNode, List<Integer> path, Integer insertedNode) {
        List<TreeView.Node> nodes = all.stream()
                .map(node -> new TreeView.Node(node.id, node.value,
                        node.left == null ? null : node.left.id,
                        node.right == null ? null : node.right.id))
                .toList();
        return new TreeView(nodes, activeNode, List.copyOf(path), insertedNode);
    }
}
