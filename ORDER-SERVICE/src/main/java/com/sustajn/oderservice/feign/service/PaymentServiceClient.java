package com.sustajn.oderservice.feign.service;

import com.sustajn.oderservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "PAYMENT-SERVICE",
configuration = FeignClientConfig.class)
public interface PaymentServiceClient {

    @PostMapping("/payments/autoPay")
    void autoPay(@RequestParam Long orderId,
                 @RequestParam Long userId,
                 @RequestParam int pendingQty,
                 @RequestParam Long unitPrice);
}
