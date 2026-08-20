package dev.klleriston.fundamentals.trace;

public sealed interface ViewPayload permits ArrayView, HashTableView {

    String kind();
}
