package com.inventory.feignClient.service;


import com.inventory.feignClient.NotificationFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
}

