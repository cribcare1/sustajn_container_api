package com.payment.service.impl;

import com.payment.configuration.StripeConfig;
import com.payment.constants.PaymentStatus;
import com.payment.entity.Payment;
import com.payment.feiginservice.OrderServiceClient;
import com.payment.repository.PaymentRepository;
import com.payment.request.CreateExtensionPaymentRequest;
import com.payment.request.SoldRequest;
import com.payment.response.ApiResponse;
import com.payment.response.BorrowResponse;
import com.payment.response.PaymentResponse;
import com.payment.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final StripeConfig stripeConfig;
    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeConfig.getSecretKey();
    }

    /**
     * Step 1: Create a Stripe Checkout Session for extension payment.
     * Saves a PENDING payment record and returns the Stripe-hosted checkout URL.
     */

    // change table name to payment
    //extra column :- amount, paymentreason(extend, sold)
    // remark (failuar case)
    // payment type credit or debite
//    @Override
//    @Transactional
//    public ApiResponse<PaymentResponse> createCheckoutSession(
//            CreateExtensionPaymentRequest request) {
//
//        // ❌ Prevent duplicate pending payments for the same order
//        boolean alreadyPending = paymentRepository.existsByOrderIdAndStatus(
//                request.getOrderId(), PaymentStatus.PENDING);
//
//        if (alreadyPending) {
//            return new ApiResponse<>("error",
//                    "A pending payment already exists for this order. " +
//                            "Please complete or cancel it before creating a new one.");
//        }
//
//        try {
//            // ✅ Build Stripe Checkout Session
//            SessionCreateParams params = SessionCreateParams.builder()
//                    .setMode(SessionCreateParams.Mode.PAYMENT)
//                    .setSuccessUrl("http://localhost:8086/payment-success.html" + "?session_id={CHECKOUT_SESSION_ID}")
//                    .setCancelUrl("http://localhost:8086/payment-cancel.html")
//                    .setClientReferenceId(String.valueOf(request.getOrderId())) // ✅ important
//                    .addLineItem(
//                            SessionCreateParams.LineItem.builder()
//                                    .setQuantity(1L)
//                                    .setPriceData(
//                                            SessionCreateParams.LineItem.PriceData.builder()
//                                                    .setCurrency(stripeConfig.getCurrency())
//                                                    .setUnitAmount(stripeConfig.getExtensionPrice())
//                                                    .setProductData(
//                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
//                                                                    .setName("Borrow Extension — 5 Days")
//                                                                    .setDescription("Extends your borrow return date by 5 days for Order #" + request.getOrderId())
//                                                                    .build()
//                                                    )
//                                                    .build()
//                                    )
//                                    .build()
//                    )
//                    // Attach metadata so we can retrieve orderId & userId in webhook
//                    .putMetadata("orderId", String.valueOf(request.getOrderId()))
//                    .putMetadata("userId", String.valueOf(request.getUserId()))
//                    .build();
//
//            Session session = Session.create(params);
//
//            // ✅ Persist PENDING payment record
//
//            Payment payment = Payment.builder()
//                    .orderId(request.getOrderId())
//                    .userId(request.getUserId())
//                    .amount(stripeConfig.getExtensionPrice())
//                    .paymentReason("Extend")
//                    .stripeSessionId(session.getId())
//                    .status(PaymentStatus.PENDING)
//                    .createdAt(LocalDateTime.now())
//                    .updatedAt(LocalDateTime.now())
//                    .build();
//
//            paymentRepository.save(payment);
//
//            // ✅ Return checkout URL to the client
//            PaymentResponse response = new PaymentResponse();
//            response.setCheckoutUrl(session.getUrl());
//            response.setSessionId(session.getId());
//
//            return new ApiResponse<>("success",
//                    "Checkout session created. Redirect user to the checkout URL.", response);
//
//        } catch (StripeException e) {
//            log.error("Stripe error while creating checkout session: {}", e.getMessage());
//            return new ApiResponse<>("error", "Failed to create payment session: " + e.getMessage());
//        }
//    }


    @Override
    @Transactional
    public ApiResponse<PaymentResponse> createCheckoutSession(CreateExtensionPaymentRequest request) {

        boolean alreadyPending = paymentRepository.existsByOrderIdAndStatus(
                request.getOrderId(), PaymentStatus.PENDING);

        if (alreadyPending) {
            return new ApiResponse<>("error",
                    "A pending payment already exists for this order. Please complete or cancel it.");
        }
        boolean alreadySuccess = paymentRepository.existsByOrderIdAndStatus(
                request.getOrderId(), PaymentStatus.SUCCESS);

        if (alreadySuccess) {
            return new ApiResponse<>("error", "All ready payment got success for this order.");
        }

        try {

            List<BorrowResponse> borrowedResponse =orderServiceClient.getBorrowedDetailsByOrderId(request.getOrderId()).getData();

            int totalPendingContainerCount = borrowedResponse.stream()
                    .mapToInt(b -> b.getQuantity() - b.getReturnedQuantity())
                    .sum();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://13.232.106.224:8086/payment-success.html?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://13.232.106.224:8086/payment-cancel.html")
                    .setClientReferenceId(String.valueOf(request.getOrderId()))

                    // ✅ IMPORTANT: attach metadata to PaymentIntent
                    .setCustomerCreation(SessionCreateParams.CustomerCreation.ALWAYS)

                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .setSetupFutureUsage(
                                            SessionCreateParams.PaymentIntentData.SetupFutureUsage.OFF_SESSION
                                    )
                                    .putMetadata("orderId", String.valueOf(request.getOrderId()))
                                    .putMetadata("userId", String.valueOf(request.getUserId()))
                                    .build()
                    )
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(stripeConfig.getCurrency())
                                                    .setUnitAmount(totalPendingContainerCount * stripeConfig.getExtensionPrice())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Borrow Extension — 5 Days")
                                                                    .setDescription("Extend return date for Order #" + request.getOrderId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);



            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .amount(totalPendingContainerCount * stripeConfig.getExtensionPrice())
                    .paymentReason("Extend")
                    .stripeSessionId(session.getId())
                    .status(PaymentStatus.PENDING)
                    .stripeCustomerId(session.getCustomer())
                    .stripePaymentMethodId(null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);

            PaymentResponse response = new PaymentResponse();
            response.setCheckoutUrl(session.getUrl());
            response.setSessionId(session.getId());

            return new ApiResponse<>("success",
                    "Checkout session created", response);

        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage());
            return new ApiResponse<>("error", "Failed to create session");
        }
    }

    /**
     * Step 2 (called by Webhook): Mark payment SUCCESS and trigger order extension.
     */
    @Override
    @Transactional
    public void handlePaymentSuccess(Integer orderId,
                                     String paymentIntentId,
                                     String customerId,
                                     String paymentMethodId) {

        Payment payment = paymentRepository
                .findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found for orderId: " + orderId));

        // ✅ Idempotency (very important for webhook retries)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Payment already processed for order {}", orderId);
            return;
        }

        // ✅ Update all required fields
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setStripeCustomerId(customerId);          // ⭐ REQUIRED for auto-pay
        payment.setStripePaymentMethodId(paymentMethodId); // ⭐ REQUIRED for auto-pay
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        try {
            orderServiceClient.extendOrder(Long.valueOf(orderId));
            log.info("Order {} extended successfully", orderId);

        } catch (Exception e) {
            log.error("Order extension failed for order {}", orderId);

            // ❗ DO NOT rollback payment
            throw new RuntimeException("Order extension failed");
        }
    }

    /**
     * Step 3 (called by Webhook): Mark payment FAILED.
     */
