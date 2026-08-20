package dev.klleriston.fundamentals.api;

public record ApiError(String error, String message, String field) {
}
