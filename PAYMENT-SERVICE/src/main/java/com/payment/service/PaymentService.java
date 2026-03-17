package com.payment.service;

import com.payment.request.CreateExtensionPaymentRequest;
import com.payment.response.ApiResponse;
import com.payment.response.PaymentResponse;
import org.springframework.stereotype.Service;

@Service
public interface PaymentService {

    ApiResponse<PaymentResponse> createCheckoutSession(CreateExtensionPaymentRequest request);

    void handlePaymentSuccess(Integer orderId, String paymentIntentId);

    void handlePaymentFailure(String stripeSessionId, String paymentIntentId, String failureReason);

    void handlePaymentFailureByOrderId(Integer orderId, String paymentIntentId, String reason);
}
