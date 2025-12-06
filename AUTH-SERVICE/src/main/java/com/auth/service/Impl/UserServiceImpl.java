package com.auth.service.Impl;

import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.repository.UserRepository;
import com.auth.response.LoginResponse;
import com.auth.service.UserService;
import com.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    @Override
    public LoginResponse generateToken(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Validate password here…

        String token = jwtUtil.generateToken(username);

        return new LoginResponse(
                user.getId(),
                user.getProfilePictureUrl(),
                user.getUserType().name(),
                user.getEmail(),
                user.getAddress(),
                user.getFullName(),
                token,
                "Bearer"   // token type
        );    }

    public UserDto saveUser(User user){
        user.setUserName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return new UserDto(savedUser.getId(),
                savedUser.getUserName(),
                savedUser.getEmail(),
                savedUser.getUserType().name());
    }

    public Map<String,Object> changePassword(Long userId, String newPassword){
      try{
          User user = userRepository.findById(userId)
                  .orElseThrow(() -> new RuntimeException("User not found"));
          user.setPasswordHash(passwordEncoder.encode(newPassword));
          userRepository.save(user);
          return Map.of(
                  "message", "Password changed successfully",
                  "status", "success"
          );
      } catch (Exception e){
          return Map.of(
                  "message", "Error changing password: " + e.getMessage(),
                  "status", "error"
          );
      }
    }
}
