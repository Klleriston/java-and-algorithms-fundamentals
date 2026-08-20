package dev.klleriston.fundamentals.trace;

public sealed interface ViewPayload permits ArrayView, LinkedListView {

    String kind();
}
