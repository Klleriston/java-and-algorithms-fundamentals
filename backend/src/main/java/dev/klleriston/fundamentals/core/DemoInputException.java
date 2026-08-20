package dev.klleriston.fundamentals.core;

public class DemoInputException extends RuntimeException {

    private final String field;

    public DemoInputException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
