package dev.klleriston.fundamentals.trace;

public sealed interface ViewPayload permits ArrayView, TreeView, LinkedListView {

    String kind();
}
