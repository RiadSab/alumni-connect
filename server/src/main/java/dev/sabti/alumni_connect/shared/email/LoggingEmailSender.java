package dev.sabti.alumni_connect.shared.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Dev/default sender: logs the email instead of sending it (read reset codes from the console).
// Active unless app.mail.provider is set to a real provider.
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String body) {
        log.info("[email] to={} subject=\"{}\"\n{}", to, subject, body);
    }
}
