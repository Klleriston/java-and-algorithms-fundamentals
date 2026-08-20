package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.ParameterSpec;

import java.util.List;

public record DemoSummary(
        String id,
        String title,
        Category category,
        String description,
        List<ParameterSpec> parameters,
        String sourceCode) {

    public static DemoSummary from(Demo demo) {
        return new DemoSummary(demo.id(), demo.title(), demo.category(), demo.description(),
                demo.parameters(), demo.displaySource());
    }
}
