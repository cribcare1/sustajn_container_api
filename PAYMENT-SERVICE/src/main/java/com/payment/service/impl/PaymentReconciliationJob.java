package com.payment.service.impl;

import com.payment.constants.PaymentStatus;
import com.payment.entity.Payment;
import com.payment.repository.PaymentRepository;
import com.stripe.model.PaymentIntent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void reconcilePayments() {

        List<Payment> pendingPayments =
                paymentRepository.findByStatus(PaymentStatus.PENDING);

        for (Payment payment : pendingPayments) {

            try {

                PaymentIntent intent =
                        PaymentIntent.retrieve(payment.getStripePaymentIntentId());

                if ("succeeded".equals(intent.getStatus())) {

                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(LocalDateTime.now());

                    paymentRepository.save(payment);

                    log.info("Recovered payment {}", payment.getId());
                }

            } catch (Exception e) {

                log.error("Reconciliation failed {}", payment.getId());
            }
        }
    }
}