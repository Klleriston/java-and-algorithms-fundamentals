package dev.klleriston.fundamentals.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DemoParams {

    private final Map<String, Object> values;

    private DemoParams(Map<String, Object> values) {
        this.values = values;
    }

    public static DemoParams of(Map<String, Object> raw, List<ParameterSpec> specs) {
        Map<String, ParameterSpec> byName = new LinkedHashMap<>();
        specs.forEach(spec -> byName.put(spec.name(), spec));

        for (String provided : raw.keySet()) {
            if (!byName.containsKey(provided)) {
                throw new DemoInputException(provided, "Unknown parameter: " + provided);
            }
        }

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (ParameterSpec spec : specs) {
            Object value = raw.containsKey(spec.name()) ? raw.get(spec.name()) : spec.defaultValue();
            if (value == null) {
                if (spec.required()) {
                    throw new DemoInputException(spec.name(), "Missing required parameter: " + spec.name());
                }
                continue;
            }
            resolved.put(spec.name(), coerce(spec, value));
        }
        return new DemoParams(resolved);
    }

    private static Object coerce(ParameterSpec spec, Object value) {
        return switch (spec.type()) {
            case INT, LONG -> coerceInteger(spec, value);
            case INT_ARRAY -> coerceIntArray(spec, value);
            case ENUM, STRING_ARRAY -> coerceString(spec, value);
        };
    }

    private static int coerceInteger(ParameterSpec spec, Object value) {
        if (!(value instanceof Number number)) {
            throw new DemoInputException(spec.name(), spec.name() + " must be a number");
        }
        int intValue = number.intValue();
        if (spec.min() != null && intValue < spec.min()) {
            throw new DemoInputException(spec.name(), spec.name() + " must be at least " + spec.min());
        }
        if (spec.max() != null && intValue > spec.max()) {
            throw new DemoInputException(spec.name(), spec.name() + " must be at most " + spec.max());
        }
        return intValue;
    }

    private static int[] coerceIntArray(ParameterSpec spec, Object value) {
        if (!(value instanceof List<?> list)) {
            throw new DemoInputException(spec.name(), spec.name() + " must be an array of integers");
        }
        if (spec.maxLength() != null && list.size() > spec.maxLength()) {
            throw new DemoInputException(spec.name(),
                    spec.name() + " must have at most " + spec.maxLength() + " elements");
        }
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Number number)) {
                throw new DemoInputException(spec.name(), spec.name() + " must contain only integers");
            }
            result[i] = number.intValue();
        }
        return result;
    }

    private static String coerceString(ParameterSpec spec, Object value) {
        if (!(value instanceof String text)) {
            throw new DemoInputException(spec.name(), spec.name() + " must be a string");
        }
        if (spec.options() != null && !spec.options().contains(text)) {
            throw new DemoInputException(spec.name(),
                    spec.name() + " must be one of " + String.join(", ", spec.options()));
        }
        return text;
    }

    public int[] intArray(String name) {
        return (int[]) values.get(name);
    }

    public int integer(String name) {
        return (Integer) values.get(name);
    }

    public String enumValue(String name) {
        return (String) values.get(name);
    }
}
