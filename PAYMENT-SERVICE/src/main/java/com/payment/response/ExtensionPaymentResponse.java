package com.payment.response;

import lombok.Data;

@Data
public class ExtensionPaymentResponse {

    private String checkoutUrl;   // Stripe hosted checkout URL
    private String sessionId;

}
