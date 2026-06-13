package dev.sabti.alumni_connect.company;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.company.entities.CompanySize;
import jakarta.validation.constraints.Size;
import lombok.Data;

// All fields nullable: null means "leave unchanged", same pattern as UpdateCandidateProfileDTO.
// Identity/admin fields are deliberately not here: email is unique (a collision would surface as a
// 500 until the exception-handling pass), and status/statusChangeReason are admin-controlled, not
// self-edits. logoId/videoPresentationId are separate file-upload flows.
@Data
public class UpdateCompanyDTO {
    private String name;
    private String phone;
    private Fields field;

    // Matches the description column length, so an over-long value is a clean 400 here rather than a
    // 500 from the database.
    @Size(max = 2000)
    private String description;

    private String website;
    private String address;
    private CompanySize size;
}
