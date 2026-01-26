package com.sustajn.oderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    @Column(nullable = false)
    private Long orderId;
    private Long borrowOrderId;
    private Long productId;

    private String type;
    // BEFORE_3_DAYS, OVERDUE, EXTENDED_BEFORE_3_DAYS

    private String message;

    private LocalDateTime sentDate;

    private Boolean isRead = false;
}
