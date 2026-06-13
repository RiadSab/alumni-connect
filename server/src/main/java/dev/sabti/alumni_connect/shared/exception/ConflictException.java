package dev.sabti.alumni_connect.shared.exception;

import org.springframework.http.HttpStatus;

// 409. The request clashes with the current state of a resource — e.g. already applied to this
// offer, the offer is no longer open, or the application cap has been reached.
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
