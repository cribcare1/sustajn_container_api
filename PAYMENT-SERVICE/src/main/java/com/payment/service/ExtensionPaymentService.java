package com.payment.service;

import com.payment.request.CreateExtensionPaymentRequest;
import com.payment.response.ApiResponse;
import com.payment.response.ExtensionPaymentResponse;
import org.springframework.stereotype.Service;

@Service
public interface ExtensionPaymentService {

    ApiResponse<ExtensionPaymentResponse> createCheckoutSession(CreateExtensionPaymentRequest request);

    void handlePaymentSuccess(String stripeSessionId, String paymentIntentId);

    void handlePaymentFailure(String stripeSessionId);
}
