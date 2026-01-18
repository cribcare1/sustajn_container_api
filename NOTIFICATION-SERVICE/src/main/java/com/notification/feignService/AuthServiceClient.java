package com.notification.feignService;

import com.notification.config.FeignClientConfig;
import com.notification.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "AUTH-SERVICE", configuration = FeignClientConfig.class)
public interface AuthServiceClient {
    @GetMapping("/auth/getUserByEmail/{email}")
    UserResponse getUserByEmail(@PathVariable("email") String email);
}