package dev.klleriston.fundamentals.core;

import java.util.List;

public record ParameterSpec(
        String name,
        ParameterType type,
        String labelKey,
        boolean required,
        Integer min,
        Integer max,
        Integer maxLength,
        List<String> options,
        Object defaultValue) {

    public static ParameterSpec integer(String name, String labelKey, int min, int max, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.INT, labelKey, true, min, max, null, null, defaultValue);
    }

    public static ParameterSpec intArray(String name, String labelKey, int maxLength, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.INT_ARRAY, labelKey, true, null, null, maxLength, null, defaultValue);
    }

    public static ParameterSpec enumOf(String name, String labelKey, List<String> options, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.ENUM, labelKey, true, null, null, null, List.copyOf(options), defaultValue);
    }
}
