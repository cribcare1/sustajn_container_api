package com.auth.service;

import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.request.UpdateProfileRequest;
import com.auth.response.LoginResponse;

import java.util.Map;

public interface UserService {

    LoginResponse generateToken(String username);

    UserDto saveUser(User user);

    Map<String, Object> changePassword(Long userId, String newPassword);

    Map<String, Object> updateProfile(String username, UpdateProfileRequest request);

    Map<String, Object> logout(Long userId, String token);
}
