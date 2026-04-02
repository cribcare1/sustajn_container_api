package com.payment.repository;

import com.payment.constants.PaymentStatus;
import com.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<Payment> findByOrderId(Integer orderId);

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Integer orderId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findByStatusAndNextRetryAtBefore(
            PaymentStatus status,
            LocalDateTime time
    );

}
