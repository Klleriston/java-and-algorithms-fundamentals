package dev.klleriston.fundamentals.api;

import java.time.Duration;

public class DemoTimeoutException extends RuntimeException {

    private final Duration limit;

    public DemoTimeoutException(Duration limit) {
        super("error.executionTimeout");
        this.limit = limit;
    }

    public Duration limit() {
        return limit;
    }
}
