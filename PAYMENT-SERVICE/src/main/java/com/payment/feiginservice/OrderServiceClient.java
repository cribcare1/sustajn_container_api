package com.payment.feiginservice;

import com.payment.configuration.FeignClientConfig;
import com.payment.response.ApiResponse;
import com.payment.response.BorrowResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ORDER-SERVICE", configuration = FeignClientConfig.class)
public interface OrderServiceClient {


    @PostMapping("/orders/{orderId}/extendOrder")
    ApiResponse<Void> extendOrder(@PathVariable("orderId") Long orderId);

    @PostMapping("/orders/getBorrowedDetailsByOrderId")
    ApiResponse<List<BorrowResponse>> getBorrowedDetailsByOrderId(@RequestParam Long orderId);
}
