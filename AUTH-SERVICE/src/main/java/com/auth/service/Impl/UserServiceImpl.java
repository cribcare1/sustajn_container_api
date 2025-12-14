package com.auth.service.impl;

import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.repository.UserRepository;
import com.auth.request.UpdateProfileRequest;
import com.auth.response.LoginResponse;
import com.auth.service.UserService;
import com.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse generateToken(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        String token = new JwtUtil().generateToken(username);

        return new LoginResponse(
                user.getId(),
                user.getProfilePictureUrl(),
                user.getUserType().name(),
                user.getEmail(),
                user.getAddress(),
                user.getFullName(),
                token,
                "Bearer"
        );
    }

    @Override
    public UserDto saveUser(User user) {
        user.setUserName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        return new UserDto(
                savedUser.getId(),
                savedUser.getUserName(),
                savedUser.getEmail(),
                savedUser.getUserType().name()
        );
    }

    @Override
    public Map<String, Object> changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return Map.of(
                "message", "Password changed successfully",
                "status", "success"
        );
    }

    @Override
    public Map<String, Object> logout(Long userId, String token) {
        Map<String, Object> response = new HashMap<>();

        if (token == null || token.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "No token provided");
            return response;
        }

        response.put("success", true);
        response.put("message", "Logout successful");
        response.put("userId", userId);
        return response;
    }

    @Override
    public Map<String, Object> updateProfile(String username, UpdateProfileRequest request) {

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());

        if (request.getAddress() != null)
            user.setAddress(request.getAddress());

        if (request.getPhoneNumber() != null)
            user.setPhoneNumber(request.getPhoneNumber());

        if (request.getProfilePictureUrl() != null)
            user.setProfilePictureUrl(request.getProfilePictureUrl());

        userRepository.save(user);

        return Map.of(
                "message", "Profile updated successfully",
                "status", "success"
        );
    }
}
