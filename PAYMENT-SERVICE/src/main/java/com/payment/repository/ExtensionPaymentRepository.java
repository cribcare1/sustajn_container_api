package com.payment.repository;

import com.payment.constants.PaymentStatus;
import com.payment.entity.ExtensionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExtensionPaymentRepository extends JpaRepository<ExtensionPayment, Long> {

    Optional<ExtensionPayment> findByStripeSessionId(String stripeSessionId);

    Optional<ExtensionPayment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

}
