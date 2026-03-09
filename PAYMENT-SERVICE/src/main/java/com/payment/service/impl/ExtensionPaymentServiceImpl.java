package com.payment.service.impl;

import com.payment.configuration.StripeConfig;
import com.payment.constants.PaymentStatus;
import com.payment.entity.ExtensionPayment;
import com.payment.feiginservice.OrderServiceClient;
import com.payment.repository.ExtensionPaymentRepository;
import com.payment.request.CreateExtensionPaymentRequest;
import com.payment.response.ApiResponse;
import com.payment.response.ExtensionPaymentResponse;
import com.payment.service.ExtensionPaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtensionPaymentServiceImpl implements ExtensionPaymentService {

    private final StripeConfig stripeConfig;
    private final ExtensionPaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeConfig.getSecretKey();
    }

    /**
     * Step 1: Create a Stripe Checkout Session for extension payment.
     * Saves a PENDING payment record and returns the Stripe-hosted checkout URL.
     */
    @Override
    @Transactional
    public ApiResponse<ExtensionPaymentResponse> createCheckoutSession(
            CreateExtensionPaymentRequest request) {

        // ❌ Prevent duplicate pending payments for the same order
        boolean alreadyPending = paymentRepository.existsByOrderIdAndStatus(
                request.getOrderId(), PaymentStatus.PENDING);

        if (alreadyPending) {
            return new ApiResponse<>("error",
                    "A pending payment already exists for this order. " +
                            "Please complete or cancel it before creating a new one.");
        }

        try {
            // ✅ Build Stripe Checkout Session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:8086/payment-success.html" + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:8086/payment-cancel.html")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(stripeConfig.getCurrency())
                                                    .setUnitAmount(stripeConfig.getExtensionPrice())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Borrow Extension — 5 Days")
                                                                    .setDescription("Extends your borrow return date by 5 days for Order #" + request.getOrderId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    // Attach metadata so we can retrieve orderId & userId in webhook
                    .putMetadata("orderId", String.valueOf(request.getOrderId()))
                    .putMetadata("userId", String.valueOf(request.getUserId()))
                    .build();

            Session session = Session.create(params);

            // ✅ Persist PENDING payment record
            ExtensionPayment payment = ExtensionPayment.builder()
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .stripeSessionId(session.getId())
                    .status(PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);

            // ✅ Return checkout URL to the client
            ExtensionPaymentResponse response = new ExtensionPaymentResponse();
            response.setCheckoutUrl(session.getUrl());
            response.setSessionId(session.getId());

            return new ApiResponse<>("success",
                    "Checkout session created. Redirect user to the checkout URL.", response);

        } catch (StripeException e) {
            log.error("Stripe error while creating checkout session: {}", e.getMessage());
            return new ApiResponse<>("error", "Failed to create payment session: " + e.getMessage());
        }
    }

    /**
     * Step 2 (called by Webhook): Mark payment SUCCESS and trigger order extension.
     */
    @Override
    @Transactional
    public void handlePaymentSuccess(String stripeSessionId, String paymentIntentId) {

        ExtensionPayment payment = paymentRepository
                .findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new RuntimeException(
                        "No payment record found for session: " + stripeSessionId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Payment already marked SUCCESS for session: {}", stripeSessionId);
            return; // idempotent — Stripe may retry webhooks
        }

        // ✅ Update payment record
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // ✅ Call Order Service to extend borrow return date
        try {
            orderServiceClient.extendOrder(payment.getOrderId());
            log.info("Successfully extended order {} after payment {}",
                    payment.getOrderId(), stripeSessionId);
        } catch (Exception e) {
            log.error("Order extension failed for orderId={} after payment success. " +
                            "Manual intervention required. Error: {}",
                    payment.getOrderId(), e.getMessage());
            // Do NOT rollback payment — log for manual retry / dead-letter queue
            throw new RuntimeException("Order extension call failed: " + e.getMessage());
        }
    }

    /**
     * Step 3 (called by Webhook): Mark payment FAILED.
     */
    @Override
    @Transactional
    public void handlePaymentFailure(String stripeSessionId) {

        paymentRepository.findByStripeSessionId(stripeSessionId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment FAILED for session: {}, orderId: {}",
                    stripeSessionId, payment.getOrderId());
        });
    }
}