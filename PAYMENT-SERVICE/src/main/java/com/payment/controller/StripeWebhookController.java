package com.payment.controller;

import com.payment.configuration.StripeConfig;
import com.payment.service.ExtensionPaymentService;
import com.stripe.exception.SignatureVerificationException;
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
    private final ExtensionPaymentService extensionPaymentService;

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
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        // ✅ Verify webhook signature — prevents spoofed events
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("❌ Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature");
        }

        log.info("📩 Stripe webhook received: type={}, id={}", event.getType(), event.getId());

        switch (event.getType()) {

            case "checkout.session.completed" -> {
                // Payment was successful
                Session session = deserializeSession(event);
                if (session == null) break;

                String paymentIntentId = session.getPaymentIntent();
                String sessionId = session.getId();

                log.info("✅ Payment completed: sessionId={}, paymentIntentId={}",
                        sessionId, paymentIntentId);

                extensionPaymentService.handlePaymentSuccess(sessionId, paymentIntentId);
            }

            case "checkout.session.expired" -> {
                // Customer didn't complete payment within session window (24h default)
                Session session = deserializeSession(event);
                if (session == null) break;

                log.warn("⏰ Checkout session expired: sessionId={}", session.getId());
                extensionPaymentService.handlePaymentFailure(session.getId());
            }

            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        // ✅ Always return 200 to Stripe (even on internal errors)
        // Stripe retries on non-2xx for up to 72 hours
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