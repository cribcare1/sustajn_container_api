package com.sustajn.oderservice.repository;

import com.sustajn.oderservice.constant.NotificationStatus;
import com.sustajn.oderservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByBorrowOrderIdAndTypeAndSentDate(
            Long borrowOrderId,
            String type,
            LocalDateTime sentDate
    );

    boolean existsByOrderIdAndTypeAndSentDate(Long orderId, NotificationStatus type, LocalDateTime todayStart);
}
