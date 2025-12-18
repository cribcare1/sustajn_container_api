package com.auth.service;

import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.request.RestaurantRegistrationRequest;
import com.auth.request.UpdateProfileRequest;
import com.auth.response.LoginResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService {

    // -------- AUTH --------
    LoginResponse generateToken(String username);

    UserDto saveUser(User user);

    Map<String, Object> logout(Long userId, String token);

    // -------- PASSWORD --------
    Map<String, Object> changePassword(Long userId, String newPassword);

    Map<String, Object> changePassword(String email, String newPassword);

    // -------- PROFILE --------
    Map<String, Object> updateProfile(String username, UpdateProfileRequest request);

    // -------- REGISTRATION --------
    Map<String, Object> registerRestaurant(
            RestaurantRegistrationRequest request,
            MultipartFile profileImage
    );

    Map<String, Object> registerUserWithBankDetails(
            RestaurantRegistrationRequest request,
            MultipartFile profileImage
    );

    // -------- LISTING --------
    Map<String, Object> getActiveRestaurantsMap(Pageable pageable);

    Map<String, Object> getActiveCustomersMap(Pageable pageable);
}
