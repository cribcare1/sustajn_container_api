package com.notification.controller;

import com.notification.dto.*;
import com.notification.entity.AccountStatus;
import com.notification.entity.DeviceToken;
import com.notification.feignService.AuthServiceClient;
import com.notification.service.DeviceTokenService;
import com.notification.service.EmailService;
import com.notification.service.PushNotificationService;
import com.notification.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final TokenService tokenService;
    private final EmailService emailService;
    private final PushNotificationService notificationService;
    private final DeviceTokenService deviceTokenService;
    private final AuthServiceClient authServiceClient;

    public NotificationController(TokenService tokenService, EmailService emailService, PushNotificationService notificationService, DeviceTokenService deviceTokenService, AuthServiceClient authServiceClient) {
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.notificationService=notificationService;
        this.deviceTokenService=deviceTokenService;
        this.authServiceClient = authServiceClient;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotRequest request) {

        // input validation is handled inside services and exceptions are mapped by GlobalExceptionHandler
        if(request.getType().equalsIgnoreCase("reset")) {
            UserResponse response = authServiceClient.getUserByEmail(request.getEmail());
            if (response == null) {
                return ResponseEntity.ok(new ApiResponse("error", "User with given email does not exist"));
            }
            if (response.getAccountStatus() == AccountStatus.rejected) {
                return ResponseEntity.ok(new ApiResponse("error", "Your account has been rejected. Please contact support."));
            }
        }

        if(request.getType()!=null && request.getType().equalsIgnoreCase("signup")) {
            UserResponse response = authServiceClient.getUserByEmail(request.getEmail());
            if (response != null) {
                if( response.getAccountStatus()== AccountStatus.rejected){
                    return ResponseEntity.ok(new ApiResponse("error", "Your account has been rejected. Please contact support."));
                }
                return ResponseEntity.ok(new ApiResponse("error", "User with given email already exists"));
            }
        }



        String token = tokenService.generateToken(request.getEmail());
        // allow MailException or other exceptions to propagate to GlobalExceptionHandler
        emailService.sendTokenEmail(request.getEmail(), token, request.getType());
        return ResponseEntity.ok(Map.of("message","token sent to email (expires in 2 minutes)","status","success"));
    }

    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestBody VerifyRequest request) {
        // tokenService.verifyToken will throw ApiException on invalid/expired token
        tokenService.verifyToken(request.getEmail(), request.getToken());
        // TODO: perform password update in your user store using request.getNewPassword()
        return ResponseEntity.ok(Map.of("message","token verified. You may reset the password now.","status","success"));

    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(
            @RequestBody NotificationRequestNew request
    ) {
        String result = notificationService.sendNotificationToMultipleDevices(request);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/registerOrUpdateDeviceToken")
    public ResponseEntity<DeviceToken> registerOrUpdateToken(
            DeviceTokenRequest deviceTokenRequest
    ) {
        DeviceToken saved = deviceTokenService.upsertUserDeviceToken(deviceTokenRequest.getUserId(),deviceTokenRequest.getDeviceToken(), deviceTokenRequest.getDeviceType());
        return ResponseEntity.ok(saved);
    }

}
