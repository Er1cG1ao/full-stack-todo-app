package com.example.todobackend.exception;

import java.time.Instant;

/**
 * The unified error response body. Its shape is always:
 *
 *   { "timestamp": "...", "status": 404, "message": "...", "path": "/users/..." }
 *
 * Why define our own? Look at the error handling in the lesson4 frontend:
 *
 *   `Request failed: ${error.status} ${error.error?.message ?? error.message}`
 *
 * The frontend reads error.error.message — so the response body must carry a message field.
 * ResponseEntity.notFound().build() returns an empty body, leaving the frontend with nothing
 * to show but "Http failure response...".
 *
 * Why is timestamp an Instant rather than a LocalDateTime?
 * LocalDateTime serializes to "2026-08-08T10:12:33.412" — no time zone, not valid RFC 3339,
 * so it fails OpenAPI's `format: date-time` check (redocly lint flags it).
 * Instant serializes to "2026-08-08T10:12:33.412Z" — the trailing Z (UTC) makes it unambiguous.
 * Rule of thumb: an instant in time carries a zone; a calendar date (targetDate) does not.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String path) {

    public static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(Instant.now(), status, message, path);
    }
}
