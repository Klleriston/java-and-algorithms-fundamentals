package dev.klleriston.fundamentals.core;

import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoRegistryTest {

    private static Demo stub(String id) {
        return new Demo() {
            @Override public String id() { return id; }
            @Override public String title() { return id; }
            @Override public Category category() { return Category.ALGORITHMS; }
            @Override public String description() { return "stub"; }
            @Override public List<ParameterSpec> parameters() { return List.of(); }
            @Override public String displaySource() { return "code"; }
            @Override public Trace run(DemoParams params) {
                return new Trace(id, id, "code", List.of(), new TraceResult(null, 0, false));
            }
        };
    }

    @Test
    void findsDemoById() {
        DemoRegistry registry = new DemoRegistry(List.of(stub("bubble-sort"), stub("binary-search")));

        assertThat(registry.require("binary-search").id()).isEqualTo("binary-search");
    }

    @Test
    void listsDemosSortedById() {
        DemoRegistry registry = new DemoRegistry(List.of(stub("bubble-sort"), stub("binary-search")));

        assertThat(registry.all()).extracting(Demo::id).containsExactly("binary-search", "bubble-sort");
    }

    @Test
    void rejectsUnknownId() {
        DemoRegistry registry = new DemoRegistry(List.of(stub("binary-search")));

        assertThatThrownBy(() -> registry.require("nope"))
                .isInstanceOf(UnknownDemoException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void rejectsDuplicateIds() {
        assertThatThrownBy(() -> new DemoRegistry(List.of(stub("dup"), stub("dup"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dup");
    }
}
