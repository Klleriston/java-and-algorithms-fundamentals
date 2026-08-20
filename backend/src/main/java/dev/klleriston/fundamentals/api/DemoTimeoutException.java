package dev.klleriston.fundamentals.api;

import java.time.Duration;

public class DemoTimeoutException extends RuntimeException {

    public DemoTimeoutException(Duration limit) {
        super("The demo took too long to run (limit: " + limit.toSeconds() + "s). Try a smaller input.");
    }
}
