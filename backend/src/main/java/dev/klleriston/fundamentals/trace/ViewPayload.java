package dev.klleriston.fundamentals.trace;

public sealed interface ViewPayload permits ArrayView, HashTableView, TreeView {

    String kind();
}
