package com.auth.service.Impl;

import com.auth.constant.AuthConstant;
import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.UserType;
import com.auth.feignClient.service.NotificationFeignClientService;
import com.auth.model.*;
import com.auth.repository.BankDetailsRepository;
import com.auth.repository.BasicRestaurantDetailsRepository;
import com.auth.repository.SocialMediaDetailsRepository;
import com.auth.repository.UserRepository;
import com.auth.request.RestaurantRegistrationRequest;
import com.auth.request.UpdateBusinessInfoRequest;
import com.auth.request.SubscriptionRequest;
import com.auth.response.*;
import com.auth.response.CustomerDetailsBasic;
import com.auth.response.LoginResponse;
import com.auth.response.RestaurantBasicDetailsResponse;
import com.auth.response.RestaurantRegisterResponse;
import com.auth.response.ProfileResponse;
import com.auth.response.BankDetailsResponse;
import com.auth.repository.FeedbackRepository; // Import exists
import com.auth.request.FeedbackRequest;
import com.auth.request.UpdateBankDetailsRequest;
import com.auth.response.FeedbackResponse;

import com.auth.service.UserService;
import com.auth.util.DistanceUtil;
import com.auth.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.auth.request.UpdateProfileRequest;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final BasicRestaurantDetailsRepository basicRepo;
    private final BankDetailsRepository bankRepo;
    private final SocialMediaDetailsRepository socialRepo;
    private final NotificationFeignClientService notificationFeignClientService;
    private final FeedbackRepository feedbackRepository;

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

    @Override
    public UserDto saveUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setUserName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        user.setAccountStatus(AccountStatus.active);
        user.setUserType(UserType.ADMIN);

        User savedUser = userRepository.save(user);

        return new UserDto(
                savedUser.getId(),
                savedUser.getUserName(),
                savedUser.getEmail(),
                savedUser.getUserType().name()
        );
    }

    @Override
    public ProfileResponse getRestaurantProfileById(Long restaurantId) {
        User user = userRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        if (user.getUserType() != UserType.RESTAURANT) {
            throw new RuntimeException("User is not a restaurant");
        }
        BankDetails bankDetails = bankRepo.findByUserId(user.getId()).orElse(null);

        BankDetailsResponse bankResponse = null;
        if (bankDetails != null) {
            bankResponse = BankDetailsResponse.builder()
                    .id(bankDetails.getId())
                    .userId(bankDetails.getUserId())
                    .bankName(bankDetails.getBankName())
                    .accountNumber(bankDetails.getAccountNumber())
                    .iBanNumber(bankDetails.getIBanNumber())
                    .taxNumber(bankDetails.getTaxNumber())
                    .build();
        }
        BasicRestaurantDetails business = basicRepo.findByRestaurantId(user.getId()).orElse(null);

        ProfileResponse.BusinessInfoResponse businessInfoResponse = null;
        if (business != null) {
            businessInfoResponse = ProfileResponse.BusinessInfoResponse.builder()
                    .businessType(business.getBusinessType())
                    .website(business.getWebsiteDetails()) // Map websiteDetails -> website
                    .build();
        }

        return ProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .address(user.getAddress())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .bankDetails(bankResponse)
                .businessInfo(businessInfoResponse)
                .build();
    }

    @Override
    public ProfileResponse updateRestaurantProfileById(
            Long restaurantId,
            UpdateProfileRequest request
    ) {
        User user = userRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        if (user.getUserType() != UserType.RESTAURANT) {
            throw new RuntimeException("User is not a restaurant");
        }

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());

        if (request.getAddress() != null)
            user.setAddress(request.getAddress());

        if (request.getPhoneNumber() != null)
            user.setPhoneNumber(request.getPhoneNumber());

        if (request.getProfilePictureUrl() != null)
            user.setProfilePictureUrl(request.getProfilePictureUrl());

        userRepository.save(user);
        BankDetails bankDetails =
                bankRepo.findByUserId(user.getId()).orElse(null);
        BankDetailsResponse bankResponse = null;
        if (bankDetails != null) {
            bankResponse = BankDetailsResponse.builder()
                    .id(bankDetails.getId())
                    .userId(bankDetails.getUserId())
                    .bankName(bankDetails.getBankName())
                    .accountNumber(bankDetails.getAccountNumber())
                    .iBanNumber(bankDetails.getIBanNumber())
                    .taxNumber(bankDetails.getTaxNumber())
                    .build();
        }
        BasicRestaurantDetails business = basicRepo.findByRestaurantId(user.getId()).orElse(null);

        ProfileResponse.BusinessInfoResponse businessInfoResponse = null;
        if (business != null) {
            businessInfoResponse = ProfileResponse.BusinessInfoResponse.builder()
                    .businessType(business.getBusinessType())
                    .website(business.getWebsiteDetails()) // Map websiteDetails -> website
                    .build();
        }


        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAddress(),
                user.getPhoneNumber(),
                user.getProfilePictureUrl(),
                bankResponse,
                businessInfoResponse
        );
    }

    // ... existing imports ...

    // 1. UPDATED SUBMIT METHOD
    @Override
    public Map<String, Object> submitFeedback(FeedbackRequest request) {
        try {
            // Changed request.getSenderId() to request.getCustomerId()
            User customer = userRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            Feedback feedback = Feedback.builder()
                    .customer(customer) // Changed .sender(sender) to .customer(customer)
//                    .restaurantId(request.getRestaurantId())
                    .rating(request.getRating())
                    .subject(request.getSubject())
                    .remark(request.getRemark())
                    .createdAt(LocalDateTime.now())
                    .build();

            feedbackRepository.save(feedback);

            return Map.of(
                    "status", "success",
                    "message", "Feedback submitted successfully"
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Error submitting feedback: " + e.getMessage()
            );
        }
    }

    // 2. NEW UNIFIED GET METHOD
    @Override
    public List<FeedbackResponse> getFeedbackByType(Long id, String type) {
        List<Feedback> feedbacks;

        if ("RESTAURANT".equalsIgnoreCase(type)) {
            feedbacks = feedbackRepository.findByRestaurantId(id);
        } else if ("CUSTOMER".equalsIgnoreCase(type)) {
            feedbacks = feedbackRepository.findByCustomerId(id);
        } else {
            throw new RuntimeException("Invalid type. Use 'RESTAURANT' or 'CUSTOMER'");
        }

        return feedbacks.stream().map(this::mapToFeedbackResponse).collect(Collectors.toList());
    }

    // 3. UPDATED MAPPER
    private FeedbackResponse mapToFeedbackResponse(Feedback f) {
        return FeedbackResponse.builder()
                .id(f.getId())
                .customerName(f.getCustomer().getFullName())            // Changed from sender

                .restaurantId(f.getRestaurantId())
                .rating(f.getRating())
                .subject(f.getSubject())
                .remark(f.getRemark())
                .createdAt(f.getCreatedAt())
                .build();
    }


    @Override
    public BankDetailsResponse updateBankDetails(Long userId, UpdateBankDetailsRequest request) {
        // 1. Verify User exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Find existing bank details OR create new ones
        BankDetails bankDetails = bankRepo.findByUserId(user.getId())
                .orElse(BankDetails.builder()
                        .userId(user.getId())
                        .build());

        // 3. Update fields if they are not null
        if (request.getBankName() != null) {
            bankDetails.setBankName(request.getBankName());
        }
        if (request.getAccountNumber() != null) {
            bankDetails.setAccountNumber(request.getAccountNumber());
        }
        if (request.getIBanNumber() != null) {
            bankDetails.setIBanNumber(request.getIBanNumber());
        }
        if (request.getTaxNumber() != null) {
            bankDetails.setTaxNumber(request.getTaxNumber());
        }

        // 4. Save to DB
        BankDetails savedBank = bankRepo.save(bankDetails);

        // 5. Return Response
        return BankDetailsResponse.builder()
                .id(savedBank.getId())
                .userId(savedBank.getUserId())
                .bankName(savedBank.getBankName())
                .accountNumber(savedBank.getAccountNumber())
                .iBanNumber(savedBank.getIBanNumber())
                .taxNumber(savedBank.getTaxNumber())
                .build();
    }
    @Override
    public Map<String, Object> updateBusinessInfo(Long userId, UpdateBusinessInfoRequest request) {
        // Use basicRepo (which you already have injected)
        // Note: Using findByRestaurantId because your Entity uses 'restaurantId'
        BasicRestaurantDetails details = basicRepo.findByRestaurantId(userId)
                .orElse(BasicRestaurantDetails.builder()
                        .restaurantId(userId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        // Update fields if they are present in request
        if (request.getBusinessType() != null) {
            details.setBusinessType(request.getBusinessType());
        }
        if (request.getWebsite() != null) {
            details.setWebsiteDetails(request.getWebsite()); // Map website -> websiteDetails
        }

        basicRepo.save(details);

        return Map.of("status", "success", "message", "Business info updated successfully");
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

    public Map<String,Object> changePassword(String email, String newPassword){
        try{
            User user = userRepository.findByEmail(email)
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

    @Transactional
    @Override
    public Map<String, Object> registerRestaurant(
            RestaurantRegistrationRequest request,
            MultipartFile profileImage
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return error("Email is required");
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                return error("Email is already registered");
            }

            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                return error("Phone number already registered");
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return error("Password must be at least 6 characters");
            }

            String profileImageUrl = null;
            if (profileImage != null && !profileImage.isEmpty()) {
                profileImageUrl = notificationFeignClientService.uploadImage("profile",profileImage);
            }

            User user = User.builder()
                    .userType(UserType.RESTAURANT)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .userName(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .subscriptionPlanId(request.getSubscriptionPlanId())
                    .address(request.getAddress())
                    .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                    .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                    .profilePictureUrl(profileImageUrl)
                    .accountStatus(AccountStatus.active)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .build();

            User savedUser = userRepository.save(user);

            BasicRestaurantDetails basic = BasicRestaurantDetails.builder()
                    .restaurantId(savedUser.getId())
                    .speciality(request.getBasicDetails().getSpeciality())
                    .websiteDetails(request.getBasicDetails().getWebsiteDetails())
                    .cuisine(request.getBasicDetails().getCuisine())
                    .build();

            basicRepo.save(basic);


            // ---------------- BANK DETAILS ----------------
            BankDetails bank = BankDetails.builder()
                    .userId(savedUser.getId())
                    .bankName(request.getBankDetails().getBankName())
                    .accountNumber(request.getBankDetails().getAccountNumber())
                    .taxNumber(request.getBankDetails().getTaxNumber())
                    .build();

            bankRepo.save(bank);


            // ---------------- SOCIAL MEDIA LINKS ----------------
            for (RestaurantRegistrationRequest.SocialMediaRequest sm : request.getSocialMediaList()) {
                SocialMediaDetails media = SocialMediaDetails.builder()
                        .restaurantId(savedUser.getId())
                        .socialMediaType(sm.getSocialMediaType())
                        .link(sm.getLink())
                        .build();
                socialRepo.save(media);
            }


            // ---------------- RESPONSE DTO ----------------
            RestaurantRegisterResponse data = RestaurantRegisterResponse.builder()
                    .restaurantId(savedUser.getId())
                    .name(savedUser.getFullName())
                    .email(savedUser.getEmail())
                    .phoneNumber(savedUser.getPhoneNumber())
                    .profileImageUrl(savedUser.getProfilePictureUrl())
                    .build();

            return Map.of("message","Restaurant registered successfully","restaurantRegistrationData" ,data,"status","success");

        } catch (Exception e) {
            return error("Something went wrong: " + e.getMessage());
        }
    }


    private Map<String, Object> error(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "error");
        map.put("message", message);
        map.put("data", null);
        return map;
    }


    @Transactional
    @Override
    public Map<String, Object> registerUserWithBankDetails(
            RestaurantRegistrationRequest request,
            MultipartFile profileImage
    ) {

        try {

            // ---------------- VALIDATIONS ----------------
            if (request == null) {
                throw new IllegalArgumentException("Request body is missing");
            }

            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalStateException("Email already registered");
            }

            if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                throw new IllegalArgumentException("Phone number is required");
            }

            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new IllegalStateException("Phone number already registered");
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }

//            if (request.getBankDetails() == null) {
//                throw new IllegalArgumentException("Bank details are required");
//            }

            // ---------------- PROFILE IMAGE UPLOAD ----------------
            String profileImageUrl = null;

            if (profileImage != null && !profileImage.isEmpty()) {

                if (profileImage.getContentType() == null ||
                        !profileImage.getContentType().startsWith("image/")) {
                    throw new IllegalArgumentException("Only image files are allowed");
                }

             if (profileImage != null && !profileImage.isEmpty()) {
                profileImageUrl = notificationFeignClientService.uploadImage("profile",profileImage);
                System.err.println("profileImageUrl = " + profileImageUrl);
            }

            }
            LocalDate dob = null;
            if (request.getDateOfBirth() != null) {
                dob = LocalDate.parse(request.getDateOfBirth());
            }


            // ---------------- CREATE USER ----------------
            User user = User.builder()
                    .userType(UserType.USER)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .userName(request.getEmail())
                    .customerId(generateUniqueCustomerId(request.getFullName()))
                    .phoneNumber(request.getPhoneNumber())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .subscriptionPlanId(request.getSubscriptionPlanId())
                    .dateOfBirth(dob)
                    .address(request.getAddress())
                    .latitude(request.getLatitude() != null
                            ? BigDecimal.valueOf(request.getLatitude())
                            : null)
                    .longitude(request.getLongitude() != null
                            ? BigDecimal.valueOf(request.getLongitude())
                            : null)
                    .profilePictureUrl(profileImageUrl)
                    .accountStatus(AccountStatus.active)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .build();

            User savedUser = userRepository.save(user);

//            // ---------------- CREATE BANK DETAILS ----------------
            if( request.getBankDetails() != null) {
                RestaurantRegistrationRequest.BankDetailsRequest bankReq =
                        request.getBankDetails();

                BankDetails bankDetails = BankDetails.builder()
                        .userId(savedUser.getId())
                        .bankName(bankReq.getBankName())
                        .accountNumber(bankReq.getAccountNumber())
                        .iBanNumber(bankReq.getIBanNumber())
                        .taxNumber(bankReq.getTaxNumber())
                        .build();

                bankRepo.save(bankDetails);
            }


            // ---------------- SUCCESS RESPONSE ----------------
            Map<String, Object> success = new HashMap<>();
            success.put("status", "success");
            success.put("message", "User registered successfully with bank details");
            success.put("userId", savedUser.getId());
            success.put("profileImageUrl", profileImageUrl);

            return success;

        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Known validation / business errors
            return error(ex.getMessage());

        } catch (Exception ex) {
            // Log full error internally
            return error(ex.getMessage());
        }
    }



    public Map<String, Object> getActiveRestaurantsMap(Pageable pageable) {
        Map<String, Object> response = new HashMap<>();
        try {
            Page<User> restaurants = userRepository.findByUserTypeAndAccountStatus(
                    UserType.RESTAURANT,
                    AccountStatus.active,
                    pageable
            );

            // Map Users to RestaurantBasicDetailsResponse
            List<RestaurantBasicDetailsResponse> data = restaurants.stream()
                    .map(user -> new RestaurantBasicDetailsResponse(
                            user.getId(),
                            user.getFullName(),
                            user.getAddress(),
                            user.getPhoneNumber(),
                            user.getEmail(),
                            user.getProfilePictureUrl(),
                            0 // container count, can update dynamically later
                    ))
                    .collect(Collectors.toList());

            // Prepare response map
            response.put("status", "success");
            response.put("restaurantData", data);
            response.put("page", restaurants.getNumber());
            response.put("size", restaurants.getSize());
            response.put("totalElements", restaurants.getTotalElements());
            response.put("totalPages", restaurants.getTotalPages());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to fetch active restaurants");
            response.put("details", e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, Object> getActiveCustomersMap(Pageable pageable) {
        Map<String, Object> response = new HashMap<>();
        try {
            Page<User> restaurants = userRepository.findByUserTypeAndAccountStatus(
                    UserType.USER,
                    AccountStatus.active,
                    pageable
            );

            // Map Users to RestaurantBasicDetailsResponse
            List<CustomerDetailsBasic> data = restaurants.stream()
                    .map(user -> new CustomerDetailsBasic(
                            user.getId(),
                            user.getEmail(),              // email
                            user.getPhoneNumber(),        // mobile
                            user.getFullName(),           // fullName
                            user.getProfilePictureUrl(),  // profileImage
                            0,                            // borrowedCount
                            0,                            // returnedCount
                            0                             // pendingCount
                    ))
                    .collect(Collectors.toList());


            // Prepare response map
            response.put("status", "success");
            response.put("customersData", data);
            response.put("page", restaurants.getNumber());
            response.put("size", restaurants.getSize());
            response.put("totalElements", restaurants.getTotalElements());
            response.put("totalPages", restaurants.getTotalPages());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to fetch active customers");
            response.put("details", e.getMessage());
        }
        return response;
    }

    @Override
    public List<RestaurantRegisterResponse> getAllActiveRestaurantsByListOfIds(List<Long> restaurantIds) {
        return userRepository.findRestaurantsByIds(restaurantIds,UserType.RESTAURANT,AccountStatus.active);
    }


    public Map<String, Object> searchRestaurants(String keyword, double currentLat, double currentLon) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Input validation
            if (keyword == null || keyword.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Search keyword cannot be empty");
                response.put("searchData", Collections.emptyList());
                return response;
            }

            // Fetch restaurants from repository
            List<User> restaurants = userRepository.searchRestaurantsByKeyword(keyword);

            if (restaurants.isEmpty()) {
                response.put("status", "success");
                response.put("message", "No restaurants found for the given keyword");
                response.put("searchData", Collections.emptyList());
                return response;
            }

            // Map restaurants to response DTO with distance
            List<RestaurantSearchResponse> restaurantList = restaurants.stream()
                    .map(r -> {
                        double distanceKm = 0.0;
                        BigDecimal lat = r.getLatitude();
                        BigDecimal lon = r.getLongitude();

                        if (lat != null && lon != null) {
                            try {
                                distanceKm = DistanceUtil.calculateDistance(
                                        currentLat, currentLon, lat.doubleValue(), lon.doubleValue()
                                );
                            } catch (Exception e) {
                                // Ignore distance calculation errors, distance will remain 0
                            }
                        }
                        // If lat/lon is null, distance will remain 0 (or you can set to -1 if you want)

                        return new RestaurantSearchResponse(
                                r.getId(),
                                r.getFullName(),
                                r.getAddress(),
                                r.getLatitude(),
                                r.getLongitude(),
                                distanceKm,
                                r.getProfilePictureUrl()
                        );
                    })
                    .sorted(Comparator.comparingDouble(RestaurantSearchResponse::getDistanceKm))
                    .collect(Collectors.toList());


            // Prepare map response
            response.put("status", "success");
            response.put("message", "Restaurants fetched successfully");
            response.put("searchData", restaurantList);
            return response;

        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Unable to search restaurants at the moment. Please try again later.");
            response.put("searchData", Collections.emptyList());
            return response;
        }
    }

    @Override
    public Map<String, Object> getUserById(Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (userId == null) {
                response.put(AuthConstant.STATUS, AuthConstant.ERROR);
                response.put(AuthConstant.MESSAGE, "User ID is required");
                return Map.of();
            }
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put(AuthConstant.STATUS, AuthConstant.SUCCESS);
                response.put(AuthConstant.MESSAGE, "User details fetched successfully");
                response.put(AuthConstant.DATA, user);
                return response;
            }
            response.put(AuthConstant.STATUS, AuthConstant.ERROR);
            response.put(AuthConstant.MESSAGE, "User not found");
            return response;
        } catch (Exception e) {
            response.put(AuthConstant.STATUS, AuthConstant.ERROR);
            response.put(AuthConstant.MESSAGE, "Failed to fetch user details");
            response.put(AuthConstant.DETAILS, e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, Object> upgradeUserSubscription(SubscriptionRequest subscriptionRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (subscriptionRequest.getUserId() == null) {
                response.put(AuthConstant.STATUS, AuthConstant.ERROR);
                response.put(AuthConstant.MESSAGE, "User ID is required");
                return response;
            }
            if (subscriptionRequest.getSubscriptionPlanId() == null) {
                response.put(AuthConstant.STATUS, AuthConstant.ERROR);
                response.put(AuthConstant.MESSAGE, "Subscription Plan ID is required");
                return response;
            }
            Optional<User> userOpt = userRepository.findById(subscriptionRequest.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setSubscriptionPlanId(subscriptionRequest.getSubscriptionPlanId());
                userRepository.save(user);
                response.put(AuthConstant.STATUS, AuthConstant.SUCCESS);
                response.put(AuthConstant.MESSAGE, "User subscription updated successfully");
                return response;
            }
            response.put(AuthConstant.STATUS, AuthConstant.ERROR);
            response.put(AuthConstant.MESSAGE, "User not found");
            return response;

        } catch (Exception e) {
            response.put(AuthConstant.STATUS, AuthConstant.ERROR);
            response.put(AuthConstant.MESSAGE, "Failed to update user subscription");
            response.put(AuthConstant.DETAILS, e.getMessage());
        }
        return response;
    }


    public String generateUniqueCustomerId(String fullName) {

        // Remove spaces and take first 4 characters
        String cleanedName = fullName.replaceAll("\\s+", "");
        String namePart = cleanedName.length() >= 4
                ? cleanedName.substring(0, 4).toUpperCase()
                : cleanedName.toUpperCase();

        // Date in DDMMYY format
        LocalDate today = LocalDate.now();
        String datePart = String.format("%02d%02d%02d",
                today.getDayOfMonth(),
                today.getMonthValue(),
                today.getYear() % 100
        );

        // Base ID
        String baseId = namePart + datePart;

        // Fetch existing IDs from DB
        List<String> existingIds = userRepository.findCustomerIdStartingWith(baseId);

        if (existingIds.isEmpty()) {
            return baseId;
        }

        // Find next available counter
        int counter = 1;
        String newId;

        do {
            newId = String.format("%s_%02d", baseId, counter);
            counter++;
        } while (existingIds.contains(newId));

        return newId;
    }


}
