package com.auth.service.Impl;

import com.auth.constant.AuthConstant;
import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.UserType;
import com.auth.exception.ResourceNotFoundException;
import com.auth.feignClient.InventoryFeignClient;
import com.auth.feignClient.service.NotificationFeignClientService;
import com.auth.model.*;
import com.auth.repository.*;
import com.auth.request.*;
import com.auth.response.*;
import com.auth.response.CustomerDetailsBasic;
import com.auth.response.LoginResponse;
import com.auth.response.RestaurantBasicDetailsResponse;
import com.auth.request.UpdateBusinessInfoRequest;
import com.auth.response.RestaurantRegisterResponse;
import com.auth.response.ProfileResponse;
import com.auth.response.BankDetailsResponse;
import com.auth.response.FeedbackResponse;
import com.auth.service.UserService;
import com.auth.util.AuthUtil;
import com.auth.util.DistanceUtil;
import com.auth.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final AddressRepository addressRepository;
    private final InventoryFeignClient inventoryFeignClient;

    @Value("${image.storage.root-path}")
    private String userProfilePath;

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
        );
    }


    private LoginResponse generateTokenWithLoginDetails(User user) {

        String token = jwtUtil.generateToken(user.getUserName());

        return new LoginResponse(
                user.getId(),
                user.getProfilePictureUrl(),
                user.getUserType().name(),
                user.getEmail(),
                user.getAddress(),
                user.getFullName(),
                token,
                "Bearer"   // token type
        );
    }

    @Override
    public UserDto saveUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setUserName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        user.setAccountStatus(AccountStatus.ACTIVE);
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


        if (request.getPhoneNumber() != null)
            user.setPhoneNumber(request.getPhoneNumber());


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
            // 1. Find the User (Works for both Customer and Restaurant)
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. Build Feedback
            // Note: We are saving the user in the 'customer' field of the Feedback entity
            // because 'customer' is a User object. It acts as the "Submitter".
            Feedback feedback = Feedback.builder()
                    .customer(user)
                    .restaurantId(null) // No specific target restaurant (App Feedback)
                    .rating(request.getRating())
                    .subject(request.getSubject())
                    .remark(request.getRemark())
                    .createdAt(LocalDateTime.now())
                    .build();

            // Optional: You can add logic here if you want to store "Type" in feedback
            // e.g. feedback.setSubmitterType(user.getUserType().name());

            feedbackRepository.save(feedback);

            return Map.of(
                    "status", "success",
                    "message", user.getUserType() + " feedback submitted successfully"
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
    @Transactional
    public ApiResponse<BankDetails> updateBankDetails(BankCardPaymentGetWayDetailsRequest request) {
        try {

            BankDetails bankDetails = null;

            // ========== 🏦 BANK DETAILS ==========
            if (request.getBankDetailsRequest() != null) {
                var r = request.getBankDetailsRequest();

                BankDetails bankRow = (r.getId() != null)
                        ? bankRepo.findById(r.getId()).orElse(new BankDetails())
                        : new BankDetails();

                bankRow.setUserId(r.getUserId());

                Optional.ofNullable(r.getBankName()).ifPresent(bankRow::setBankName);
                Optional.ofNullable(r.getAccountNumber()).ifPresent(bankRow::setAccountNumber);
                Optional.ofNullable(r.getIBanNumber()).ifPresent(bankRow::setIBanNumber);
                Optional.ofNullable(r.getTaxNumber()).ifPresent(bankRow::setTaxNumber);

                bankRow.setStatus("ACTIVE");

                bankDetails = bankRepo.save(bankRow);
            }

            // ========== 💳 CARD DETAILS ==========
            if (request.getCardDetailsRequest() != null) {
                var r = request.getCardDetailsRequest();

                BankDetails cardRow = (r.getId() != null)
                        ? bankRepo.findById(r.getId()).orElse(new BankDetails())
                        : new BankDetails();

                cardRow.setUserId(r.getUserId());

                Optional.ofNullable(r.getCardHolderName()).ifPresent(cardRow::setCardHolderName);
                Optional.ofNullable(r.getCardNumber()).ifPresent(cardRow::setCardNumber);
                Optional.ofNullable(r.getExpiryDate()).ifPresent(cardRow::setExpiryDate);
                Optional.ofNullable(r.getCvv()).ifPresent(cardRow::setCvv);

                cardRow.setStatus("ACTIVE");

                bankDetails = bankRepo.save(cardRow);
            }

            // ========== 🧾 PAYMENT GATEWAY ==========
            if (request.getPaymentGetWayRequest() != null) {
                var r = request.getPaymentGetWayRequest();

                BankDetails payRow = (r.getId() != null)
                        ? bankRepo.findById(r.getId()).orElse(new BankDetails())
                        : new BankDetails();

                payRow.setUserId(r.getUserId());

                Optional.ofNullable(r.getPaymentGatewayId()).ifPresent(payRow::setPaymentGatewayId);
                Optional.ofNullable(r.getPaymentGatewayName()).ifPresent(payRow::setPaymentGatewayName);

                payRow.setStatus("ACTIVE");

                bankDetails = bankRepo.save(payRow);
            }

            return new ApiResponse<>("Details updated successfully", AuthConstant.SUCCESS, bankDetails);

        } catch (Exception e) {
            return new ApiResponse<>("Error on updating details", AuthConstant.ERROR, null);
        }
    }

    @Override
    public ApiResponse<CustomerProfileResponse> getCustomerProfileDetails(Long userId) {

        try {
            List<Object[]> profileResultRows = userRepository.getCustomerProfileDetailsByUserId(userId);

            if (CollectionUtils.isEmpty(profileResultRows)) {
                return new ApiResponse<>(AuthConstant.ERROR, "Customer not found", null);
            }

            Object[] baseProfileRow = profileResultRows.get(0);

            CustomerProfileResponse response = new CustomerProfileResponse();

            // 🧍 Basic User Info
            response.setId((Long) baseProfileRow[0]);
            response.setFullName((String) baseProfileRow[1]);
            response.setMobileNumber((String) baseProfileRow[2]);
            response.setCustomerId((String) baseProfileRow[3]);
            response.setEmailId((String) baseProfileRow[4]);
            response.setProfileImageUrl((String) baseProfileRow[5]);
            response.setSubscriptionPlanId((Integer) baseProfileRow[6]);

            // 🏦 Bank Details
            if (baseProfileRow[7] != null) {
                response.setBankDetailsResponse(new BankDetailsResponse(
                        (Long) baseProfileRow[7],
                        userId,
                        (String) baseProfileRow[8],
                        (String) baseProfileRow[9],
                        (String) baseProfileRow[10],
                        (String) baseProfileRow[11]
                ));
            }

            // 💳 Card Details
            if (baseProfileRow[12] != null) {
                response.setCardDetailsResponse(new CardDetailsResponse(
                        (Long) baseProfileRow[12],
                        (String) baseProfileRow[13],
                        (String) baseProfileRow[14],
                        (String) baseProfileRow[15]
                ));
            }

            // 🧾 Payment Gateway
            if (baseProfileRow[16] != null) {
                response.setPaymentGetWayResponse(new PaymentGetWayResponse(
                        (Long) baseProfileRow[16],
                        (String) baseProfileRow[17],
                        (String) baseProfileRow[18]
                ));
            }

            // 🏠 Addresses
            List<AddressResponse> addressList = new ArrayList<>();
            for (Object[] row : profileResultRows) {
                if (row[19] != null) {
                    addressList.add(new AddressResponse(
                            (Long) row[19],
                            (String) row[20],
                            (String) row[21],
                            (String) row[22],
                            (String) row[23]
                    ));
                }
            }
            response.setAddressResponses(addressList);

            // 🧾 ================= SUBSCRIPTION =================
            if (response.getSubscriptionPlanId() != null) {

                Map<String, Object> planResp =
                        inventoryFeignClient.getSubscriptionPlanById(response.getSubscriptionPlanId());

                if ("success".equals(planResp.get("status"))) {

                    Map<String, Object> data = (Map<String, Object>) planResp.get("data");

                    SubscriptionResponse subscription = new SubscriptionResponse(
                            (Integer) data.get("planId"),
                            (String) data.get("planName"),
                            (String) data.get("planType"),
                            (String) data.get("description"),
                            (String) data.get("partnerType"),
                            new BigDecimal(data.get("feeType").toString()),
                            new BigDecimal(data.get("depositType").toString()),
                            new BigDecimal(data.get("commissionPercentage").toString()),
                            (Integer) data.get("minContainers"),
                            (Integer) data.get("maxContainers"),
                            (Integer) data.get("totalContainers"),
                            (Boolean) data.get("includesDelivery"),
                            (Boolean) data.get("includesMarketing"),
                            (Boolean) data.get("includesAnalytics"),
                            (String) data.get("billingCycle"),
                            (String) data.get("planStatus")
                    );

                    response.setSubscriptionResponse(subscription);
                }
            }

            return new ApiResponse<>(AuthConstant.SUCCESS,
                    "Customer profile fetched successfully", response);

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(AuthConstant.ERROR,
                    "Failed to fetch customer profile", null);
        }
    }


    private static @NonNull List<AddressResponse> getAddressResponses(List<Object[]> profileResultRows) {
        List<AddressResponse> addresses = new ArrayList<>();
        Set<Long> processedAddressIds = new HashSet<>();

        for (Object[] row : profileResultRows) {

            if (row[14] != null && processedAddressIds.add((Long) row[14])) {

                AddressResponse address = new AddressResponse();
                address.setId((Long) row[14]);
                address.setAddressType((String) row[15]);
                address.setFlatDoorHouseDetails((String) row[16]);
                address.setAreaStreetCityBlockDetails((String) row[17]);
                address.setPoBoxOrPostalCode((String) row[18]);

                addresses.add(address);
            }
        }
        return addresses;
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


    @Override
    public ApiResponse<Address> saveNewAddress(AddressRequest request) {
        // Save Address
        Address address = Address.builder()
                .userId(request.getUserId())
                .addressType(request.getAddressType())
                .flatDoorHouseDetails(request.getFlatDoorHouseDetails())
                .areaStreetCityBlockDetails(request.getAreaStreetCityBlockDetails())
                .poBoxOrPostalCode(request.getPoBoxOrPostalCode())
                .status(AuthConstant.ACTIVE)
                .build();

        addressRepository.save(address);
        return new ApiResponse<>("Address created successfully", AuthConstant.SUCCESS);
    }

    @Override
    public ApiResponse<Address> updateAddress(AddressRequest request) {

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found", AuthConstant.ERROR));

        // Update only non-null fields
        Optional.ofNullable(request.getAddressType()).ifPresent(address::setAddressType);
        Optional.ofNullable(request.getFlatDoorHouseDetails()).ifPresent(address::setFlatDoorHouseDetails);
        Optional.ofNullable(request.getAreaStreetCityBlockDetails()).ifPresent(address::setAreaStreetCityBlockDetails);
        Optional.ofNullable(request.getPoBoxOrPostalCode()).ifPresent(address::setPoBoxOrPostalCode);

        addressRepository.save(address);

        return new ApiResponse<>("Address updated successfully", AuthConstant.SUCCESS);
    }

    @Override
    public ApiResponse<Address> deleteAddress(AddressRequest request) {

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found", AuthConstant.ERROR));

        address.setStatus(AuthConstant.IN_ACTIVE);
        addressRepository.save(address);
        return new ApiResponse<>("Address deleted successfully", AuthConstant.SUCCESS);
    }

    @Override
    public ApiResponse<BankDetails> createBankDetails(
            BankCardPaymentGetWayDetailsRequest request) {

        try {
            BankDetails bankDetails = new BankDetails();

            // ====== BANK DETAILS ======
            if (request.getBankDetailsRequest() != null) {
                BankCardPaymentGetWayDetailsRequest.BankDetailsRequest bankReq =
                        request.getBankDetailsRequest();

                bankDetails.setUserId(bankReq.getUserId());
                bankDetails.setBankName(bankReq.getBankName());
                bankDetails.setTaxNumber(bankReq.getTaxNumber());
                bankDetails.setAccountNumber(bankReq.getAccountNumber());
                bankDetails.setIBanNumber(bankReq.getIBanNumber());
            }

            // ====== CARD DETAILS ======
            if (request.getCardDetailsRequest() != null) {
                BankCardPaymentGetWayDetailsRequest.CardDetailsRequest cardReq =
                        request.getCardDetailsRequest();

                bankDetails.setCardHolderName(cardReq.getCardHolderName());
                bankDetails.setCardNumber(cardReq.getCardNumber());
                bankDetails.setExpiryDate(cardReq.getExpiryDate());
                bankDetails.setCvv(cardReq.getCvv());
                bankDetails.setPaymentGatewayId(cardReq.getPaymentGatewayId());
                bankDetails.setPaymentGatewayName(cardReq.getPaymentGatewayName());
            }

            // ====== PAYMENT GATEWAY DETAILS ======
            if (request.getPaymentGetWayRequest() != null) {
                BankCardPaymentGetWayDetailsRequest.PaymentGetWayRequest payReq =
                        request.getPaymentGetWayRequest();

                bankDetails.setPaymentGatewayId(payReq.getPaymentGatewayId());
                bankDetails.setPaymentGatewayName(payReq.getPaymentGatewayName());
            }

            // ====== DEFAULT STATUS ======
            bankDetails.setStatus("ACTIVE");

            // ====== SAVE ======
            BankDetails saved = bankRepo.save(bankDetails);

            return new ApiResponse<>("Bank details created successfully",
                    AuthConstant.SUCCESS, saved);

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Error on creating bank details",
                    AuthConstant.ERROR, null);
        }
    }

    @Override
    public ApiResponse<BankDetails> deleteBankDetails(Long id) {
        try {
            Optional<BankDetails> bankDetailsOptional = bankRepo.findById(id);
            if (bankDetailsOptional.isPresent()) {
                BankDetails bankDetails = bankDetailsOptional.get();
                bankDetails.setStatus(AuthConstant.IN_ACTIVE);
                bankRepo.save(bankDetails);
                return new ApiResponse<>("Bank details deleted successfully", AuthConstant.SUCCESS, null);
            }
            return new ApiResponse<>("Bank details not found", AuthConstant.ERROR, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Error on deleting bank details",
                    AuthConstant.ERROR, null);
        }
    }

    @Override
    public ApiResponse<CustomerProfileResponse> updateUserProfile(String userData, MultipartFile profileImage) {
        try {
            UpdateProfileRequest request = AuthUtil.convertToJson(userData, UpdateProfileRequest.class);
            if (request == null) {
                return new ApiResponse<>("Please provide valid request", AuthConstant.ERROR, null);
            }

            Optional<User> userOptional = userRepository.findById(request.getUserId());
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                Optional.ofNullable(request.getFullName()).ifPresent(user::setFullName);

                // Validate and update phone number
                if (request.getPhoneNumber() != null) {
                    Optional<User> otherUserOptional =
                            userRepository.findByPhoneNumber(request.getPhoneNumber());

                    if (otherUserOptional.isPresent()
                            && !otherUserOptional.get().getId().equals(user.getId())) {

                        return new ApiResponse<>("Phone number already in use by another user",
                                AuthConstant.ERROR, null);
                    }

                    user.setPhoneNumber(request.getPhoneNumber());
                }

                // save profile image if present
                String profileImageUrl = null;
                if (profileImage != null && !profileImage.isEmpty()) {
                    profileImageUrl = notificationFeignClientService.uploadImage("profile", profileImage);
                    user.setProfilePictureUrl(profileImageUrl);
                }

                User updatedUser = userRepository.save(user);

                ApiResponse<CustomerProfileResponse> customerProfileResponse = getCustomerProfileDetails(updatedUser.getId());

                return new ApiResponse<>("User profile updated successfully", AuthConstant.SUCCESS, customerProfileResponse.getData());
            }

            return new ApiResponse<>("User not found", AuthConstant.ERROR, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Error on updating profile details",
                    AuthConstant.ERROR, null);
        }
    }


    public Map<String, Object> changePassword(Long userId, String newPassword) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return Map.of(
                    "message", "Password changed successfully",
                    "status", "success"
            );
        } catch (Exception e) {
            return Map.of(
                    "message", "Error changing password: " + e.getMessage(),
                    "status", "error"
            );
        }
    }

    public Map<String, Object> changePassword(String email, String newPassword) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return Map.of(
                    "message", "Password changed successfully",
                    "status", "success"
            );
        } catch (Exception e) {
            return Map.of(
                    "message", "Error changing password: " + e.getMessage(),
                    "status", "error"
            );
        }
    }

    @Transactional
    @Override
    public Map<String, Object> registerRestaurant(
            RestaurantRegistrationRequest request
    ) {
        try {

            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return error("Email is required");
            }

            if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                return error("Phone number is required");
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return error("Password must be at least 6 characters");
            }

            // 🔹 FETCH USER ONCE
            User existingUser = userRepository
                    .findByEmail(request.getEmail())
                    .orElse(null);

            // 🔹 EMAIL ALREADY EXISTS
            if (existingUser != null) {

                // ⛔ PENDING
                if (existingUser.getAccountStatus() == AccountStatus.PENDING) {
                    return error("Your registration is already pending for approval");
                }

                // 🔁 REJECTED → UPDATE SAME USER
                if (existingUser.getAccountStatus() == AccountStatus.REJECTED) {

                    // Phone number uniqueness (exclude same user)
                    if (userRepository.existsByPhoneNumberAndIdNot(
                            request.getPhoneNumber(),
                            existingUser.getId())) {
                        return error("Phone number already registered");
                    }

                    // 🔄 UPDATE USER
                    existingUser.setFullName(request.getFullName());
                    existingUser.setPhoneNumber(request.getPhoneNumber());
                    existingUser.setPasswordHash(
                            passwordEncoder.encode(request.getPassword())
                    );
                    existingUser.setLatitude(
                            request.getLatitude() != null
                                    ? BigDecimal.valueOf(request.getLatitude())
                                    : null
                    );
                    existingUser.setLongitude(
                            request.getLongitude() != null
                                    ? BigDecimal.valueOf(request.getLongitude())
                                    : null
                    );
                    existingUser.setAccountStatus(AccountStatus.PENDING);
                    existingUser.setEmailVerified(false);
                    existingUser.setPhoneVerified(false);

                    User savedUser = userRepository.save(existingUser);

                    // 🔥 DELETE OLD DATA
                    basicRepo.deleteByRestaurantId(savedUser.getId());
                    addressRepository.deleteByUserId(savedUser.getId());
                    bankRepo.deleteByUserId(savedUser.getId());
                    socialRepo.deleteByRestaurantId(savedUser.getId());

                    // 🔁 RE-SAVE DETAILS (same logic as create)
                    saveAllRestaurantDetails(savedUser, request);

                    return Map.of(
                            "status", "success",
                            "message", "Registration updated successfully and sent for approval"
                    );
                }

                // ⛔ ACTIVE / APPROVED
                return error("Email is already registered");
            }

            // ================= NEW REGISTRATION FLOW =================

            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                return error("Phone number already registered");
            }

            User user = User.builder()
                    .userType(UserType.RESTAURANT)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .userName(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .subscriptionPlanId(request.getSubscriptionPlanId())
                    .latitude(request.getLatitude() != null
                            ? BigDecimal.valueOf(request.getLatitude())
                            : null)
                    .longitude(request.getLongitude() != null
                            ? BigDecimal.valueOf(request.getLongitude())
                            : null)
                    .accountStatus(AccountStatus.PENDING)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .build();

            User savedUser = userRepository.save(user);

            saveAllRestaurantDetails(savedUser, request);

            LoginResponse loginResponse =
                    generateTokenWithLoginDetails(savedUser);
            if (request.getBasicDetails() != null) {
                BasicRestaurantDetails basic = BasicRestaurantDetails.builder()
                        .restaurantId(savedUser.getId())
                        .speciality(request.getBasicDetails().getSpeciality())
                        .websiteDetails(request.getBasicDetails().getWebsiteDetails())
                        .cuisine(request.getBasicDetails().getCuisine())
                        .build();
                basicRepo.save(basic);
            }


            // ---------------- CREATE ADDRESS DETAILS ----------------
            if (request.getAddress() != null) {
                RestaurantRegistrationRequest.AddressRequest addressReq = request.getAddress();

                // You can create an AddressDetails entity and save it if needed
                Address addressDetails = Address.builder()
                        .userId(savedUser.getId())
                        .addressType(addressReq.getAddressType())
                        .flatDoorHouseDetails(addressReq.getFlatDoorHouseDetails())
                        .areaStreetCityBlockDetails(addressReq.getAreaStreetCityBlockDetails())
                        .poBoxOrPostalCode(addressReq.getPoBoxOrPostalCode())
                        .status(AuthConstant.ACTIVE)
                        .build();
                addressRepository.save(addressDetails);
            }

            return Map.of(
                    "status", "success",
                    "message", "Restaurant registered successfully",
                    "data", loginResponse
            );

        } catch (Exception e) {
            return error("Something went wrong: " + e.getMessage());
        }
    }

    private void saveAllRestaurantDetails(
            User savedUser,
            RestaurantRegistrationRequest request
    ) {

        BasicRestaurantDetails basic = BasicRestaurantDetails.builder()
                .restaurantId(savedUser.getId())
                .speciality(request.getBasicDetails().getSpeciality())
                .websiteDetails(request.getBasicDetails().getWebsiteDetails())
                .cuisine(request.getBasicDetails().getCuisine())
                .build();
        basicRepo.save(basic);

        if (request.getAddress() != null) {
            var addressReq = request.getAddress();
            Address address = Address.builder()
                    .userId(savedUser.getId())
                    .addressType(addressReq.getAddressType())
                    .flatDoorHouseDetails(addressReq.getFlatDoorHouseDetails())
                    .areaStreetCityBlockDetails(addressReq.getAreaStreetCityBlockDetails())
                    .poBoxOrPostalCode(addressReq.getPoBoxOrPostalCode())
                    .status(AuthConstant.ACTIVE)
                    .build();
            addressRepository.save(address);
        }

        if (request.getBankDetails() != null) {
            var bankReq = request.getBankDetails();
            bankRepo.save(
                    BankDetails.builder()
                            .userId(savedUser.getId())
                            .bankName(bankReq.getBankName())
                            .accountNumber(bankReq.getAccountNumber())
                            .iBanNumber(bankReq.getIBanNumber())
                            .taxNumber(bankReq.getTaxNumber())
                            .status(AuthConstant.ACTIVE)
                            .build()
            );
        }

        if (request.getCardDetails() != null) {
            var cardReq = request.getCardDetails();
            bankRepo.save(
                    BankDetails.builder()
                            .userId(savedUser.getId())
                            .cardHolderName(cardReq.getCardHolderName())
                            .cardNumber(cardReq.getCardNumber())
                            .expiryDate(cardReq.getExpiryDate())
                            .cvv(passwordEncoder.encode(cardReq.getCvv()))
                            .status(AuthConstant.ACTIVE)
                            .build()
            );
        }

        if (request.getPaymentGetWay() != null) {
            var payReq = request.getPaymentGetWay();
            bankRepo.save(
                    BankDetails.builder()
                            .userId(savedUser.getId())
                            .paymentGatewayId(payReq.getPaymentGatewayId())
                            .paymentGatewayName(payReq.getPaymentGatewayName())
                            .status(AuthConstant.ACTIVE)
                            .build()
            );
        }

        if (request.getSocialMediaList() != null) {
            for (var sm : request.getSocialMediaList()) {
                socialRepo.save(
                        SocialMediaDetails.builder()
                                .restaurantId(savedUser.getId())
                                .socialMediaType(sm.getSocialMediaType())
                                .link(sm.getLink())
                                .build()
                );
            }
            // ---------------- SOCIAL MEDIA LINKS ----------------
            if (request.getSocialMediaList() != null) {
                for (RestaurantRegistrationRequest.SocialMediaRequest sm : request.getSocialMediaList()) {
                    SocialMediaDetails media = SocialMediaDetails.builder()
                            .restaurantId(savedUser.getId())
                            .socialMediaType(sm.getSocialMediaType())
                            .link(sm.getLink())
                            .build();
                    socialRepo.save(media);
                }
            }



            // ---------------- RESPONSE DTO ----------------
            LoginResponse loginResponse = generateTokenWithLoginDetails(savedUser);

            return Map.of("message", "Restaurant registered successfully", "data", loginResponse, "status", "success");

        } catch (Exception e) {
            e.printStackTrace();
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
            RestaurantRegistrationRequest request
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
                    .dateOfBirth(request.getDateOfBirth())
                    .latitude(request.getLatitude() != null
                            ? BigDecimal.valueOf(request.getLatitude())
                            : null)
                    .longitude(request.getLongitude() != null
                            ? BigDecimal.valueOf(request.getLongitude())
                            : null)
                    .accountStatus(AccountStatus.ACTIVE)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .build();

            User savedUser = userRepository.save(user);


            // ---------------- CREATE ADDRESS DETAILS ----------------
            if (request.getAddress() != null) {
                RestaurantRegistrationRequest.AddressRequest addressReq = request.getAddress();

                // You can create an AddressDetails entity and save it if needed
                Address addressDetails = Address.builder()
                        .userId(savedUser.getId())
                        .addressType(addressReq.getAddressType())
                        .flatDoorHouseDetails(addressReq.getFlatDoorHouseDetails())
                        .areaStreetCityBlockDetails(addressReq.getAreaStreetCityBlockDetails())
                        .poBoxOrPostalCode(addressReq.getPoBoxOrPostalCode())
                        .status(AuthConstant.ACTIVE)
                        .build();
                addressRepository.save(addressDetails);
            }

            // ---------------- CREATE BANK DETAILS ----------------
            if (request.getBankDetails() != null) {
                RestaurantRegistrationRequest.BankDetailsRequest bankReq =
                        request.getBankDetails();

                BankDetails bankDetails = BankDetails.builder()
                        .userId(savedUser.getId())
                        .bankName(bankReq.getBankName())
                        .accountNumber(bankReq.getAccountNumber())
                        .iBanNumber(bankReq.getIBanNumber())
                        .taxNumber(bankReq.getTaxNumber())
                        .status(AuthConstant.ACTIVE)
                        .build();

                bankRepo.save(bankDetails);
            }

            if (request.getCardDetails() != null) {
                RestaurantRegistrationRequest.CardDetailsRequest cardReq =
                        request.getCardDetails();
                BankDetails bankDetails = BankDetails.builder()
                        .userId(savedUser.getId())
                        .cardHolderName(cardReq.getCardHolderName())
                        .cardNumber(cardReq.getCardNumber())
                        .expiryDate(cardReq.getExpiryDate())
                        .cvv(passwordEncoder.encode(cardReq.getCvv()))
                        .status(AuthConstant.ACTIVE)
                        .build();
                bankRepo.save(bankDetails);
            }

            if (request.getPaymentGetWay() != null) {
                RestaurantRegistrationRequest.PaymentGetWayRequest payReq =
                        request.getPaymentGetWay();
                BankDetails bankDetails = BankDetails.builder()
                        .userId(savedUser.getId())
                        .paymentGatewayId(payReq.getPaymentGatewayId())
                        .paymentGatewayName(payReq.getPaymentGatewayName())
                        .status(AuthConstant.ACTIVE)
                        .build();
                bankRepo.save(bankDetails);
            }

            LoginResponse loginResponse = generateTokenWithLoginDetails(savedUser);

            // ---------------- SUCCESS RESPONSE ----------------
            Map<String, Object> success = new HashMap<>();
            success.put("status", "success");
            success.put("message", "User registered successfully with bank details");
            success.put("data", loginResponse);

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
                    AccountStatus.ACTIVE,
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
            // 1. Fetch Active Users
            Page<User> customersPage = userRepository.findByUserTypeAndAccountStatus(
                    UserType.USER,
                    AccountStatus.ACTIVE,
                    pageable
            );

            // 2. Map Users to DTO with REAL DATA
            List<CustomerDetailsBasic> data = customersPage.stream()
                    .map(user -> {

                        // A. Fetch Real Addresses
                        // Note: Ensure findByUserIdAndStatus exists in your AddressRepository
                        List<Address> addressEntities = addressRepository.findByUserIdAndStatus(user.getId(), AuthConstant.ACTIVE);
                        List<AddressResponse> addressList = addressEntities.stream()
                                .map(this::mapToAddressResponse) // Use helper method
                                .collect(Collectors.toList());

                        // B. Fetch Real Subscription Plan
                        SubscriptionResponse subResponse = null;
                        if (user.getSubscriptionPlanId() != null) {
                            subResponse = fetchSubscription(user.getSubscriptionPlanId()); // Use helper method
                        }

                        // C. Build Response
                        return new CustomerDetailsBasic(
                                user.getId(),
                                user.getEmail(),
                                user.getPhoneNumber(),
                                user.getFullName(),
                                user.getProfilePictureUrl(),
                                0, // borrowedCount (placeholder)
                                0, // returnedCount (placeholder)
                                0, // pendingCount (placeholder)
                                subResponse, // ✅ Real Subscription Data
                                addressList  // ✅ Real Address Data
                        );
                    })
                    .collect(Collectors.toList());

            // 3. Prepare response map
            response.put("status", "success");
            response.put("customersData", data);
            response.put("page", customersPage.getNumber());
            response.put("size", customersPage.getSize());
            response.put("totalElements", customersPage.getTotalElements());
            response.put("totalPages", customersPage.getTotalPages());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to fetch active customers");
            response.put("details", e.getMessage());
            e.printStackTrace();
        }
        return response;
    }
    @Override
    public List<RestaurantRegisterResponse> getAllActiveRestaurantsByListOfIds(List<Long> restaurantIds) {
        return userRepository.findRestaurantsByIds(restaurantIds, UserType.RESTAURANT, AccountStatus.ACTIVE);
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
                return response;
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

    @Override
    public ApiResponse<?> uploadImage(MultipartFile profileImage, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", AuthConstant.ERROR));
        try {
            if(user.getProfilePictureUrl()!=null){
                String existingImageName = user.getProfilePictureUrl();
                notificationFeignClientService.deleteContainer("profile",existingImageName);
            }

            String profileImageUrl = null;

            if (profileImage != null && !profileImage.isEmpty()) {

                if (profileImage.getContentType() == null ||
                        !profileImage.getContentType().startsWith("image/")) {
                    throw new IllegalArgumentException("Only image files are allowed");
                }

                    profileImageUrl = notificationFeignClientService.uploadImage("profile",profileImage);
                    System.err.println("profileImageUrl = " + profileImageUrl);
                }


            System.err.println("Uploaded image URL: " + profileImageUrl);
            user.setProfilePictureUrl(profileImageUrl);
            userRepository.save(user);
            return new ApiResponse<>("Profile image updated successfully", AuthConstant.SUCCESS, profileImageUrl);
        } catch (Exception e) {
            return new ApiResponse<>("Error updating profile image", AuthConstant.ERROR, null);
        }

    }


    @Override
    public ApiResponse<List<RestaurantRegisterResponse>> getPendingRestaurants() {

        List<User> users =
                userRepository.findByAccountStatus(AccountStatus.PENDING);

        if (users.isEmpty()) {
            return new ApiResponse<>(
                    "success",
                    "No pending restaurant registrations found",
                    List.of()
            );
        }

        List<RestaurantRegisterResponse> responseList =
                users.stream()
                        .map(this::mapToResponse)
                        .toList();

        return new ApiResponse<>(
                "success",
                "Pending restaurant registrations fetched successfully",
                responseList
        );
    }

    private RestaurantRegisterResponse mapToResponse(User user) {

        List<Address> addresses = addressRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        "active"
                );

        String address = addresses.isEmpty()
                ? null
                : addresses.stream()
                .map(a -> Stream.of(
                                        a.getFlatDoorHouseDetails(),
                                        a.getAreaStreetCityBlockDetails(),
                                        a.getPoBoxOrPostalCode()
                                )
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining(", "))
                )
                .collect(Collectors.joining(" | "));



        return RestaurantRegisterResponse.builder()
                .restaurantId(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfilePictureUrl())
                .address(address)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Void> approveOrRejectUser(AdminUserActionRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID: " + request.getUserId())
                );

        if (user.getAccountStatus() != AccountStatus.PENDING) {
            return ApiResponse.<Void>builder()
                    .status(AuthConstant.ERROR)
                    .message("Only pending users can be approved or rejected")
                    .build();
        }
        try {
            if (request.getApproveStatus().equalsIgnoreCase("APPROVE")) {
                user.setAccountStatus(AccountStatus.ACTIVE);
            } else if (request.getApproveStatus().equalsIgnoreCase("REJECT")) {
                user.setAccountStatus(AccountStatus.REJECTED);
            } else {
                return ApiResponse.<Void>builder()
                        .status(AuthConstant.ERROR)
                        .message("Invalid action. Use APPROVE or REJECT.")
                        .build();
            }

            userRepository.save(user);

            return ApiResponse.<Void>builder()
                    .status(AuthConstant.SUCCESS)
                    .message("User " + request.getApproveStatus().toLowerCase() + "d successfully")
                    .build();

        } catch (Exception e) {
            return ApiResponse.<Void>builder()
                    .status(AuthConstant.ERROR)
                    .message("Error processing user action: " + e.getMessage())
                    .build();
        }
    }

    // Helper to convert Address Entity -> AddressResponse DTO
    private AddressResponse mapToAddressResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getAddressType(),
                address.getFlatDoorHouseDetails(),
                address.getAreaStreetCityBlockDetails(),
                address.getPoBoxOrPostalCode()
        );
    }

    // Helper to fetch and parse Subscription Plan from Inventory Service
    private SubscriptionResponse fetchSubscription(Integer planId) {
        try {
            // Call Inventory Service via Feign Client
            Map<String, Object> planResp = inventoryFeignClient.getSubscriptionPlanById(planId);

            if (planResp != null && "success".equals(planResp.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) planResp.get("data");

                // Parse the Map to DTO safely
                return new SubscriptionResponse(
                        (Integer) data.get("planId"),
                        (String) data.get("planName"),
                        (String) data.get("planType"),
                        (String) data.get("description"),
                        (String) data.get("partnerType"),
                        data.get("feeType") != null ? new BigDecimal(data.get("feeType").toString()) : BigDecimal.ZERO,
                        data.get("depositType") != null ? new BigDecimal(data.get("depositType").toString()) : BigDecimal.ZERO,
                        data.get("commissionPercentage") != null ? new BigDecimal(data.get("commissionPercentage").toString()) : BigDecimal.ZERO,
                        (Integer) data.get("minContainers"),
                        (Integer) data.get("maxContainers"),
                        (Integer) data.get("totalContainers"),
                        (Boolean) data.get("includesDelivery"),
                        (Boolean) data.get("includesMarketing"),
                        (Boolean) data.get("includesAnalytics"),
                        (String) data.get("billingCycle"),
                        (String) data.get("planStatus")
                );
            }
        } catch (Exception e) {
            System.err.println("Error fetching subscription for planId " + planId + ": " + e.getMessage());
        }
        return null; // Return null if not found or error
    }
}
