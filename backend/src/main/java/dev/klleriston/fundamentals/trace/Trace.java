package dev.klleriston.fundamentals.trace;

import java.util.List;

public record Trace(
        String demoId,
        String title,
        String sourceCode,
        List<TraceStep> steps,
        TraceResult result) {

    public Trace {
        steps = List.copyOf(steps);
    }
}
