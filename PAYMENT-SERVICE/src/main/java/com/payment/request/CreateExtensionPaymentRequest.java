package com.payment.request;

import lombok.Data;

@Data
public class CreateExtensionPaymentRequest {

    private Long orderId;
    private Long userId;
    private String successUrl;  // redirect after payment
    private String cancelUrl;

}