//    @Override
//    @Transactional
//    public void handlePaymentFailure(String stripeSessionId) {
//
//        paymentRepository.findByStripeSessionId(stripeSessionId).ifPresent(payment -> {
//            payment.setStatus(PaymentStatus.FAILED);
//            paymentRepository.save(payment);
//            log.warn("Payment FAILED for session: {}, orderId: {}",
//                    stripeSessionId, payment.getOrderId());
//        });
//    }

    @Override
    @Transactional
    public void handlePaymentFailure(String stripeSessionId,
                                     String paymentIntentId,
                                     String failureReason) {

        paymentRepository.findByStripeSessionId(stripeSessionId)
                .ifPresent(payment -> {

                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setStripePaymentIntentId(paymentIntentId);
                    payment.setFailureReason(failureReason);
                    payment.setUpdatedAt(LocalDateTime.now());

                    paymentRepository.save(payment);

                    log.warn("❌ Payment FAILED for session: {}, orderId: {}, reason: {}",
                            stripeSessionId,
                            payment.getOrderId(),
                            failureReason);
                });
    }


    @Override
    @Transactional
    public void handlePaymentFailureByOrderId(Integer orderId,
                                              String paymentIntentId,
                                              String reason) {

        paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .ifPresent(payment -> {

                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setStripePaymentIntentId(paymentIntentId);
                    payment.setFailureReason(reason);
                    payment.setUpdatedAt(LocalDateTime.now());

                    paymentRepository.save(payment);

                    log.warn("Payment FAILED orderId={} reason={}", orderId, reason);
                });
    }


