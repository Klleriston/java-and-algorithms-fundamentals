package dev.klleriston.fundamentals.trace;

public sealed interface ViewPayload permits ArrayView, HashTableView, TreeView, LinkedListView {

    String kind();
}
