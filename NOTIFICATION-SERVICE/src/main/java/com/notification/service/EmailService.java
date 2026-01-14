package com.notification.service;

import com.notification.dto.EmailRequest; // Make sure to import your DTO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    @Async
    public void sendEmail(EmailRequest request) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(request.getTo());

        if (fromAddress != null && !fromAddress.isBlank()) {
            msg.setFrom(fromAddress);
        }

        String type = request.getNotificationType();

        // 1. SIGNUP Logic
        if ("SIGNUP".equalsIgnoreCase(type)) {
            msg.setSubject("Welcome to Our App!");
            msg.setText("Hello,\n\n" +
                    "Welcome to the application! We are excited to have you on This App.\n" +
                    "Your registration was successful.");
        }
        // 2. RESET Logic
        else if ("RESET".equalsIgnoreCase(type)) {
            msg.setSubject("Password Reset Request");
            msg.setText("Hello,\n\n" +
                    "You requested a password reset. Use the following token/message:\n\n" +
                    request.getMessage() + "\n\n" + // Uses the 'message' field for token
                    "This expires in 2 minutes. If you did not request this, please ignore this email.");
        }
        // 3. FALLBACK (Generic)
        else {
            msg.setSubject(request.getSubject() != null ? request.getSubject() : "Notification");
            msg.setText(request.getMessage());
        }

        mailSender.send(msg);
    }


    // --- EXISTING METHOD (Keep it if other internal services still use it) ---
    @Async
    public void sendTokenEmail(String to, String token) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        if (fromAddress != null && !fromAddress.isBlank()) msg.setFrom(fromAddress);
        msg.setSubject("Your password reset token");
        msg.setText("Use the following token to reset your password. It expires in 2 minutes:\n\n" + token + "\n\nIf you did not request this, please ignore this email.");
        mailSender.send(msg);
    }
}