package dev.klleriston.fundamentals.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoParamsTest {

    private static final List<ParameterSpec> SPECS = List.of(
            ParameterSpec.intArray("array", "Sorted array", 512, List.of(2, 5, 8)),
            ParameterSpec.integer("target", "Target", -1000, 1000, 5),
            ParameterSpec.enumOf("order", "Order", List.of("ASC", "DESC"), "ASC"));

    @Test
    void readsProvidedValues() {
        DemoParams params = DemoParams.of(
                Map.of("array", List.of(1, 3, 7), "target", 7, "order", "DESC"), SPECS);

        assertThat(params.intArray("array")).containsExactly(1, 3, 7);
        assertThat(params.integer("target")).isEqualTo(7);
        assertThat(params.enumValue("order")).isEqualTo("DESC");
    }

    @Test
    void fallsBackToDefaults() {
        DemoParams params = DemoParams.of(Map.of(), SPECS);

        assertThat(params.intArray("array")).containsExactly(2, 5, 8);
        assertThat(params.integer("target")).isEqualTo(5);
        assertThat(params.enumValue("order")).isEqualTo("ASC");
    }

    @Test
    void rejectsUnknownParameter() {
        assertThatThrownBy(() -> DemoParams.of(Map.of("nope", 1), SPECS))
                .isInstanceOf(DemoInputException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void rejectsIntegerOutOfRange() {
        assertThatThrownBy(() -> DemoParams.of(Map.of("target", 5000), SPECS))
                .isInstanceOf(DemoInputException.class)
                .extracting("field").isEqualTo("target");
    }

    @Test
    void rejectsArrayLongerThanMaxLength() {
        List<ParameterSpec> shortSpec = List.of(
                ParameterSpec.intArray("array", "Array", 2, List.of(1)));

        assertThatThrownBy(() -> DemoParams.of(Map.of("array", List.of(1, 2, 3)), shortSpec))
                .isInstanceOf(DemoInputException.class)
                .hasMessageContaining("2");
    }

    @Test
    void rejectsNonIntegerArrayElement() {
        assertThatThrownBy(() -> DemoParams.of(Map.of("array", List.of("x")), SPECS))
                .isInstanceOf(DemoInputException.class)
                .extracting("field").isEqualTo("array");
    }

    @Test
    void rejectsEnumValueOutsideOptions() {
        assertThatThrownBy(() -> DemoParams.of(Map.of("order", "SIDEWAYS"), SPECS))
                .isInstanceOf(DemoInputException.class)
                .extracting("field").isEqualTo("order");
    }
}
