package dev.klleriston.fundamentals.trace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record TraceStep(
        int index,
        int line,
        String messageKey,
        Map<String, Object> messageArgs,
        Map<String, Object> vars,
        ViewPayload view) {

    public TraceStep {
        messageArgs = Collections.unmodifiableMap(new LinkedHashMap<>(messageArgs));
        vars = Collections.unmodifiableMap(new LinkedHashMap<>(vars));
    }
}
