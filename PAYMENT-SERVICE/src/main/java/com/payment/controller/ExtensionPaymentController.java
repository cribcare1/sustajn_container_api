package com.payment.controller;

import com.payment.request.CreateExtensionPaymentRequest;
import com.payment.response.ApiResponse;
import com.payment.response.ExtensionPaymentResponse;
import com.payment.service.ExtensionPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("payments/extension")
@RequiredArgsConstructor
public class ExtensionPaymentController {

    private final ExtensionPaymentService extensionPaymentService;

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
    public ResponseEntity<ApiResponse<ExtensionPaymentResponse>> createCheckout(
            @RequestBody CreateExtensionPaymentRequest request) {

        ApiResponse<ExtensionPaymentResponse> response =
                extensionPaymentService.createCheckoutSession(request);

        return ResponseEntity.ok(response);
    }
}