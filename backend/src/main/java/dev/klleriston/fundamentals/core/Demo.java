package dev.klleriston.fundamentals.core;

import dev.klleriston.fundamentals.trace.Trace;

import java.util.List;

public interface Demo {

    String id();

    String title();

    Category category();

    String description();

    List<ParameterSpec> parameters();

    /** Java source shown to the learner, without tracer calls. Step line numbers refer to this text. */
    String displaySource();

    Trace run(DemoParams params);
}
