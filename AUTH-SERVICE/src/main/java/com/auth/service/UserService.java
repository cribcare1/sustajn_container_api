package com.auth.service;

import com.auth.model.User;
import com.auth.request.UserDto;
import com.auth.request.*;
import com.auth.response.*;
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
    public User getUserByEmail(String email);
    public ApiResponse<?> registerRestaurant(
            RegistrationRequest request
    );
    ProfileResponse getRestaurantProfileById(Long restaurantId);
    ProfileResponse updateRestaurantProfileById(
            Long restaurantId,
            UpdateProfileRequest request
    );





    public ApiResponse<?> registerUserWithBankDetails(
            RegistrationRequest request
    );

    public Map<String, Object> getActiveRestaurantsMap(Pageable pageable);
    public Map<String, Object> getActiveCustomersMap(Pageable pageable);
    public List<RestaurantRegisterResponse> getAllActiveRestaurantsByListOfIds(List<Long> restaurantIds);
    public Map<String, Object> searchRestaurants(String keyword, double currentLat, double currentLon);
    public Map<String, Object> submitFeedback(FeedbackRequest request);


    Map<String, Object> getUserById(Long userId);
    ApiResponse<UserProfileResponse> upgradeUserSubscription(SubscriptionRequest subscriptionRequest);

    // Single API for both
    public List<FeedbackResponse> getFeedbackByType(Long id, String type);
    ApiResponse<UserProfileResponse> updateBusinessInfo(RegistrationRequest request);

    ApiResponse<UserProfileResponse> updateBankDetails(BankCardPaymentGetWayDetailsRequest request);

    ApiResponse<UserProfileResponse> getCustomerProfileDetails(Long userId);

    ApiResponse<UserProfileResponse> saveNewAddress(AddressRequest request);

    ApiResponse<UserProfileResponse> updateAddress(AddressRequest request);

    ApiResponse<UserProfileResponse> deleteAddress(AddressRequest request);

    ApiResponse<UserProfileResponse> createBankDetails(BankCardPaymentGetWayDetailsRequest bankCardPaymentGetWayDetailsRequest);

    ApiResponse<UserProfileResponse> deleteBankDetails(Long id);

    ApiResponse<UserProfileResponse> updateUserProfile(String userData, MultipartFile profileImage);

    ApiResponse<?> uploadImage(MultipartFile file, Long userId);

    ApiResponse<List<RestaurantListResponse>> getAllActiveRestaurants();

    ApiResponse<RestaurantDetailsResponse> getRestaurantDetailsById(Long restaurantId);

    ApiResponse<?> referPartner(ReferPartnerRequest request);

    ApiResponse<UserProfileResponse> addBusinessInfo(RegistrationRequest request);

    ApiResponse<UserResponse> getUserByCustomerId(String customerId);

    public ApiResponse<?> updateBankDetails(Long userId, RegistrationRequest request);
}


