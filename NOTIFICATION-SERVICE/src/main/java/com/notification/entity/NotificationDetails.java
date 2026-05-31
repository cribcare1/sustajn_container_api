package com.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notification_details")
public class NotificationDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String message;
    private String notificationType;
    private Long senderId;
    private Long receiverId;
    private String timestamp;
    private Boolean isRead;
    private String approvalStatus;
    private Long orderId;
    private String status;
}
