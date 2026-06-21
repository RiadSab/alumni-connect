package dev.sabti.alumni_connect.shared.email;

public interface EmailSender {
    void send(String to, String subject, String body);
}
