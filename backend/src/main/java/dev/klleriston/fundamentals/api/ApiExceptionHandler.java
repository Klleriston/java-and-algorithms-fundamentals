package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.UnknownDemoException;
import dev.klleriston.fundamentals.trace.StepLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DemoInputException.class)
    public ResponseEntity<ApiError> handleInvalidInput(DemoInputException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_INPUT", exception.getMessage(), exception.field()));
    }

    @ExceptionHandler(StepLimitExceededException.class)
    public ResponseEntity<ApiError> handleStepLimit(StepLimitExceededException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("STEP_LIMIT_EXCEEDED", exception.getMessage(), null));
    }

    @ExceptionHandler(UnknownDemoException.class)
    public ResponseEntity<ApiError> handleUnknownDemo(UnknownDemoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("UNKNOWN_DEMO", exception.getMessage(), null));
    }
}
