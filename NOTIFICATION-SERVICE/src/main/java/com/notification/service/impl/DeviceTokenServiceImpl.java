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



    //        return deviceTokenRepository
//                .findByUserId(userId)
//                .map(existing -> {
//
//                    // 👍 If token is SAME → do nothing
//                    if (existing.getDeviceToken() != null
//                            && existing.getDeviceToken().equals(newToken)) {
//                        return existing;
//                    }
//
//                    // 🔁 Token changed → update record
//                    existing.setDeviceToken(newToken);
//                    existing.setDeviceType(deviceType);
//                    existing.setActive(true);
//                    return deviceTokenRepository.save(existing);
//                })
//                .orElseGet(() -> {
//                    // 🆕 First-time registration
//                    DeviceToken token = new DeviceToken();
//                    token.setUserId(userId);
//                    token.setDeviceToken(newToken);
//                    token.setDeviceType(deviceType);
//                    token.setActive(true);
//                    return deviceTokenRepository.save(token);
//                });
}
