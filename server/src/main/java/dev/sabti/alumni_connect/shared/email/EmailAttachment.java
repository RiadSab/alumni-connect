package dev.sabti.alumni_connect.shared.email;

// One file on an outgoing email. Only calendar invites use this today.
public record EmailAttachment(String filename, String contentType, byte[] content) {
}
