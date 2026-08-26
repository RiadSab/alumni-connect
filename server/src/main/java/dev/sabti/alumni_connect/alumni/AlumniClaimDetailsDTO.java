package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.Fields;

// What the claim page shows before the person sets a password: the school's own facts about them.
public record AlumniClaimDetailsDTO(String firstName, String lastName, Integer promotionYear,
                                    Fields fieldOfStudy, String email) {
}
