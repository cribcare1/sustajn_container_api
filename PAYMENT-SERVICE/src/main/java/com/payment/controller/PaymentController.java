package com.payment.controller;

import com.payment.request.CreateExtensionPaymentRequest;
import com.payment.response.ApiResponse;
import com.payment.response.PaymentResponse;
import com.payment.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/extension/checkout
     * Customer calls this to get a Stripe Checkout URL for paying the extension fee.
     *
     * Request body:
     * {
     *   "orderId": 101,
     *   "userId": 55,
     *   "successUrl": "https://yourapp.com/payment/success",
     *   "cancelUrl":  "https://yourapp.com/payment/cancel"
     * }
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<PaymentResponse>> createCheckout(
            @RequestBody CreateExtensionPaymentRequest request) {

        ApiResponse<PaymentResponse> response =
                paymentService.createCheckoutSession(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{sessionId}")
    public ApiResponse<String> checkPaymentStatus(@PathVariable String sessionId) {

        try {

            Session session = Session.retrieve(sessionId);

            return new ApiResponse<>("success", session.getPaymentStatus());

        } catch (StripeException e) {

            return new ApiResponse<>("error", "Unable to fetch status");
        }
    }

    @PostMapping("/autoPay")
    public ResponseEntity<Void> autoPay(
            @RequestParam Long orderId,
            @RequestParam Long userId,
            @RequestParam int pendingQty,
            @RequestParam Long unitPrice) {

        paymentService.autoPay(orderId, userId, pendingQty, unitPrice);

        return ResponseEntity.ok().build();
    }
}