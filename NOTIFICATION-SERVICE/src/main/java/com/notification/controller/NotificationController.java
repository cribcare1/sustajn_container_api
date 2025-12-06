package com.notification.controller;

import com.notification.dto.ForgotRequest;
import com.notification.dto.VerifyRequest;
import com.notification.service.EmailService;
import com.notification.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final TokenService tokenService;
    private final EmailService emailService;

    @Autowired
    public NotificationController(TokenService tokenService, EmailService emailService) {
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotRequest request) {
        // input validation is handled inside services and exceptions are mapped by GlobalExceptionHandler
        String token = tokenService.generateToken(request.getEmail());
        // allow MailException or other exceptions to propagate to GlobalExceptionHandler
        emailService.sendTokenEmail(request.getEmail(), token);
        return ResponseEntity.ok(Map.of("message","token sent to email (expires in 2 minutes)","status","success"));
    }

    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestBody VerifyRequest request) {
        // tokenService.verifyToken will throw ApiException on invalid/expired token
        tokenService.verifyToken(request.getEmail(), request.getToken());
        // TODO: perform password update in your user store using request.getNewPassword()
        return ResponseEntity.ok(Map.of("message","token verified. You may reset the password now.","status","success"));

    }
}
