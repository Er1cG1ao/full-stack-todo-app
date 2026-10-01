package com.example.todobackend.exception;

/**
 * Business exception: the requested todo doesn't exist.
 *
 * The key design point: the service layer only throws — it says nothing about HTTP status
 * codes. Status codes are HTTP's business, so they belong to GlobalExceptionHandler.
 * That way the service can be reused in other contexts (a scheduled job, a CLI) without
 * dragging a pile of web concepts along with it.
 */
public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(String username, long id) {
        super("No todo with id=%d found for user %s".formatted(id, username));
    }
}
