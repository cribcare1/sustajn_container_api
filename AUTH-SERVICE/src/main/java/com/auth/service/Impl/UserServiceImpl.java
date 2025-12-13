package com.auth.service.Impl;

import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.UserType;
import com.auth.model.*;
import com.auth.repository.BankDetailsRepository;
import com.auth.repository.BasicRestaurantDetailsRepository;
import com.auth.repository.SocialMediaDetailsRepository;
import com.auth.repository.UserRepository;
import com.auth.request.RestaurantRegistrationRequest;
import com.auth.response.LoginResponse;
import com.auth.response.RestaurantRegisterResponse;
import com.auth.service.UserService;
import com.auth.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final BasicRestaurantDetailsRepository basicRepo;
    private final BankDetailsRepository bankRepo;
    private final SocialMediaDetailsRepository socialRepo;
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
                //profileImageUrl = fileUploadService.uploadImage(profileImage);
                profileImageUrl=null;
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
                    .restaurantId(savedUser.getId())
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
}
