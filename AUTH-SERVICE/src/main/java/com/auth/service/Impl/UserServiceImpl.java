package com.auth.service.Impl;

import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.UserType;
import com.auth.feignClient.service.NotificationFeignClientService;
import com.auth.model.*;
import com.auth.repository.BankDetailsRepository;
import com.auth.repository.BasicRestaurantDetailsRepository;
import com.auth.repository.SocialMediaDetailsRepository;
import com.auth.repository.UserRepository;
import com.auth.request.RestaurantRegistrationRequest;
import com.auth.response.CustomerDetailsBasic;
import com.auth.response.LoginResponse;
import com.auth.response.RestaurantBasicDetailsResponse;
import com.auth.response.RestaurantRegisterResponse;
import com.auth.service.UserService;
import com.auth.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
//    private final  fileUploadService;
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

    public UserDto saveUser(User user){
        user.setUserName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return new UserDto(savedUser.getId(),
                savedUser.getUserName(),
                savedUser.getEmail(),
                savedUser.getUserType().name());
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

            // ---------------- VALIDATIONS ----------------

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


            // ---------------- UPLOAD PROFILE IMAGE ----------------
            String profileImageUrl = null;

            if (profileImage != null && !profileImage.isEmpty()) {
                profileImageUrl = notificationFeignClientService.uploadImage("profile",profileImage);
                System.err.println("profileImageUrl = " + profileImageUrl);
            }


            // ---------------- CREATE USER ----------------
            User user = User.builder()
                    .userType(UserType.RESTAURANT)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .userName(request.getEmail())    // Email acts as username
                    .phoneNumber(request.getPhoneNumber())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
//                    .dateOfBirth(request.getDateOfBirth())
                    .address(request.getAddress())
                    .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                    .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                    .profilePictureUrl(profileImageUrl)
                    .accountStatus(AccountStatus.active)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .build();

            User savedUser = userRepository.save(user);


            // ---------------- BASIC DETAILS ----------------
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
                    .phoneNumber(request.getPhoneNumber())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
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

}
