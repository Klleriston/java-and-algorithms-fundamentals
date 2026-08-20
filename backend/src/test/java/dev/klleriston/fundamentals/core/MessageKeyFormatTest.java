package dev.klleriston.fundamentals.core;

import dev.klleriston.fundamentals.trace.TraceStep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MessageKeyFormatTest {

    private static final Pattern KEY = Pattern.compile("^[a-z][A-Za-z0-9]*(\\.[a-zA-Z0-9]+)+$");

    @Autowired
    private List<Demo> demos;

    @Test
    void everyKeyLooksLikeAKeyAndNotLikeASentence() {
        assertThat(demos).isNotEmpty();
        for (Demo demo : demos) {
            assertThat(demo.titleKey()).matches(KEY);
            assertThat(demo.descriptionKey()).matches(KEY);
            demo.parameters().forEach(parameter -> assertThat(parameter.labelKey()).matches(KEY));

            for (TraceStep step : demo.run(defaults(demo)).steps()) {
                assertThat(step.messageKey())
                        .as("step %d of %s", step.index(), demo.id())
                        .matches(KEY);
            }
        }
    }

    private static DemoParams defaults(Demo demo) {
        Map<String, Object> raw = new LinkedHashMap<>();
        demo.parameters().forEach(parameter -> raw.put(parameter.name(), parameter.defaultValue()));
        return DemoParams.of(raw, demo.parameters());
    }
}
