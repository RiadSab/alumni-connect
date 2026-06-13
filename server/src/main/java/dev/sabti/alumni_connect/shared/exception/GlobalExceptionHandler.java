package dev.sabti.alumni_connect.shared.exception;

import dev.sabti.alumni_connect.storage.StorageException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Catches exceptions thrown within the controller/service dispatch and turns them into a consistent
// ApiError body with the right status. Two things to keep in mind:
//   - This does NOT see security failures (authentication/authorization happen in the filter chain,
//     before the controller) — those are handled in SecurityConfig.
//   - Handling an exception here keeps it inside the original dispatch, so it never becomes an
//     unhandled 500 that Spring forwards to /error (which would otherwise be re-masked as a 403).
// 4xx responses carry a caller-facing message; 5xx responses log the full detail and return a
// generic message, so internal detail (SQL, stack traces) never reaches the client.
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // @Valid on a @RequestBody failed — collect every field's messages (and any class-level/global
    // errors under "_global") into fieldErrors.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                fieldErrors.computeIfAbsent(e.getField(), k -> new ArrayList<>()).add(e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(e ->
                fieldErrors.computeIfAbsent("_global", k -> new ArrayList<>()).add(e.getDefaultMessage()));
        return validationResponse(fieldErrors);
    }

    // @Validated on @RequestParam/@PathVariable failed — keyed by the parameter path.
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleParamValidation(ConstraintViolationException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                fieldErrors.computeIfAbsent(v.getPropertyPath().toString(), k -> new ArrayList<>()).add(v.getMessage()));
        return validationResponse(fieldErrors);
    }

    // A path/query value couldn't bind to its target type — e.g. ?city=BOGUS for a JobCity enum, or a
    // non-numeric id. Previously surfaced as a masked 403.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Parameter '" + ex.getName() + "' has an invalid value.");
    }

    // The request body was missing or not valid JSON.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or malformed.");
    }

    // Wrong HTTP verb for the route (e.g. PUT where only PATCH exists).
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method " + ex.getMethod() + " is not supported on this endpoint.");
    }

    // An uploaded file exceeded the configured multipart size limit.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large.");
    }

    // A database constraint was violated — a unique collision (e.g. duplicate email) or a check
    // constraint. The exception's detail can include SQL/constraint names, so it is logged, not
    // returned. 409 Conflict: the request clashes with existing data.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data.");
    }

    // Storage backend failure (read/write/delete of a stored file). Internal — log and return a
    // generic 500.
    @ExceptionHandler(StorageException.class)
    ResponseEntity<ApiError> handleStorage(StorageException ex) {
        log.error("Storage failure", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "A storage error occurred.");
    }

    // Domain errors we throw deliberately (any ApiException subclass — NotFound/Forbidden/Conflict/
    // BadRequest). The status rides on the exception and the message is the caller-facing text, so
    // one handler covers them all. These are expected outcomes, not bugs, so no logging.
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    // Catch-all for anything not matched above — an unexpected bug. Log the full detail; never leak
    // it in the body.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private ResponseEntity<ApiError> validationResponse(Map<String, List<String>> fieldErrors) {
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST, "Validation failed.");
        body.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, message));
    }
}
