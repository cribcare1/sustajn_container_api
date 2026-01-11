package com.notification.service;

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
    public void sendTokenEmail(String to, String token) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        if (fromAddress != null && !fromAddress.isBlank()) msg.setFrom(fromAddress);
        msg.setSubject("Your password reset token");
        msg.setText("Use the following token to reset your password. It expires in 2 minutes:\n\n" + token + "\n\nIf you did not request this, please ignore this email.");
        mailSender.send(msg);
    }
}
