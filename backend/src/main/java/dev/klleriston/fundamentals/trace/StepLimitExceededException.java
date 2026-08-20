package dev.klleriston.fundamentals.trace;

public class StepLimitExceededException extends RuntimeException {

    private final int limit;

    public StepLimitExceededException(int limit) {
        super("Execution produced more than " + limit + " steps. Try a smaller input.");
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }
}
