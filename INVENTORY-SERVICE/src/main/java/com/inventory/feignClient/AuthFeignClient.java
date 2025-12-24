package com.inventory.feignClient;

import com.inventory.config.FeignMultipartConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "AUTH-SERVICE",
        configuration = FeignMultipartConfig.class
)
public interface AuthFeignClient {
}
