package dev.sabti.alumni_connect.alumni;

// sent = claim emails handed to the mail sender; skipped = rows we couldn't email.
public record ClaimInviteResultDTO(int sent, int skipped) {
}
