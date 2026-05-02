package com.payment.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SoldRequest {

    private Long orderId;
    private Long paymentId;
    private String stripePaymentIntentId;
}
