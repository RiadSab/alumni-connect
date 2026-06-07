package dev.sabti.alumni_connect.auth.register.validation;

import dev.sabti.alumni_connect.auth.register.RegisterCandidateDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class StudentIdRequiredValidator implements ConstraintValidator<StudentIdRequired, RegisterCandidateDTO> {

    @Override
    public boolean isValid(RegisterCandidateDTO dto, ConstraintValidatorContext context) {
        if(dto == null){
            return true; // Let @NotNull handle null case if needed
        }
        Boolean isStudent = dto.getIsStudent();
        String studentId = dto.getStudentId();

        if(Boolean.TRUE.equals(isStudent)){
            boolean valid = studentId != null && !studentId.trim().isEmpty();
            if(!valid){
                context.disableDefaultConstraintViolation(); // empêche le message par défaut
                context.buildConstraintViolationWithTemplate("Student ID is required for students")
                        .addPropertyNode("studentId") // sans cette ligne, l'erreur serait globale
                        .addConstraintViolation(); // ajoute la violation personnalisée au contexte
            }
            return valid;
        }
        return true;
    }
}
