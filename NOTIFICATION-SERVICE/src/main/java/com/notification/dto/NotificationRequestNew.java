package com.notification.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequestNew {
    private List<String> deviceTokens; // List of device tokens
    private String title; // Notification title
    private String body; // Notification body
    private Map<String, String> data; // Additional data to send

}
