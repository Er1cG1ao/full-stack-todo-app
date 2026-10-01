package com.example.todobackend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The global exception translator: turns business exceptions into HTTP status codes plus a
 * consistent response body.
 *
 * Without it, TodoNotFoundException bubbles all the way up to Tomcat and becomes a 500 —
 * but "you asked for an id that doesn't exist" isn't a server failure, it's a 404.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Step 9-①: translate the business-level not-found exception into HTTP 404.
    @ExceptionHandler(TodoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTodoNotFound(
            TodoNotFoundException ex,
            HttpServletRequest request) {
        int status = HttpStatus.NOT_FOUND.value();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(status, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {
        int status = HttpStatus.NOT_FOUND.value();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(status, ex.getMessage(), request.getRequestURI()));
    }

    // Step 9-②: translate invalid input into HTTP 400 with the same error shape.
    @ExceptionHandler(InvalidTodoException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTodo(
            InvalidTodoException ex,
            HttpServletRequest request) {
        int status = HttpStatus.BAD_REQUEST.value();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(status, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        int status = HttpStatus.BAD_REQUEST.value();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        status,
                        "request body is malformed or contains invalid values",
                        request.getRequestURI()));
    }
}