//    @Override
//    @Transactional
//    public void autoPay(Long orderId, Long userId, int pendingQty, Long unitPrice) {
//
//        Long amount = pendingQty * unitPrice;
//
//        Payment lastPayment = paymentRepository
//                .findTopByOrderIdOrderByCreatedAtDesc(orderId)
//                .orElseThrow(() -> new RuntimeException("No previous payment found"));
//
//        // ❗ Safety check
//        if (lastPayment.getStripeCustomerId() == null ||
//                lastPayment.getStripePaymentMethodId() == null) {
//
//            log.error("No saved card for order {}", orderId);
//            return;
//        }
//
//        try {
//
//            PaymentIntentCreateParams params =
//                    PaymentIntentCreateParams.builder()
//                            .setAmount(amount)
//                            .setCurrency("usd")
//                            .setCustomer(lastPayment.getStripeCustomerId())
//                            .setPaymentMethod(lastPayment.getStripePaymentMethodId())
//                            .setOffSession(true)
//                            .setConfirm(true)
//                            .putMetadata("orderId", String.valueOf(orderId))
//                            .build();
//
//            PaymentIntent intent = PaymentIntent.create(params);
//
//            if ("succeeded".equals(intent.getStatus())) {
//
//                Payment payment = paymentRepository.save(
//                        Payment.builder()
//                                .orderId(orderId)
//                                .userId(userId)
//                                .amount(amount)
//                                .status(PaymentStatus.SUCCESS)
//                                .stripePaymentIntentId(intent.getId())
//                                .stripeCustomerId(lastPayment.getStripeCustomerId())
//                                .stripePaymentMethodId(lastPayment.getStripePaymentMethodId())
//                                .paymentReason("AUTO_SOLD")
//                                .paidAt(LocalDateTime.now())
//                                .createdAt(LocalDateTime.now())
//                                .updatedAt(LocalDateTime.now())
//                                .build()
//                );
//
//                // 🔥 CALL ORDER SERVICE
//                orderServiceClient.markOrderAsSold(
//                        new SoldRequest(orderId, payment.getId(), intent.getId())
//                );
//
//            }
//
//        } catch (Exception e) {
//
//            String error = e.getMessage();
//            if (error != null && error.length() > 500) {
//                error = error.substring(0, 500);
//            }
//
//            paymentRepository.save(
//                    Payment.builder()
//                            .orderId(orderId)
//                            .userId(userId)
//                            .amount(amount)
//                            .status(PaymentStatus.FAILED)
//                            .stripeCustomerId(lastPayment.getStripeCustomerId())
//                            .stripePaymentMethodId(lastPayment.getStripePaymentMethodId())
//                            .paymentReason("AUTO_SOLD")
//                            .failureReason(error)
//                            .retryCount(0)
//                            .nextRetryAt(LocalDateTime.now().plusMinutes(5))
//                            .createdAt(LocalDateTime.now())
//                            .updatedAt(LocalDateTime.now())
//                            .build()
//            );
//
//            log.error("AutoPay failed for order {}", orderId, e);
//        }
//    }


    @Override
    @Transactional
    public void autoPay(Long orderId, Long userId, int pendingQty, Long unitPrice) {

        Long amount = pendingQty * unitPrice;

        Payment lastPayment = paymentRepository
                .findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new RuntimeException("No previous payment found"));

        if (lastPayment.getStripeCustomerId() == null ||
                lastPayment.getStripePaymentMethodId() == null) {
            log.error("No saved card for order {}", orderId);
            return;
        }

        Payment savedPayment = null;

        // ── Stripe block ──────────────────────────────────────────────
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency("usd")
                    .setCustomer(lastPayment.getStripeCustomerId())
                    .setPaymentMethod(lastPayment.getStripePaymentMethodId())
                    .setOffSession(true)
                    .setConfirm(true)
                    .putMetadata("orderId", String.valueOf(orderId))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            if ("succeeded".equals(intent.getStatus())) {
                savedPayment = paymentRepository.save(
                        Payment.builder()
                                .orderId(orderId)
                                .userId(userId)
                                .amount(amount)
                                .status(PaymentStatus.SUCCESS)
                                .stripePaymentIntentId(intent.getId())
                                .stripeCustomerId(lastPayment.getStripeCustomerId())
                                .stripePaymentMethodId(lastPayment.getStripePaymentMethodId())
                                .paymentReason("AUTO_SOLD")
                                .paidAt(LocalDateTime.now())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                );
            }

        } catch (Exception e) {
            String error = e.getMessage();
            if (error != null && error.length() > 500) error = error.substring(0, 500);

            paymentRepository.save(
                    Payment.builder()
                            .orderId(orderId)
                            .userId(userId)
                            .amount(amount)
                            .status(PaymentStatus.FAILED)
                            .stripeCustomerId(lastPayment.getStripeCustomerId())
                            .stripePaymentMethodId(lastPayment.getStripePaymentMethodId())
                            .paymentReason("AUTO_SOLD")
                            .failureReason(error)
                            .retryCount(0)
                            .nextRetryAt(LocalDateTime.now().plusMinutes(5))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build()
            );

            log.error("Stripe charge failed for order {}", orderId, e);
            return;
        }

        // ── Order mark-as-sold block (separate, so Stripe errors don't mask this) ──
        if (savedPayment != null) {
            try {
                orderServiceClient.markOrderAsSold(
                        new SoldRequest(orderId, savedPayment.getId(),
                                savedPayment.getStripePaymentIntentId())
                );
                log.info("markOrderAsSold called successfully for order {}", orderId);
            } catch (Exception e) {
                // Payment succeeded but order update failed — needs alerting/retry
                log.error("CRITICAL: Payment succeeded but markOrderAsSold failed " +
                        "for order {}. Manual intervention required.", orderId, e);
            }
        }
    }


    @Transactional
    public void retryAutoPayments() {

        List<Payment> failedPayments =
                paymentRepository.findByStatusAndNextRetryAtBefore(
                        PaymentStatus.FAILED,
                        LocalDateTime.now()
                );

        for (Payment payment : failedPayments) {

            if (payment.getRetryCount() >= 3) continue;

            try {

                PaymentIntentCreateParams params =
                        PaymentIntentCreateParams.builder()
                                .setAmount(payment.getAmount())
                                .setCurrency("usd")
                                .setCustomer(payment.getStripeCustomerId())
                                .setPaymentMethod(payment.getStripePaymentMethodId())
                                .setOffSession(true)
                                .setConfirm(true)
                                .build();

                PaymentIntent intent = PaymentIntent.create(params);

                if ("succeeded".equals(intent.getStatus())) {

                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setStripePaymentIntentId(intent.getId());
                    payment.setPaidAt(LocalDateTime.now());

                    orderServiceClient.markOrderAsSold(
                            new SoldRequest(
                                    payment.getOrderId(),
                                    payment.getId(),
                                    intent.getId()
                            )
                    );

                }

            } catch (Exception e) {

                payment.setRetryCount(payment.getRetryCount() + 1);

                payment.setNextRetryAt(
                        LocalDateTime.now().plusMinutes(5 * payment.getRetryCount())
                );
            }

            paymentRepository.save(payment);
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void retryPaymentsScheduler() {
        retryAutoPayments();
    }
}