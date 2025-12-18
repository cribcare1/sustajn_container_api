package com.inventory.feignClient;


import com.inventory.config.FeignMultipartConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@FeignClient(
        name = "NOTIFICATION-SERVICE",
        configuration = FeignMultipartConfig.class
)public interface NotificationFeignClient {

    /* ---------- PASSWORD / TOKEN ---------- */
//
//    @PostMapping("/notification/forgot-password")
//    Map<String, String> forgotPassword(@RequestBody ForgotRequest request);
//
//    @PostMapping("/notification/verify-token")
//    Map<String, String> verifyToken(@RequestBody VerifyRequest request);

    /* ---------- IMAGE APIs ---------- */

    @PostMapping(
            value = "/images/{type}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    String uploadImage(
            @PathVariable("type") String type,
            @RequestPart("file") MultipartFile file
    );

    @PutMapping(
            value = "/images/{type}/{fileName}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    String updateImage(
            @PathVariable("type") String type,
            @PathVariable("fileName") String fileName,
            @RequestPart("file") MultipartFile file
    );

    @DeleteMapping("/images/{type}/{fileName}")
    void deleteImage(
            @PathVariable("type") String type,
            @PathVariable("fileName") String fileName
    );

    @GetMapping("/images/{type}/{fileName}")
    byte[] fetchImage(
            @PathVariable("type") String type,
            @PathVariable("fileName") String fileName
    );
}


