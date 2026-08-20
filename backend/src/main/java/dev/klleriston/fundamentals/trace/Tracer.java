package dev.klleriston.fundamentals.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Tracer {

    public static final int DEFAULT_MAX_STEPS = 10_000;

    private final int maxSteps;
    private final List<TraceStep> steps = new ArrayList<>();

    public Tracer() {
        this(DEFAULT_MAX_STEPS);
    }

    public Tracer(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public void step(int line, String messageKey, Map<String, Object> messageArgs,
                     Map<String, Object> vars, ViewPayload view) {
        if (steps.size() >= maxSteps) {
            throw new StepLimitExceededException(maxSteps);
        }
        steps.add(new TraceStep(steps.size(), line, messageKey, messageArgs, vars, view));
    }

    public List<TraceStep> steps() {
        return List.copyOf(steps);
    }
}
