package com.notification.service;

import com.notification.dto.UserResponse;
import com.notification.feignService.AuthServiceClient;
import lombok.RequiredArgsConstructor;
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
    public void sendTokenEmail(String to, String token, String type) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);

        if ("RESET".equalsIgnoreCase(type)) {
         //User is present means We have to sent the Email .
            if (fromAddress != null && !fromAddress.isBlank()) msg.setFrom(fromAddress);
            msg.setSubject("Your password reset token");
            msg.setText("Use the following token to reset your password. It expires in 2 minutes:\n\n" + token + "\n\nIf you did not request this, please ignore this email.");
            mailSender.send(msg);
        } else if ("SIGNUP".equalsIgnoreCase(type)) {

            msg.setSubject("Sustajn Sign Up Verification");
            msg.setText("Welcome to Sustajn! Your sign up verification code is :\n\n" +
                    token);


            mailSender.send(msg);
        }

    }
    }
