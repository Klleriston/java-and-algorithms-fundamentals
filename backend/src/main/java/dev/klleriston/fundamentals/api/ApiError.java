package dev.klleriston.fundamentals.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ApiError(String error, String messageKey, Map<String, Object> messageArgs, String field) {

    public ApiError {
        messageArgs = Collections.unmodifiableMap(new LinkedHashMap<>(messageArgs));
    }
}
