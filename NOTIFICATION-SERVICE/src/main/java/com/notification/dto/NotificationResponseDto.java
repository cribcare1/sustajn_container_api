package com.notification.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private Long id;
    private String message;
    private String notificationType;
    private Long senderId;
    private Long receiverId;
    private String timestamp;
    private Boolean isRead;           // true = READ, false = UNREAD
    private String approvalStatus;
    private Long orderId;
    private String status;
}