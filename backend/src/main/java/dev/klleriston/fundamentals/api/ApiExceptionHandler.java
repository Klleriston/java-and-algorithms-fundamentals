package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.UnknownDemoException;
import dev.klleriston.fundamentals.trace.StepLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DemoInputException.class)
    public ResponseEntity<ApiError> handleInvalidInput(DemoInputException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_INPUT", exception.messageKey(), exception.messageArgs(), exception.field()));
    }

    @ExceptionHandler(StepLimitExceededException.class)
    public ResponseEntity<ApiError> handleStepLimit(StepLimitExceededException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("STEP_LIMIT_EXCEEDED", "error.stepLimitExceeded",
                        Map.of("maxSteps", exception.limit()), null));
    }

    @ExceptionHandler(UnknownDemoException.class)
    public ResponseEntity<ApiError> handleUnknownDemo(UnknownDemoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("UNKNOWN_DEMO", "error.unknownDemo", Map.of("id", exception.demoId()), null));
    }

    @ExceptionHandler(DemoTimeoutException.class)
    public ResponseEntity<ApiError> handleTimeout(DemoTimeoutException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("EXECUTION_TIMEOUT", "error.executionTimeout",
                        Map.of("seconds", exception.limit().toSeconds()), null));
    }
}
