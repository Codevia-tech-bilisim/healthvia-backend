package com.healthvia.platform.contact;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactEmailService {

    private final JavaMailSender mailSender;

    @Value("${contact.recipient-email:info@codevia.tech}")
    private String recipientEmail;

    @Value("${spring.mail.username:noreply@healthvia.com}")
    private String fromEmail;

    public void sendContactEmail(ContactRequest request) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(recipientEmail);
            mail.setFrom(fromEmail);
            mail.setReplyTo(request.getEmail());
            mail.setSubject("HealthVia Contact Form: " + request.getFullName());
            mail.setText(
                "New contact form submission\n" +
                "─────────────────────────\n\n" +
                "Name:  " + request.getFullName() + "\n" +
                "Email: " + request.getEmail() + "\n\n" +
                "Message:\n" + request.getMessage() + "\n"
            );

            mailSender.send(mail);
            log.info("Contact email sent to {} from {}", recipientEmail, request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send contact email from {}: {}", request.getEmail(), e.getMessage(), e);
        }
    }
}
