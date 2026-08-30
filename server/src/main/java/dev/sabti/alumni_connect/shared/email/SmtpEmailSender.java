package dev.sabti.alumni_connect.shared.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

// Real sender over SMTP (configured via spring.mail.* — Resend by default). Active when
// app.mail.provider=smtp, which switches off LoggingEmailSender.
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;

    // Must be an address on the verified domain (e.g. noreply@yourdomain.com).
    @Value("${app.mail.from}")
    private String from;

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    // An attachment needs a multipart MimeMessage; SimpleMailMessage can only carry text.
    @Override
    public void send(String to, String subject, String body, EmailAttachment attachment) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(attachment.filename(),
                    new ByteArrayResource(attachment.content()), attachment.contentType());
        } catch (Exception e) {
            throw new MailPreparationException("Could not build the message for " + to, e);
        }
        mailSender.send(message);
    }
}
