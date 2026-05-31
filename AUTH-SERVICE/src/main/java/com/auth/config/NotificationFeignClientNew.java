//package com.auth.config;
//
//import com.auth.request.DeviceTokenRequest;
//import com.auth.response.DeviceToken;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//
//@FeignClient(name = "NOTIFICATION-SERVICE")
//public interface NotificationFeignClientNew {
//
//    @PostMapping(
//            value = "/notification/registerOrUpdateDeviceToken",
//            consumes = "application/json"
//    )
//    ResponseEntity<DeviceToken> registerOrUpdateDeviceToken(
//            @RequestBody DeviceTokenRequest request
//    );
//}
