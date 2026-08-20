package dev.klleriston.fundamentals.trace;

import java.util.Map;

public record TraceStep(int index, int line, String message, Map<String, Object> vars, ViewPayload view) {

    public TraceStep {
        vars = Map.copyOf(vars);
    }
}
