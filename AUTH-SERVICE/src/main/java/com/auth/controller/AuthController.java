package com.auth.controller;

import com.auth.constant.AuthConstant;
import com.auth.feignClient.service.NotificationFeignClientService;
import com.auth.model.User;
import com.auth.repository.UserRepository;
import com.auth.request.*;
import com.auth.response.*;
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

import java.util.HashMap;
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
                            "message", "Something went wrong" + ex.getMessage()
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
                        new ApiResponse<>(AuthConstant.ERROR, "Username not found"));
            }

            User user = userOpt.get();

            if (!user.getUserType().equals(loginRequest.getRole())) {
                System.err.println("User role mismatch: expected " + user.getUserType() + " but got " + loginRequest.getRole());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ApiResponse<>(AuthConstant.ERROR, "User role mismatch")
                );
            }

            // 2️⃣ Check password manually
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ApiResponse<>(AuthConstant.ERROR, "Invalid password")
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
                        new ApiResponse<>(AuthConstant.ERROR, "Authentication failed")
                );
            }

            // 4️⃣ Generate token
            LoginResponse response = userService.generateToken(loginRequest.getUserName());

            return ResponseEntity.ok(
                    new ApiResponse<>(AuthConstant.SUCCESS, "Login successful", response)
            );

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponse<>(AuthConstant.ERROR, "Something went wrong")
            );
        }
    }


    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest passwordRequest) {
        Map<String, Object> response = userService.changePassword(passwordRequest.getEmail(), passwordRequest.getNewPassword());
        return ResponseEntity.ok(response);
    }


    @PostMapping(
            value = "/register-restaurant",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> registerRestaurant(
            @RequestBody @Valid RegistrationRequest data) {
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
    ) {
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
    public ResponseEntity<?> searchRestaurant(@RequestParam String keyword,
                                              @RequestParam double lat,
                                              @RequestParam double lon) {
        Map<String, Object> response = userService.searchRestaurants(keyword, lat, lon);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/userDetails/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        Map<String, Object> response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upgradeSubscription")
    public ResponseEntity<ApiResponse<UserProfileResponse>> upgradeUserSubscription(
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
    public ResponseEntity<ApiResponse<UserProfileResponse>> createBankDetails(@RequestBody @Validated(CreateGroup.class) BankCardPaymentGetWayDetailsRequest bankCardPaymentGetWayDetailsRequest) {
        return ResponseEntity.ok(userService.createBankDetails(bankCardPaymentGetWayDetailsRequest));
    }

    @PostMapping("/deleteBankDetails/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> deleteBankDetails(@PathVariable @NotNull(message = "Please provide user id") Long id) {
        return ResponseEntity.ok(userService.deleteBankDetails(id));
    }

    @PostMapping("/updateBankDetails")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateBankDetails(@RequestBody @Validated(UpdateGroup.class) BankCardPaymentGetWayDetailsRequest request) {
        return ResponseEntity.ok(userService.updateBankDetails(request));
    }

    @PostMapping("/updateBusinessInfo")
    public ResponseEntity<?> updateBusinessInfo(@RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(userService.updateBusinessInfo(request));
    }

    // save new address
    @PostMapping("/saveAddress")
    public ResponseEntity<ApiResponse<UserProfileResponse>> saveNewAddress(@RequestBody @Validated(CreateGroup.class) AddressRequest request) {
        return ResponseEntity.ok(userService.saveNewAddress(request));
    }

    //update address
    @PostMapping("/updateAddress")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateAddress(@RequestBody @Validated(UpdateGroup.class) AddressRequest request) {
        return ResponseEntity.ok(userService.updateAddress(request));
    }

    //Delete address
    @PostMapping("/deleteAddress")
    public ResponseEntity<ApiResponse<UserProfileResponse>> deleteAddress(@RequestBody @Validated(UpdateGroup.class) AddressRequest request) {
        return ResponseEntity.ok(userService.deleteAddress(request));
    }

    //Get Profile details
    @GetMapping("/getProfileDetails/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileDetails(@PathVariable @NotNull(message = "Please provide user id") Long userId) {
        return ResponseEntity.ok(userService.getCustomerProfileDetails(userId));
    }

    //Update profile details
    @PostMapping("/updateProfileDetails")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfileDetails(@RequestPart @Validated(UpdateGroup.class) String userData, @RequestPart(required = false) MultipartFile profileImage) {
        return ResponseEntity.ok(userService.updateUserProfile(userData, profileImage));
    }


    @PostMapping("/uploadImage/{userId}")
    public ResponseEntity<?> uploadImage(@RequestPart MultipartFile image, @PathVariable Long userId) {
        ApiResponse apiResponse = userService.uploadImage(image, userId);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/getUserByEmail/{email}")
    public ResponseEntity<NotificationUserResponse> getUserByEmail(@PathVariable String email) {
        User user = userService.getUserByEmail(email);
        if (user != null) {
            NotificationUserResponse userResponse = new NotificationUserResponse();
            userResponse.setId(user.getId());
            userResponse.setUserType(user.getUserType());
            userResponse.setEmail(user.getEmail());
            userResponse.setAccountStatus(user.getAccountStatus());
            userResponse.setFullName(user.getFullName());
            userResponse.setPhoneNumber(user.getPhoneNumber());
            userResponse.setPushNotification(null);
            return ResponseEntity.ok(userResponse);
        }
        return ResponseEntity.ok(null);
    }

    // Get All Active Restaurants list
    @GetMapping("/getAllActiveRestaurants")
    public ResponseEntity<?> getAllActiveRestaurants() {
        return ResponseEntity.ok(userService.getAllActiveRestaurants());
    }

    @GetMapping("/getRestaurantDetailsById/{restaurantId}")
    public ResponseEntity<?> getRestaurantDetailsById(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(userService.getRestaurantDetailsById(restaurantId));
    }

    @PostMapping("/referPartner")
    public ResponseEntity<?> referPartner(@RequestBody @Validated(CreateGroup.class) ReferPartnerRequest request) {
        return ResponseEntity.ok(userService.referPartner(request));
    }

    @PostMapping("/addBusinessInfo")
    public ResponseEntity<ApiResponse<UserProfileResponse>> addBusinessInfo(@RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(userService.addBusinessInfo(request));
    }

    @GetMapping("/getUserByCustomerId")
    public ResponseEntity<ApiResponse<UserResponse>> getUserIdByCustomerId(@RequestParam String customerId) {
        return ResponseEntity.ok(userService.getUserByCustomerId(customerId));
    }

    @PostMapping("/updateBankDetailsByUser")
    public ResponseEntity<ApiResponse<?>> updateBankDetails(
            @RequestParam Long customerId,
            @RequestBody RegistrationRequest request) {

        return ResponseEntity.ok(userService.updateBankDetails(customerId, request));
    }

    @GetMapping("/internal/restaurant-name/{userId}")
    public ResponseEntity<Map<String, String>> getRestaurantNameInternal(@PathVariable Long userId) {
        Map<String, String> response = new HashMap<>();

        try {
            // 1. Find the user entity inside your auth database (using your exact User model)
            Optional<com.auth.model.User> userOpt = userRepository.findById(userId);

            if (userOpt.isPresent() && userOpt.get().getFullName() != null) {
                // 🟢 Map the entity field 'fullName' directly to the response key
                response.put("restaurantName", userOpt.get().getFullName());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to resolve restaurant name for id: {}", userId, e);
        }

        // Safe fallback text if no account is found or an exception is caught
        response.put("restaurantName", "Unknown Restaurant");
        return ResponseEntity.ok(response);
    }
    @GetMapping("/internal/restaurant-profile/{userId}")
    public ResponseEntity<Map<String, String>> getRestaurantProfileInternal(@PathVariable Long userId) {
        Map<String, String> response = new HashMap<>();
        response.put("restaurantName", "Unknown Restaurant");
        response.put("address", "No Address Provided");

        try {
            Optional<com.auth.model.User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                com.auth.model.User user = userOpt.get();

                // 1. Extract Full Name safely
                if (user.getFullName() != null) {
                    response.put("restaurantName", user.getFullName());
                }

                // 2. Extract first address text safely from the List<Address> using explicit entity fields
                if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {

                    // 🟢 Typed explicitly to your core database Address entity model class
                    com.auth.model.Address firstAddress = user.getAddresses().get(0);

                    if (firstAddress != null) {
                        StringBuilder addressBuilder = new StringBuilder();

                        // Append house/flat details safely
                        if (firstAddress.getFlatDoorHouseDetails() != null && !firstAddress.getFlatDoorHouseDetails().trim().isEmpty()) {
                            addressBuilder.append(firstAddress.getFlatDoorHouseDetails().trim());
                        }

                        // Append area/street/block details with proper comma positioning logic
                        if (firstAddress.getAreaStreetCityBlockDetails() != null && !firstAddress.getAreaStreetCityBlockDetails().trim().isEmpty()) {
                            if (addressBuilder.length() > 0) {
                                addressBuilder.append(", ");
                            }
                            addressBuilder.append(firstAddress.getAreaStreetCityBlockDetails().trim());
                        }

                        // Append the postal tracking code block
                        if (firstAddress.getPoBoxOrPostalCode() != null && !firstAddress.getPoBoxOrPostalCode().trim().isEmpty()) {
                            if (addressBuilder.length() > 0) {
                                addressBuilder.append(" - ");
                            }
                            addressBuilder.append(firstAddress.getPoBoxOrPostalCode().trim());
                        }

                        String combinedAddress = addressBuilder.toString();
                        if (!combinedAddress.isEmpty()) {
                            response.put("address", combinedAddress);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to resolve restaurant profile for id: {}", userId, e);
        }

        return ResponseEntity.ok(response);
    }
}
