package com.notification.repository;

import com.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUserIdAndActiveTrue(Long userId);

    Optional<DeviceToken> findByUserIdAndDeviceToken(Long userId, String deviceToken);

    Optional<DeviceToken> findByUserId(Long userId);
}

