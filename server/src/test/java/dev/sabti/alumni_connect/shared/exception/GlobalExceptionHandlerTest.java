package dev.sabti.alumni_connect.shared.exception;

import dev.sabti.alumni_connect.storage.StorageException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// The GlobalExceptionHandler is a plain bean, so each handler is called directly (no web layer) and
// the ApiError envelope is asserted. Two things matter most: domain exceptions map to their declared
// status + a status-derived code, and internal failures (data integrity, unexpected bugs) return a
// generic message that never leaks the exception's detail.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // --- domain exceptions (ApiException hierarchy) --------------------------

    @Test
    void notFoundException_maps404WithNotFoundCodeAndPassesMessageThrough() {
        ResponseEntity<ApiError> response = handler.handleApiException(new NotFoundException("Job offer not found"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("Job offer not found");
    }

    @Test
    void forbiddenException_maps403WithForbiddenCode() {
        ResponseEntity<ApiError> response = handler.handleApiException(new ForbiddenException("nope"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void conflictException_maps409WithConflictCode() {
        ResponseEntity<ApiError> response = handler.handleApiException(new ConflictException("already there"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getCode()).isEqualTo("CONFLICT");
    }

    @Test
    void badRequestException_maps400WithBadRequestCode() {
        ResponseEntity<ApiError> response = handler.handleApiException(new BadRequestException("bad"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
    }

    // --- no internal detail leaks --------------------------------------------

    @Test
    void dataIntegrityViolation_returns409WithGenericMessage_notTheConstraintDetail() {
        var ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"users_email_key\"");

        ResponseEntity<ApiError> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("The request conflicts with existing data.");
        assertThat(response.getBody().getMessage()).doesNotContain("users_email_key");
    }

    @Test
    void unexpectedException_returns500WithGenericMessage_notTheCause() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new RuntimeException("NPE at SecretService line 42"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred.");
        assertThat(response.getBody().getMessage()).doesNotContain("SecretService");
    }

    @Test
    void storageException_returns500WithGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleStorage(new StorageException("disk full at /var/data"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("A storage error occurred.");
        assertThat(response.getBody().getMessage()).doesNotContain("/var/data");
    }

    // --- validation ----------------------------------------------------------

    @Test
    void bodyValidation_returns400WithFieldAndGlobalErrors() throws Exception {
        MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("sampleMethod", String.class), 0);
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "email", "must not be blank"));
        bindingResult.addError(new ObjectError("dto", "passwords must match"));

        ResponseEntity<ApiError> response =
                handler.handleBodyValidation(new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().getFieldErrors()).containsEntry("email", java.util.List.of("must not be blank"));
        assertThat(response.getBody().getFieldErrors()).containsEntry("_global", java.util.List.of("passwords must match"));
    }

    @Test
    void paramValidation_returns400WithFieldErrorsKeyedByPropertyPath() {
        ConstraintViolationException ex;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            ex = new ConstraintViolationException(validator.validate(new Sample()));
        }

        ResponseEntity<ApiError> response = handler.handleParamValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getFieldErrors()).containsKey("value");
    }

    // --- framework exceptions ------------------------------------------------

    @Test
    void typeMismatch_returns400NamingTheParameter() throws Exception {
        MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("sampleMethod", String.class), 0);
        var ex = new MethodArgumentTypeMismatchException("BOGUS", Integer.class, "city", parameter,
                new IllegalArgumentException("nope"));

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("city");
    }

    @Test
    void unreadableBody_returns400() {
        ResponseEntity<ApiError> response = handler.handleUnreadableBody(mock(HttpMessageNotReadableException.class));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void methodNotSupported_returns405NamingTheMethod() {
        ResponseEntity<ApiError> response =
                handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PUT"));

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getBody().getMessage()).contains("PUT");
    }

    @Test
    void maxUpload_returns413() {
        ResponseEntity<ApiError> response = handler.handleMaxUpload(new MaxUploadSizeExceededException(5L));

        assertThat(response.getStatusCode().value()).isEqualTo(413);
    }

    @Test
    void noResourceFound_returns404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(mock(NoResourceFoundException.class));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    // Target for the MethodParameter used to build validation/type-mismatch exceptions.
    @SuppressWarnings("unused")
    private void sampleMethod(String s) {
    }

    private static class Sample {
        @NotBlank
        private final String value = "";
    }
}
