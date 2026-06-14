package dev.sabti.alumni_connect.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// The single body shape for every error response — both the in-dispatch errors from
// GlobalExceptionHandler and the filter-chain errors from the security entry point / access-denied
// handler. fieldErrors is present only for validation failures (field -> messages); for all other
// errors it is null and omitted from the JSON (@JsonInclude NON_NULL), leaving
// timestamp/status/error/code/message. Built via the of(...) factory, same idiom as the DTOs' from(...).
//
// code is a stable, machine-readable label for the frontend to switch on without parsing the human
// message. It is coarse: by default it's the HTTP status name (NOT_FOUND, FORBIDDEN, CONFLICT,
// BAD_REQUEST). The security layer sets finer codes the status alone can't express — e.g. both
// TOKEN_EXPIRED and AUTH_REQUIRED are 401. If per-business-case codes are ever needed (e.g. telling
// the three different 409s apart), that's an additive change on ApiException, not here.
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private Instant timestamp;
    private int status;          // numeric HTTP status, e.g. 400
    private String error;        // reason phrase, e.g. "Bad Request"
    private String code;         // machine-readable, e.g. "NOT_FOUND", "TOKEN_EXPIRED"
    private String message;      // human-readable, safe to show the caller (never internal detail)
    private Map<String, List<String>> fieldErrors;

    // Default code is the status name — covers every domain error with no per-throw change.
    public static ApiError of(HttpStatus status, String message) {
        return of(status, message, status.name());
    }

    // Explicit code, for the cases the status name can't distinguish (the security 401/403 codes).
    public static ApiError of(HttpStatus status, String message, String code) {
        ApiError error = new ApiError();
        error.timestamp = Instant.now();
        error.status = status.value();
        error.error = status.getReasonPhrase();
        error.code = code;
        error.message = message;
        return error;
    }
}
