package com.auth.service;

import com.auth.model.Address;
import com.auth.model.BankDetails;
import com.auth.model.User;
import com.auth.request.UserDto;
import com.auth.request.*;
import com.auth.response.*;
import com.auth.request.UpdateBusinessInfoRequest;
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
            RestaurantRegistrationRequest request
    );
    ProfileResponse getRestaurantProfileById(Long restaurantId);
    ProfileResponse updateRestaurantProfileById(
            Long restaurantId,
            UpdateProfileRequest request
    );





    public Map<String, Object> registerUserWithBankDetails(
            RestaurantRegistrationRequest request
    );

    public Map<String, Object> getActiveRestaurantsMap(Pageable pageable);
    public Map<String, Object> getActiveCustomersMap(Pageable pageable);
    public List<RestaurantRegisterResponse> getAllActiveRestaurantsByListOfIds(List<Long> restaurantIds);
    public Map<String, Object> searchRestaurants(String keyword, double currentLat, double currentLon);
    public Map<String, Object> submitFeedback(FeedbackRequest request);


    Map<String, Object> getUserById(Long userId);
    ApiResponse<CustomerProfileResponse> upgradeUserSubscription(SubscriptionRequest subscriptionRequest);

    // Single API for both
    public List<FeedbackResponse> getFeedbackByType(Long id, String type);
    Map<String, Object> updateBusinessInfo(Long userId, UpdateBusinessInfoRequest request);

  ApiResponse<CustomerProfileResponse> updateBankDetails(BankCardPaymentGetWayDetailsRequest request);

   ApiResponse<CustomerProfileResponse> getCustomerProfileDetails(Long userId);

    ApiResponse<Address> saveNewAddress(AddressRequest request);

    ApiResponse<CustomerProfileResponse> updateAddress(AddressRequest request);

    ApiResponse<Address> deleteAddress(AddressRequest request);

    ApiResponse<BankDetails> createBankDetails(BankCardPaymentGetWayDetailsRequest bankCardPaymentGetWayDetailsRequest);

    ApiResponse<BankDetails> deleteBankDetails(Long id);

    ApiResponse<CustomerProfileResponse> updateUserProfile(String userData, MultipartFile profileImage);
    ApiResponse<?> uploadImage(MultipartFile file, Long userId);
}


