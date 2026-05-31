package com.payment.response;

import lombok.Data;

@Data
public class PaymentResponse {

    private String checkoutUrl;   // Stripe hosted checkout URL
    private String sessionId;

}
