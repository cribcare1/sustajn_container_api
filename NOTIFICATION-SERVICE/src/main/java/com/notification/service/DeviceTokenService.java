package com.notification.service;

import com.notification.entity.DeviceToken;
import com.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceToken upsertUserDeviceToken(Long userId, String newToken, String deviceType) {

        return deviceTokenRepository
                .findByUserId(userId)
                .map(existing -> {

                    // 👍 If token is SAME → do nothing
                    if (existing.getDeviceToken() != null
                            && existing.getDeviceToken().equals(newToken)) {
                        return existing;
                    }

                    // 🔁 Token changed → update record
                    existing.setDeviceToken(newToken);
                    existing.setDeviceType(deviceType);
                    existing.setActive(true);
                    return deviceTokenRepository.save(existing);
                })
                .orElseGet(() -> {
                    // 🆕 First-time registration
                    DeviceToken token = new DeviceToken();
                    token.setUserId(userId);
                    token.setDeviceToken(newToken);
                    token.setDeviceType(deviceType);
                    token.setActive(true);
                    return deviceTokenRepository.save(token);
                });
    }

    public DeviceToken getDeviceTokenByUserId(Long userId) {
        return deviceTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Device token not found for user ID: " + userId));
    }
}
