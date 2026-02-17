package com.sustajn.oderservice.feign.service;

import com.sustajn.oderservice.config.FeignClientConfig;
import com.sustajn.oderservice.dto.DeviceTokenResponse;
import com.sustajn.oderservice.dto.NotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "NOTIFICATION-SERVICE",
        configuration = FeignClientConfig.class
)
public interface NotificationFeignClient {


    @PostMapping("/notification/send")
    public String sendNotificationToMultipleDevices(@RequestBody NotificationResponse request);


    @PostMapping("/notification/getDeviceTokens")
    public DeviceTokenResponse getDeviceTokensByUserId(@RequestParam Long userId);

}
