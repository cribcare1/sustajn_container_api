package com.notification.service.impl;

import com.notification.entity.DeviceToken;
import com.notification.repository.DeviceTokenRepository;
import com.notification.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public DeviceToken addOrUpdateUserDeviceToken(Long userId, String newToken, String deviceType) {

        // If token already exists for some other user → delete it
        deviceTokenRepository.findByDeviceToken(newToken)
                .filter(token -> !token.getUserId().equals(userId))
                .ifPresent(deviceTokenRepository::delete);

        // Find existing token for current user
        DeviceToken deviceToken = deviceTokenRepository.findByUserId(userId)
                .map(existing -> {
                    // If same token → return as is
                    if (existing.getDeviceToken().equals(newToken)) {
                        return existing;
                    }
                    // Else update token
                    existing.setDeviceToken(newToken);
                    return existing;
                })
                .orElseGet(() -> {
                    DeviceToken deviceTokenNew = new DeviceToken();
                    deviceTokenNew.setUserId(userId);
                    deviceTokenNew.setDeviceToken(newToken);
                    return deviceTokenNew;
                });

        deviceToken.setDeviceType(deviceType);
        deviceToken.setActive(true);

        return deviceTokenRepository.save(deviceToken);
    }


    public DeviceToken getDeviceTokenByUserId(Long userId) {
        return deviceTokenRepository.findByUserId(userId)
                .orElse(null);
    }
}
