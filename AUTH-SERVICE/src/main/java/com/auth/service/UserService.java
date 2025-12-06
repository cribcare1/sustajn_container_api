package com.auth.service;

import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.response.LoginResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface UserService {
    public LoginResponse generateToken(String username);
    public UserDto saveUser(User user);
    public Map<String,Object> changePassword(Long userId, String newPassword);
}
