package dev.sabti.alumni_connect.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// The single body shape for every error response produced by GlobalExceptionHandler. fieldErrors is
// present only for validation failures (field -> messages); for all other errors it is null and
// omitted from the JSON (@JsonInclude NON_NULL), leaving timestamp/status/error/message. Built via
// the of(...) factory, same idiom as the DTOs' from(...).
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private Instant timestamp;
    private int status;          // numeric HTTP status, e.g. 400
    private String error;        // reason phrase, e.g. "Bad Request"
    private String message;      // human-readable, safe to show the caller (never internal detail)
    private Map<String, List<String>> fieldErrors;

    public static ApiError of(HttpStatus status, String message) {
        ApiError error = new ApiError();
        error.timestamp = Instant.now();
        error.status = status.value();
        error.error = status.getReasonPhrase();
        error.message = message;
        return error;
    }
}
