package dev.sabti.alumni_connect.shared.exception;

import org.springframework.http.HttpStatus;

// Base for domain errors we throw deliberately (as opposed to the framework exceptions Spring/JPA
// raise on their own). Each subclass fixes the HTTP status; the message is the human-readable text
// shown to the caller. A single advice handler maps any ApiException to an ApiError using
// getStatus()/getMessage(), so every domain error flows through the same envelope.
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
