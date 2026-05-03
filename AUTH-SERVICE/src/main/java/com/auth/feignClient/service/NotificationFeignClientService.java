package com.auth.feignClient.service;

import com.auth.feignClient.NotificationFeignClient;
import com.auth.request.DeviceTokenRequest;
import com.auth.response.DeviceToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class NotificationFeignClientService {
    private final NotificationFeignClient notificationClient;

    public String uploadImage(String imageType,MultipartFile file) {
        return notificationClient.uploadImage(imageType, file);
    }

    public byte[] getContainerImage(String imageType,String fileName) {
        return notificationClient.fetchImage(imageType, fileName);
    }

    public void deleteContainer(String imageType,String fileName) {
        notificationClient.deleteImage(imageType, fileName);
    }

    public void registerOrUpdateDeviceToken(DeviceTokenRequest request) {
        notificationClient.registerOrUpdateDeviceToken(request);
    }



}
