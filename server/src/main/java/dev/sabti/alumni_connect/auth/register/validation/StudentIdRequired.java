package dev.sabti.alumni_connect.auth.register.validation;


import jakarta.validation.Constraint;

import java.lang.annotation.*;

@Documented // To include this annotation in the Javadoc
@Constraint(validatedBy = StudentIdRequiredValidator.class) // Specify the validator class (logic of the annotation)
@Target(ElementType.TYPE) // This annotation can be applied to classes (DTOs), interfaces, or enums, not to fields or methods
@Retention(RetentionPolicy.RUNTIME) // The annotation will be available at runtime
public @interface StudentIdRequired {
    String message() default "studentId is required when isStudent is true";

    Class<?>[] groups() default {}; // Used for grouping constraints

    Class<? extends jakarta.validation.Payload>[] payload() default {}; // Used to carry metadata information
}
