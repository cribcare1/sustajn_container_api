package com.auth.controller;

import com.auth.feignClient.service.NotificationFeignClientService;
import com.auth.model.User;
import com.auth.request.UserDto;
import com.auth.repository.UserRepository;
import com.auth.request.*;
import com.auth.response.*;
import com.auth.request.FeedbackRequest;
import com.auth.request.BankCardPaymentGetWayDetailsRequest;
import com.auth.service.UserService;
import com.auth.validation.CreateGroup;
import com.auth.validation.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final NotificationFeignClientService notificationFeignClientService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    
    @PostMapping("/register-user")
    public ResponseEntity<?> registerUser(@RequestBody User user) {

        try {
            log.info("inside register user method");
            // Check if username already exists
            UserDto userDto = userService.saveUser(user);

            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of(
                            "status", "success",
                            "message", "User registered successfully",
                            "data", userDto
                    )
            );

        } catch (RuntimeException ex) {

            return ResponseEntity.ok(
                    Map.of(
                            "status", "error",
                            "message", ex.getMessage()
                    )
            );

        } catch (Exception ex) {

            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of(
                            "status", "success",
                            "message", "Something went wrong"+ ex.getMessage()
                    )
            );
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> generateToken(
            @RequestBody LoginRequest loginRequest) {

        try {
            log.info("inside login method");
            Optional<User> userOpt = userRepository.findByUserName(loginRequest.getUserName());
            log.info("UserOpt: {}", userOpt);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of(
                                "status", "ERROR",
                                "message", "Username not found"
                        )
                );
            }

            User user = userOpt.get();

            // 2️⃣ Check password manually
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of(
                                "status", "error",
                                "message", "Invalid password"
                        )
                );
            }

            // 3️⃣ Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUserName(),
                            loginRequest.getPassword()
                    )
            );

            if (!authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of(
                                "status", "error",
                                "message", "Authentication failed"
                        )
                );
            }

            // 4️⃣ Generate token
            LoginResponse response = userService.generateToken(loginRequest.getUserName());

            return ResponseEntity.ok(
                    Map.of(
                            "status", "success",
                            "message", "Login successful",
                            "data", response
                    )
            );

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of(
                            "status", "error",
                            "message", "Something went wrong"
                    )
            );
        }
    }


    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest passwordRequest){
        Map<String,Object> response = userService.changePassword(passwordRequest.getEmail(),passwordRequest.getNewPassword());
        return ResponseEntity.ok(response);
    }


    @PostMapping(
            value = "/register-restaurant",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> registerRestaurant(
            @RequestBody @Valid RegistrationRequest data)  {
        return ResponseEntity.ok(
                userService.registerRestaurant(data)
        );
    }


    @PostMapping(
            value = "/registerCostumer",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> registerUserWithBankDetails(
            @RequestBody @Valid RegistrationRequest request
    )  {
        return ResponseEntity.ok(
                userService.registerUserWithBankDetails(request)
        );
    }

    @GetMapping("/activeRestaurants")
    public ResponseEntity<?> getActiveRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Map<String, Object> response = userService.getActiveRestaurantsMap(PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }



    @GetMapping("/activeCustomersDetails")
    public ResponseEntity<?> getActiveCustomersDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Map<String, Object> response = userService.getActiveCustomersMap(PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }


    @GetMapping("/images/{type}/{fileName}")
    public ResponseEntity<byte[]> fetchImage(
            @PathVariable String type,
            @PathVariable String fileName) {

        byte[] imageBytes = notificationFeignClientService.getContainerImage(type, fileName);

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.IMAGE_JPEG) // You can dynamically detect type if needed
                .body(imageBytes);
    }
    @GetMapping("/{restaurantId}/getProfile")
    public ProfileResponse getRestaurantProfileById(
            @PathVariable Long restaurantId
    ) {
        return userService.getRestaurantProfileById(restaurantId);
    }
    @PutMapping("/{restaurantId}/profile")
    public ProfileResponse updateRestaurantProfileById(
            @PathVariable Long restaurantId,
            @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateRestaurantProfileById(restaurantId, request);
    }


    @PostMapping("/getRestaurants")
    public List<RestaurantRegisterResponse> getRestaurants(@RequestBody List<Long> ids) {
        return userService.getAllActiveRestaurantsByListOfIds(ids);
    }

    @GetMapping("/searchRestaurant")
    public ResponseEntity<?> searchRestaurant(  @RequestParam String keyword,
                                                @RequestParam double lat,
                                                @RequestParam double lon) {
        Map<String,Object> response= userService.searchRestaurants(keyword,lat,lon);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/userDetails/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        Map<String, Object> response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upgradeSubscription")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> upgradeUserSubscription(
            @RequestBody @Validated({CreateGroup.class, UpdateGroup.class}) SubscriptionRequest subscriptionRequest
    ) {
        return ResponseEntity.ok(userService.upgradeUserSubscription(subscriptionRequest));
    }

    @PostMapping("/submitFeedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(userService.submitFeedback(request));
    }

    // Single API: /auth/feedback/fetch?id=1&type=RESTAURANT
    @GetMapping("/getFeeback")
    public ResponseEntity<?> getFeedback(
            @RequestParam Long id,
            @RequestParam String type
    ) {
        try {
            return ResponseEntity.ok(userService.getFeedbackByType(id, type));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/createBankDetails")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> createBankDetails(@RequestBody @Validated(CreateGroup.class) BankCardPaymentGetWayDetailsRequest bankCardPaymentGetWayDetailsRequest){
        return ResponseEntity.ok(userService.createBankDetails(bankCardPaymentGetWayDetailsRequest));
    }

    @PostMapping("/deleteBankDetails/{id}")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> deleteBankDetails(@PathVariable @NotNull(message = "Please provide user id") Long id){
        return ResponseEntity.ok(userService.deleteBankDetails(id));
    }

    @PostMapping("/updateBankDetails")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateBankDetails(@RequestBody @Validated(UpdateGroup.class) BankCardPaymentGetWayDetailsRequest request) {
            return ResponseEntity.ok(userService.updateBankDetails(request));
    }
    @PutMapping("/updateBusinessInfo/{restaurantId}")
    public ResponseEntity<?> updateBusinessInfo(
            @PathVariable Long restaurantId,  // <--- Renamed variable
            @RequestBody UpdateBusinessInfoRequest request
    ) {
        try {
            // Pass restaurantId to the service (it matches the Long type expected)
            return ResponseEntity.ok(userService.updateBusinessInfo(restaurantId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    // save new address
    @PostMapping("/saveAddress")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> saveNewAddress(@RequestBody @Validated(CreateGroup.class) AddressRequest request) {
        return ResponseEntity.ok(userService.saveNewAddress(request));
    }

    //update address
    @PostMapping("/updateAddress")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateAddress(@RequestBody @Validated(UpdateGroup.class) AddressRequest request) {
        return ResponseEntity.ok(userService.updateAddress(request));
    }

    //Delete address
    @PostMapping("/deleteAddress")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> deleteAddress(@RequestBody @Validated(UpdateGroup.class) AddressRequest request) {
        return ResponseEntity.ok(userService.deleteAddress(request));
    }

    //Get Profile details
    @GetMapping("/getProfileDetails/{userId}")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfileDetails(@PathVariable @NotNull(message = "Please provide user id") Long userId) {
        return ResponseEntity.ok(userService.getCustomerProfileDetails(userId));
    }

    //Update profile details
    @PostMapping("/updateProfileDetails")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfileDetails(@RequestPart @Validated(UpdateGroup.class) String userData, @RequestPart(required = false) MultipartFile profileImage) {
        return ResponseEntity.ok(userService.updateUserProfile(userData, profileImage));
    }


    @PostMapping("/uploadImage/{userId}")
    public ResponseEntity<?> uploadImage(@RequestPart MultipartFile image,@PathVariable Long userId) {
      ApiResponse apiResponse=  userService.uploadImage(image,userId);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/getUserByEmail/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email){
        User user=userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}
