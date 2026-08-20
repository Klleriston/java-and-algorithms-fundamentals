package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.ParameterSpec;

import java.util.List;

public record DemoSummary(
        String id,
        String titleKey,
        Category category,
        String descriptionKey,
        List<ParameterSpec> parameters,
        String sourceCode) {

    public static DemoSummary from(Demo demo) {
        return new DemoSummary(demo.id(), demo.titleKey(), demo.category(), demo.descriptionKey(),
                demo.parameters(), demo.displaySource());
    }
}
