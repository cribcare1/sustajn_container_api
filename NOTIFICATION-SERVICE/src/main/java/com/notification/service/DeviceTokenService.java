package com.notification.service;

import com.notification.entity.DeviceToken;
import org.springframework.stereotype.Service;

@Service
public interface DeviceTokenService {

    DeviceToken addOrUpdateUserDeviceToken(Long userId, String newToken, String deviceType);

    DeviceToken getDeviceTokenByUserId(Long userId);


}
