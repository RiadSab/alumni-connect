package dev.sabti.alumni_connect.shared.email;

public interface EmailSender {
    void send(String to, String subject, String body);

    // Same email with a file attached. Defaulted so the plain-text senders and every existing
    // caller stay untouched.
    default void send(String to, String subject, String body, EmailAttachment attachment) {
        send(to, subject, body);
    }
}
