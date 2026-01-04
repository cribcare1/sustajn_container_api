package com.auth.service;

import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.request.*;
import com.auth.response.LoginResponse;
import com.auth.response.RestaurantRegisterResponse;
import com.auth.response.ProfileResponse;
import com.auth.response.BankDetailsResponse;
import com.auth.response.FeedbackResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.Map;

@Service
public interface UserService {
    public LoginResponse generateToken(String username);
    public UserDto saveUser(User user);
    public Map<String,Object> changePassword(Long userId, String newPassword);
    public Map<String,Object> changePassword(String email, String newPassword);

    public Map<String,Object> registerRestaurant(
            RestaurantRegistrationRequest request,
            MultipartFile profileImage
    );
    ProfileResponse getRestaurantProfileById(Long restaurantId);
    ProfileResponse updateRestaurantProfileById(
            Long restaurantId,
            UpdateProfileRequest request
    );





    public Map<String, Object> registerUserWithBankDetails(
            RestaurantRegistrationRequest request,
            MultipartFile profileImage
    );

    public Map<String, Object> getActiveRestaurantsMap(Pageable pageable);
    public Map<String, Object> getActiveCustomersMap(Pageable pageable);
    public List<RestaurantRegisterResponse> getAllActiveRestaurantsByListOfIds(List<Long> restaurantIds);
    public Map<String, Object> searchRestaurants(String keyword, double currentLat, double currentLon);
    public Map<String, Object> submitFeedback(FeedbackRequest request);


    Map<String, Object> getUserById(Long userId);
    Map<String, Object> upgradeUserSubscription(SubscriptionRequest subscriptionRequest);

    // Single API for both
    public List<FeedbackResponse> getFeedbackByType(Long id, String type);
    BankDetailsResponse updateBankDetails(Long userId, UpdateBankDetailsRequest request);

    Map<String, Object> getCustomerProfileDetails(Long userId);

    Map<String, Object> saveNewAddress(AddressRequest request);

    Map<String, Object> updateAddress(AddressRequest request);

    Map<String, Object> deleteAddress(Long addressId);
}


