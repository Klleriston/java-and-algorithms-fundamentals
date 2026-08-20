package dev.klleriston.fundamentals.core;

import java.util.List;

public record ParameterSpec(
        String name,
        ParameterType type,
        String label,
        boolean required,
        Integer min,
        Integer max,
        Integer maxLength,
        List<String> options,
        Object defaultValue) {

    public static ParameterSpec integer(String name, String label, int min, int max, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.INT, label, true, min, max, null, null, defaultValue);
    }

    public static ParameterSpec intArray(String name, String label, int maxLength, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.INT_ARRAY, label, true, null, null, maxLength, null, defaultValue);
    }

    public static ParameterSpec enumOf(String name, String label, List<String> options, Object defaultValue) {
        return new ParameterSpec(name, ParameterType.ENUM, label, true, null, null, null, List.copyOf(options), defaultValue);
    }
}
