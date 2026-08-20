package dev.klleriston.fundamentals.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DemoInputException extends RuntimeException {

    private final String field;
    private final String messageKey;
    private final Map<String, Object> messageArgs;

    public DemoInputException(String field, String messageKey, Map<String, Object> messageArgs) {
        super(messageKey);
        this.field = field;
        this.messageKey = messageKey;
        this.messageArgs = Collections.unmodifiableMap(new LinkedHashMap<>(messageArgs));
    }

    public String field() {
        return field;
    }

    public String messageKey() {
        return messageKey;
    }

    public Map<String, Object> messageArgs() {
        return messageArgs;
    }
}
