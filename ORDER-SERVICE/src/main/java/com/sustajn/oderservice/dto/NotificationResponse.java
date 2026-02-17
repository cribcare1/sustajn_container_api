package com.sustajn.oderservice.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class NotificationResponse {

    private List<String> deviceTokens; // List of device tokens
    private String title; // Notification title
    private String body; // Notification body
    private Map<String, String> data; // Additional data to send

}
