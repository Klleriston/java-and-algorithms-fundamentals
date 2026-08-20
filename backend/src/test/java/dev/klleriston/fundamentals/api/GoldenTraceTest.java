package dev.klleriston.fundamentals.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.DemoRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GoldenTraceTest {

    private static final Path FIXTURES = Path.of("..", "frontend", "src", "fixtures");

    @Autowired
    private DemoRegistry registry;

    /** Spring's mapper, so the fixture matches what the API actually returns (including non-null inclusion). */
    @Autowired
    private ObjectMapper springMapper;

    private ObjectMapper mapper() {
        return springMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    void binarySearchTraceMatchesFixture() throws Exception {
        assertGolden("binary-search", Map.of("array", List.of(2, 5, 8, 12, 20, 33), "target", 20));
    }

    @Test
    void bubbleSortTraceMatchesFixture() throws Exception {
        assertGolden("bubble-sort", Map.of("array", List.of(5, 2, 9, 1, 7)));
    }

    @Test
    void hashMapPutTraceMatchesFixture() throws Exception {
        assertGolden("hash-map-put", Map.of("keys", List.of(5, 21, 37, 8), "key", 13, "value", 99));
    }

    private void assertGolden(String demoId, Map<String, Object> params) throws Exception {
        Demo demo = registry.require(demoId);
        String actual = mapper().writeValueAsString(demo.run(DemoParams.of(params, demo.parameters())));

        Path fixture = FIXTURES.resolve(demoId + ".json");
        if (Boolean.getBoolean("golden.update") || !Files.exists(fixture)) {
            Files.createDirectories(FIXTURES);
            Files.writeString(fixture, actual + System.lineSeparator());
        }

        assertThat(Files.readString(fixture).trim())
                .as("Fixture %s is stale. Regenerate with -Dgolden.update=true", fixture)
                .isEqualTo(actual.trim());
    }
}
