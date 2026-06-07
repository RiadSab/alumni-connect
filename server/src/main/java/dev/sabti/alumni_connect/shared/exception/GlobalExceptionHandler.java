package dev.sabti.alumni_connect.shared.exception;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.*;


@ControllerAdvice

public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class) // This annotation is used to define the class of Exception we want to handle
        // Here we handle validation exceptions, which are thrown when @Valid fails with RequestBody
        // RequestParam, PathVariable throwing ConstraintViolationException

    ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex){
        // linkedHashMap is used to preserve the order of insertion of errors
        Map<String, List<String>> errors = new LinkedHashMap<>();

        // We can have multiple errors for a single field, so we use a list of strings to store error messages
        // BindingResult contains field errors and global errors
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.computeIfAbsent(error.getField(), key -> new ArrayList<>()).add(error.getDefaultMessage());
        });

        // All global errors are added under the key "_global"
        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            errors.computeIfAbsent("_global", key -> new ArrayList<>()).add(error.getDefaultMessage());
        });

        Map<String, Object> responseBody = new LinkedHashMap<>();

        responseBody.put("timestamp", Instant.now());
        responseBody.put("status", HttpStatus.BAD_REQUEST.value());
        responseBody.put("errors", errors);
        return ResponseEntity.badRequest().body(responseBody);
    }
}