package dev.klleriston.fundamentals.core;

public class UnknownDemoException extends RuntimeException {

    private final String demoId;

    public UnknownDemoException(String demoId) {
        super("Unknown demo: " + demoId);
        this.demoId = demoId;
    }

    public String demoId() {
        return demoId;
    }
}
