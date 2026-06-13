package dev.sabti.alumni_connect.shared.exception;

import org.springframework.http.HttpStatus;

// 403. The caller is authenticated but not allowed to perform this action on this resource (e.g. not
// the company OWNER). For "not yours" cases where existence must not leak, use NotFoundException
// instead.
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
