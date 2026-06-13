package dev.sabti.alumni_connect.shared.exception;

import org.springframework.http.HttpStatus;

// 400 for a domain rule that isn't bean-validation — the request is well-formed but asks for
// something invalid in context (e.g. useProfileResume requested but the candidate has no resume on
// file). Bean-validation failures keep their own framework handler.
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
