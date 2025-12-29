package com.auth.service;


import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.request.RestaurantRegistrationRequest;
import com.auth.request.RestaurantFeedbackRequest;
import com.auth.response.LoginResponse;
import com.auth.response.ProfileResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.auth.request.UpdateProfileRequest;


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
    Map<String, Object> submitRestaurantFeedback(
            RestaurantFeedbackRequest request
    );


    public Map<String, Object> getActiveRestaurantsMap(Pageable pageable);
    public Map<String, Object> getActiveCustomersMap(Pageable pageable);





}
