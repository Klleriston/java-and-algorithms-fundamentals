package dev.klleriston.fundamentals.trace;

public record TraceResult(Object returnValue, int stepCount, boolean measured) {
}
