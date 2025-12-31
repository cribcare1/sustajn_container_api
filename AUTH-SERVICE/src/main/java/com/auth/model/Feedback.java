package com.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RENAMED from 'sender' to 'customer'
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    private Long restaurantId;

    private String rating;
    private String subject;

    @Column(length = 1000)
    private String remark;

    private LocalDateTime createdAt;
}