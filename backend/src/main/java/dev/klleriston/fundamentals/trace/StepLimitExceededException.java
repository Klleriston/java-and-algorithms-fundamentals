package dev.klleriston.fundamentals.trace;

public class StepLimitExceededException extends RuntimeException {

    private final int limit;

    public StepLimitExceededException(int limit) {
        super("error.stepLimitExceeded");
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }
}
