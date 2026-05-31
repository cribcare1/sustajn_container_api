package com.payment.request;

import lombok.Data;

@Data
public class CreateExtensionPaymentRequest {

    private Long orderId;
    private Long userId;
    private Double amount;
    private String successUrl;  // redirect after payment
    private String cancelUrl;

}
