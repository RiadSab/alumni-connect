package dev.sabti.alumni_connect.company.users;

import dev.sabti.alumni_connect.company.entities.CompanyUserPosition;
import lombok.Data;

// All fields nullable: null means "leave unchanged", same pattern as
// UpdateCandidateProfileDTO. companyRole and company are deliberately not here —
// those are admin/owner-controlled, not self-editable.
@Data
public class UpdateCompanyUserProfileDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private CompanyUserPosition position;
}
