package dev.sabti.alumni_connect.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// The single body shape for every error response. fieldErrors appears only for validation failures
// and is omitted otherwise (@JsonInclude NON_NULL). code is a stable label the frontend can switch on:
// the HTTP status name by default, or an explicit one for the security cases the status can't tell
// apart (TOKEN_EXPIRED vs AUTH_REQUIRED are both 401).
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;        // reason phrase, e.g. "Bad Request"
    private String code;         // machine-readable, e.g. "NOT_FOUND", "TOKEN_EXPIRED"
    private String message;      // human-readable, safe to show the caller
    private Map<String, List<String>> fieldErrors;

    public static ApiError of(HttpStatus status, String message) {
        return of(status, message, status.name());
    }

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
