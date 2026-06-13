package dev.sabti.alumni_connect.shared.exception;

import org.springframework.http.HttpStatus;

// 404. Also the correct choice for "exists, but not yours": throwing this (rather than
// ForbiddenException) keeps an inaccessible id indistinguishable from a missing one, so a caller
// can't probe which ids exist.
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
