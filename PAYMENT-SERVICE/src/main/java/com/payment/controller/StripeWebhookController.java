package com.payment.controller;

import com.payment.configuration.StripeConfig;
import com.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("payments/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeConfig stripeConfig;
    private final PaymentService paymentService;

    /**
     * POST ayments/webhook/stripe
     *
     * Stripe sends events here after payment.
     * ⚠️ This endpoint must be excluded from CSRF protection and Spring Security filters.
     * ⚠️ Raw request body must reach this method — do NOT use @RequestBody with a DTO.
     *
     * Register this URL in your Stripe Dashboard → Webhooks:
     *   https://yourdomain.com/api/payments/webhook/stripe
     *
     * Events to listen for:
     *   - checkout.session.completed  → payment succeeded
     *   - checkout.session.expired    → session expired without payment
     */
    //=====================================================================//
//    @PostMapping("/stripe")
//    public ResponseEntity<String> handleStripeWebhook(
//            @RequestBody String payload,
//            @RequestHeader("Stripe-Signature") String sigHeader) {
//
//        Event event;
//
//        // ✅ Verify webhook signature — prevents spoofed events
//        try {
//            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
//        } catch (SignatureVerificationException e) {
//            log.error("❌ Invalid Stripe webhook signature: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body("Invalid signature");
//        }
//
//        log.info("📩 Stripe webhook received: type={}, id={}", event.getType(), event.getId());
//
//        switch (event.getType()) {
//
//            case "checkout.session.completed" -> {
//                // Payment was successful
//                Session session = deserializeSession(event);
//                if (session == null) break;
//
//                String paymentIntentId = session.getPaymentIntent();
//                String sessionId = session.getId();
//
//                log.info("✅ Payment completed: sessionId={}, paymentIntentId={}",
//                        sessionId, paymentIntentId);
//
//                paymentService.handlePaymentSuccess(sessionId, paymentIntentId);
//            }
//
//            case "checkout.session.expired" -> {
//                // Customer didn't complete payment within session window (24h default)
//                Session session = deserializeSession(event);
//                if (session == null) break;
//
//                log.warn("⏰ Checkout session expired: sessionId={}", session.getId());
//                paymentService.handlePaymentFailure(session.getId(), );
//            }
//
//            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
//        }
//
//        // ✅ Always return 200 to Stripe (even on internal errors)
//        // Stripe retries on non-2xx for up to 72 hours
//        return ResponseEntity.ok("Webhook received");
//    }

    //============================================================================//

//    @PostMapping("/stripe")
//    public ResponseEntity<String> handleStripeWebhook(
//            @RequestBody String payload,
//            @RequestHeader("Stripe-Signature") String sigHeader) {
//
//        Event event;
//
//        try {
//            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
//        } catch (SignatureVerificationException e) {
//            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
//        }
//
//        log.info("Stripe webhook received: type={}, id={}", event.getType(), event.getId());
//
//        switch (event.getType()) {
//
//            // ✅ SUCCESS PAYMENT
//            case "checkout.session.completed" -> {
//
//                Session session = deserializeSession(event);
//                if (session == null) break;
//
//                Integer orderId = Integer.parseInt(session.getMetadata().get("orderId"));
//
//                paymentService.handlePaymentSuccess(
//                        orderId,
//                        session.getPaymentIntent()
//                );
//            }
//
//            // ❌ USER DID NOT COMPLETE PAYMENT
//            case "checkout.session.expired" -> {
//
//                Session session = deserializeSession(event);
//                if (session == null) break;
//
//                log.warn("Checkout session expired: {}", session.getId());
//
//                paymentService.handlePaymentFailure(
//                        session.getId(),
//                        session.getPaymentIntent(),
//                        "Checkout session expired"
//                );
//            }
//
//            // ❌ CARD FAILURE / INSUFFICIENT FUNDS / INVALID CARD
//            case "payment_intent.payment_failed" -> {
//
//                PaymentIntent paymentIntent =
//                        (PaymentIntent) event.getDataObjectDeserializer()
//                                .getObject()
//                                .orElse(null);
//
//                if (paymentIntent == null) break;
//
//                String reason =
//                        paymentIntent.getLastPaymentError() != null
//                                ? paymentIntent.getLastPaymentError().getMessage()
//                                : "Payment failed";
//
//                Integer orderId = Integer.valueOf(paymentIntent.getMetadata().get("orderId"));
//                System.err.println("orderId: " + orderId);
//
//                log.error("Payment failed: orderId={}, reason={}", orderId, reason);
//
//                paymentService.handlePaymentFailureByOrderId(
//                        orderId,
//                        paymentIntent.getId(),
//                        reason
//                );
//            }
//
//            // ❌ CHARGE FAILED
//            case "charge.failed" -> {
//
//                Charge charge = (Charge) event.getDataObjectDeserializer()
//                        .getObject()
//                        .orElse(null);
//
//                if (charge == null) break;
//
//                log.error("Charge failed: {} reason={}",
//                        charge.getId(),
//                        charge.getFailureMessage());
//            }
//
//            // ❌ PAYMENT CANCELED
//            case "payment_intent.canceled" -> {
//
//                PaymentIntent paymentIntent =
//                        (PaymentIntent) event.getDataObjectDeserializer()
//                                .getObject()
//                                .orElse(null);
//
//                if (paymentIntent == null) break;
//
//                log.warn("Payment canceled: {}", paymentIntent.getId());
//
//                paymentService.handlePaymentFailure(
//                        paymentIntent.getMetadata().get("sessionId"),
//                        paymentIntent.getId(),
//                        "Payment canceled"
//                );
//            }
//
//            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
//        }
//
//        return ResponseEntity.ok("Webhook received");
//    }



    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    stripeConfig.getWebhookSecret());

        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        log.info("Stripe event received: {}", event.getType());

        switch (event.getType()) {

            // ✅ PAYMENT SUCCESS
            case "checkout.session.completed" -> {

                Session session = deserializeSession(event);
                if (session == null) break;

                try {

                    // ✅ Get latest session
                    Session fullSession = Session.retrieve(session.getId());

                    String orderIdStr = fullSession.getClientReferenceId();

                    if (orderIdStr == null) {
                        log.error("Missing orderId in session");
                        break;
                    }

                    Integer orderId = Integer.valueOf(orderIdStr);

                    // ✅ Get PaymentIntent ID
                    String paymentIntentId = fullSession.getPaymentIntent();

                    // ✅ IMPORTANT FIX (no casting)
                    PaymentIntent paymentIntent =
                            PaymentIntent.retrieve(paymentIntentId);

                    String customerId = fullSession.getCustomer();
                    System.err.println("Customer ID: " + customerId);
                    String paymentMethodId = paymentIntent.getPaymentMethod();

                    // ✅ Call service
                    paymentService.handlePaymentSuccess(
                            orderId,
                            paymentIntentId,
                            customerId,
                            paymentMethodId
                    );

                } catch (Exception e) {
                    log.error("Error processing success webhook", e);
                }
            }

            // ❌ SESSION EXPIRED
            case "checkout.session.expired" -> {

                Session session = deserializeSession(event);
                if (session == null) break;

                paymentService.handlePaymentFailure(
                        session.getId(),
                        session.getPaymentIntent(),
                        "Checkout expired"
                );
            }

            // ❌ PAYMENT FAILED
            case "payment_intent.payment_failed" -> {

                PaymentIntent paymentIntent =
                        (PaymentIntent) event.getDataObjectDeserializer()
                                .getObject()
                                .orElse(null);

                if (paymentIntent == null) break;

                String orderIdStr = paymentIntent.getMetadata().get("orderId");

                if (orderIdStr == null) {
                    log.error("Missing orderId metadata");
                    break;
                }

                Integer orderId = Integer.valueOf(orderIdStr);

                String reason =
                        paymentIntent.getLastPaymentError() != null
                                ? paymentIntent.getLastPaymentError().getMessage()
                                : "Payment failed";

                paymentService.handlePaymentFailureByOrderId(
                        orderId,
                        paymentIntent.getId(),
                        reason
                );
            }

            default -> log.info("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook received");
    }

    private Session deserializeSession(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return (Session) deserializer.getObject().get();
        }
        log.error("Failed to deserialize Stripe Session from event: {}", event.getId());
        return null;
    }
}