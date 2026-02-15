package com.auth.service.Impl;

import com.auth.constant.AuthConstant;
import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.Gender;
import com.auth.enumDetails.UserType;
import com.auth.exception.GenericException;
import com.auth.exception.ResourceNotFoundException;
import com.auth.feignClient.InventoryFeignClient;
import com.auth.feignClient.service.NotificationFeignClientService;
import com.auth.model.*;
import com.auth.repository.*;
import com.auth.request.*;
import com.auth.response.*;
import com.auth.service.UserService;
import com.auth.util.AuthUtil;
import com.auth.util.DistanceUtil;
import com.auth.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final ContactAndRegistrationDetailsRepository contactAndRegistrationDetailsRepository;
    private final SocialMediaDetailsRepository socialMediaDetailsRepository;
    private final ReferPartnerRepository referPartnerRepository;
    @Value("${image.storage.root-path}")
    private String userProfilePath;

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
                    .bicNumber(bankDetails.getBicNumber())
                    .iBanNumber(bankDetails.getIBanNumber())
                    .accountHolderName(bankDetails.getCardHolderName())
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

    // ... existing imports ...

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
                    .accountHolderName(bankDetails.getAccountHolderName())
                    .iBanNumber(bankDetails.getIBanNumber())
                    .bicNumber(bankDetails.getBicNumber())
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
    public ApiResponse<CustomerProfileResponse> updateBankDetails(BankCardPaymentGetWayDetailsRequest request) {
        try {

            BankDetails bankDetails = null;

            // ========== 🏦 BANK DETAILS ==========
            if (request.getBankDetailsRequest() != null) {
                var r = request.getBankDetailsRequest();

                BankDetails bankRow = (r.getId() != null)
                        ? bankRepo.findById(r.getId()).orElse(new BankDetails())
                        : new BankDetails();

                Optional.ofNullable(r.getBankName()).ifPresent(bankRow::setBankName);
                Optional.ofNullable(r.getAccountHolderName()).ifPresent(bankRow::setAccountHolderName);
                Optional.ofNullable(r.getIBanNumber()).ifPresent(bankRow::setIBanNumber);
                Optional.ofNullable(r.getBicNumber()).ifPresent(bankRow::setBicNumber);
                bankDetails = bankRepo.save(bankRow);
            }

            // ========== 💳 CARD DETAILS ==========
            if (request.getCardDetailsRequest() != null) {
                var r = request.getCardDetailsRequest();

                BankDetails cardRow = (r.getId() != null)
                        ? bankRepo.findById(r.getId()).orElse(new BankDetails())
                        : new BankDetails();

                Optional.ofNullable(r.getCardHolderName()).ifPresent(cardRow::setCardHolderName);
                Optional.ofNullable(r.getCardNumber()).ifPresent(cardRow::setCardNumber);
                Optional.ofNullable(r.getExpiryDate()).ifPresent(cardRow::setExpiryDate);
                Optional.ofNullable(r.getCvv()).ifPresent(cardRow::setCvv);

                bankDetails = bankRepo.save(cardRow);
            }

            // ========== 🧾 PAYMENT GATEWAY ==========
            if (request.getPaymentGetWayRequest() != null) {
                var r = request.getPaymentGetWayRequest();

                BankDetails payRow = (r.getId() != null)
                        ? bankRepo.findById(r.getId()).orElse(new BankDetails())
                        : new BankDetails();

                Optional.ofNullable(r.getPaymentGatewayId()).ifPresent(payRow::setPaymentGatewayId);
                Optional.ofNullable(r.getPaymentGatewayName()).ifPresent(payRow::setPaymentGatewayName);

                bankDetails = bankRepo.save(payRow);
            }

            CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(bankDetails.getUserId()).getData();


            return new ApiResponse<>(AuthConstant.SUCCESS, "Bank Details updated successfully", customerProfileResponse);

        } catch (Exception e) {
            return new ApiResponse<>(AuthConstant.ERROR, "Error on updating details", null);
        }
    }

    @Override
    public ApiResponse<CustomerProfileResponse> getCustomerProfileDetails(Long userId) {

        try {
            List<Object[]> result =
                    userRepository.getCustomerProfileDetailsByUserId(userId);

            if (result == null || result.isEmpty()) {
                return new ApiResponse<>(AuthConstant.ERROR, "Customer not found", null);
            }

            Object[] row = result.get(0);   // ← THIS LINE FIXES EVERYTHING

            CustomerProfileResponse response = new CustomerProfileResponse();

            // Basic
            response.setId(((Number) row[0]).longValue());
            response.setFullName((String) row[1]);
            response.setMobileNumber((String) row[2]);
            response.setCustomerId((String) row[3]);
            response.setEmailId((String) row[4]);
            response.setProfileImageUrl((String) row[5]);
            response.setSubscriptionPlanId(
                    row[6] != null ? ((Number) row[6]).intValue() : null
            );

            // Bank
            if (row[7] != null) {
                response.setBankDetailsResponse(
                        new BankDetailsResponse(
                                ((Number) row[7]).longValue(),
                                userId,
                                (String) row[8],
                                (String) row[9],
                                (String) row[10],
                                (String) row[11]
                        )
                );
            }

            // Card
            if (row[12] != null) {
                response.setCardDetailsResponse(
                        new CardDetailsResponse(
                                ((Number) row[12]).longValue(),
                                (String) row[13],
                                (String) row[14],
                                (String) row[15]
                        )
                );
            }

            // Payment
            if (row[16] != null) {
                response.setPaymentGetWayResponse(
                        new PaymentGetWayResponse(
                                ((Number) row[16]).longValue(),
                                (String) row[17],
                                (String) row[18]
                        )
                );
            }

            // Secondary & DOB
            response.setSecondaryNumber((String) row[19]);
            response.setDateOfBirth(
                    row[20] != null ? (row[20]).toString() : null
            );

            // Contact
            if (row[21] != null) {
                response.setContactAndRegistrationDetailsResponse(
                        new ContactAndRegistrationDetailsResponse(
                                ((Number) row[21]).longValue(),
                                (String) row[22],
                                (String) row[23],
                                (String) row[24],
                                (String) row[25],
                                (String) row[26],
                                (String) row[27]
                        )
                );
            }

            // Addresses (JSON)
            String addressJson = row[28].toString();
            ObjectMapper mapper = new ObjectMapper();
            List<AddressResponse> addresses =
                    mapper.readValue(addressJson,
                            new TypeReference<List<AddressResponse>>() {});
            response.setAddressResponses(addresses);

            //Social Media Details (JSON)
            String socialMediaJson = row[29].toString();
            List<SocialMediaResponse> socialMediaResponses =
                    mapper.readValue(socialMediaJson,
                            new TypeReference<List<SocialMediaResponse>>() {});
            response.setSocialMediaResponse(socialMediaResponses);

            //Business Details
            if (row[30] != null) {
                response.setBusinessDetailsResponse(
                        new BusinessDetailsResponse(
                                ((Number) row[30]).longValue(),
                                (String) row[31],
                                (String) row[32]
                        )
                );
            }


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
                            (String) data.get("planStatus"),
                            (String) data.get("userType"),
                            LocalDateTime.parse((String) data.get("createdAt")),
                            LocalDateTime.parse((String) data.get("updatedAt"))
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


    @Override
    public ApiResponse<CustomerProfileResponse> updateBusinessInfo(RegistrationRequest request) {
        try {
            if (request.getContactAndRegistrationDetails() != null){
                Optional<ContactRegistrationDetails> contactRegistrationDetailsOpt = contactAndRegistrationDetailsRepository.findById(request.getContactAndRegistrationDetails().getId());
                if (contactRegistrationDetailsOpt.isPresent()){
                    ContactRegistrationDetails contactDetails = contactRegistrationDetailsOpt.get();
                    contactDetails.setContactPersonName(request.getContactAndRegistrationDetails().getContactPersonName());
                    contactDetails.setContactNumber(request.getContactAndRegistrationDetails().getContactNumber());
                    contactDetails.setContactEmail(request.getContactAndRegistrationDetails().getContactEmail());
                    contactDetails.setRegistrationNumber(request.getContactAndRegistrationDetails().getRegistrationNumber());
                    contactDetails.setVatNumber(request.getContactAndRegistrationDetails().getVatNumber());
                    contactDetails.setTreadLicenseNumber(request.getContactAndRegistrationDetails().getTreadLicenseNumber());
                    contactAndRegistrationDetailsRepository.save(contactDetails);
                }
            }

            if (!CollectionUtils.isEmpty(request.getSocialMediaList())){
                for (RegistrationRequest.SocialMediaRequest socialMediaRequest : request.getSocialMediaList()) {
                    Optional<SocialMediaDetails> socialMediaDetailsOpt = socialMediaDetailsRepository.findById(socialMediaRequest.getId());
                    if (socialMediaDetailsOpt.isPresent()){
                        SocialMediaDetails socialMediaDetails = socialMediaDetailsOpt.get();
                        socialMediaDetails.setSocialMediaType(socialMediaRequest.getSocialMediaType());
                        socialMediaDetails.setLink(socialMediaRequest.getLink());
                        socialMediaDetailsRepository.save(socialMediaDetails);
                    }
                }
            }

            if (request.getBasicDetails() != null){
                Optional<BasicRestaurantDetails> basicDetailsOpt = basicRepo.findById(request.getBasicDetails().getId());
                if (basicDetailsOpt.isPresent()){
                    BasicRestaurantDetails basicDetails = basicDetailsOpt.get();
                    basicDetails.setBusinessType(request.getBasicDetails().getBusinessType());
                    basicDetails.setWebsiteDetails(request.getBasicDetails().getWebsiteDetails());
                    basicRepo.save(basicDetails);
                }
            }
            CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(request.getUserId()).getData();

            return new ApiResponse<>(AuthConstant.ERROR, "Business info updated successfully.", customerProfileResponse);

        }catch (Exception e) {
            log.error("Error updating business info: {}", e.getMessage(), e);
            return new ApiResponse<>(AuthConstant.ERROR, "Error on updating business info", null);
        }
    }


    @Override
    public ApiResponse<CustomerProfileResponse> saveNewAddress(AddressRequest request) {
        // Save Address
        Address address = Address.builder()
                .userId(request.getUserId())
                .addressType(request.getAddressType())
                .flatDoorHouseDetails(request.getFlatDoorHouseDetails())
                .areaStreetCityBlockDetails(request.getAreaStreetCityBlockDetails())
                .poBoxOrPostalCode(request.getPoBoxOrPostalCode())
                .status(AuthConstant.ACTIVE)
                .build();

        Address savedAddress = addressRepository.save(address);
        CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(savedAddress.getUserId()).getData();

        return new ApiResponse<>("Address created successfully", AuthConstant.SUCCESS, customerProfileResponse);
    }

    @Override
    public ApiResponse<CustomerProfileResponse> updateAddress(AddressRequest request) {

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found", AuthConstant.ERROR));

        // Update only non-null fields
        Optional.ofNullable(request.getAddressType()).ifPresent(address::setAddressType);
        Optional.ofNullable(request.getFlatDoorHouseDetails()).ifPresent(address::setFlatDoorHouseDetails);
        Optional.ofNullable(request.getAreaStreetCityBlockDetails()).ifPresent(address::setAreaStreetCityBlockDetails);
        Optional.ofNullable(request.getPoBoxOrPostalCode()).ifPresent(address::setPoBoxOrPostalCode);

        Address updatedAddress = addressRepository.save(address);

        CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(updatedAddress.getUserId()).getData();


        return new ApiResponse<>(AuthConstant.SUCCESS, "Address updated successfully", customerProfileResponse);
    }

    @Override
    public ApiResponse<CustomerProfileResponse> deleteAddress(AddressRequest request) {

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found", AuthConstant.ERROR));

        address.setStatus(AuthConstant.IN_ACTIVE);
        Address deletedAddress = addressRepository.save(address);

        CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(deletedAddress.getUserId()).getData();

        return new ApiResponse<>(AuthConstant.SUCCESS, "Address deleted successfully", customerProfileResponse);
    }

    @Override
    public ApiResponse<CustomerProfileResponse> createBankDetails(
            BankCardPaymentGetWayDetailsRequest request) {

        try {
            BankDetails bankDetails = new BankDetails();

            // ====== BANK DETAILS ======
            if (request.getBankDetailsRequest() != null) {
                BankCardPaymentGetWayDetailsRequest.BankDetailsRequest bankReq =
                        request.getBankDetailsRequest();

                bankDetails.setUserId(bankReq.getUserId());
                bankDetails.setBankName(bankReq.getBankName());
                bankDetails.setBicNumber(bankReq.getBicNumber());
                bankDetails.setAccountHolderName(bankReq.getAccountHolderName());
                bankDetails.setIBanNumber(bankReq.getIBanNumber());
            }

            // ====== CARD DETAILS ======
            if (request.getCardDetailsRequest() != null) {
                BankCardPaymentGetWayDetailsRequest.CardDetailsRequest cardReq =
                        request.getCardDetailsRequest();

                bankDetails.setCardHolderName(cardReq.getCardHolderName());
                bankDetails.setUserId(cardReq.getUserId());
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
                bankDetails.setUserId(payReq.getUserId());

            }

            // ====== DEFAULT STATUS ======
            bankDetails.setStatus(AuthConstant.ACTIVE);

            // ====== SAVE ======
            BankDetails saved = bankRepo.save(bankDetails);

            CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(saved.getUserId()).getData();


            return new ApiResponse<>(AuthConstant.SUCCESS, "Bank details created successfully", customerProfileResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(AuthConstant.ERROR, "Error on creating bank details",
                    null);
        }
    }

    @Override
    public ApiResponse<CustomerProfileResponse> deleteBankDetails(Long id) {
        try {
            Optional<BankDetails> bankDetailsOptional = bankRepo.findById(id);
            if (bankDetailsOptional.isPresent()) {
                BankDetails bankDetails = bankDetailsOptional.get();
                bankDetails.setStatus(AuthConstant.IN_ACTIVE);
                BankDetails deleteDetails = bankRepo.save(bankDetails);

                CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(deleteDetails.getUserId()).getData();

                return new ApiResponse<>(AuthConstant.SUCCESS, "Bank details deleted successfully", customerProfileResponse);
            }
            return new ApiResponse<>(AuthConstant.ERROR, "Bank details not found", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(AuthConstant.ERROR, "Error on deleting bank details",
                    null);
        }
    }


    @Override
    public ApiResponse<CustomerProfileResponse> updateUserProfile(String userData, MultipartFile profileImage) {
        try {
            UpdateProfileRequest request = AuthUtil.convertToJson(userData, UpdateProfileRequest.class);
            if (request == null) {
                return new ApiResponse<>(AuthConstant.ERROR, "Please provide valid request", null);
            }

            Optional<User> userOptional = userRepository.findById(request.getUserId());
            if (userOptional.isEmpty()) {
                return new ApiResponse<>(AuthConstant.ERROR, "User not found", null);
            }

            User user = userOptional.get();
            List<String> updatedFields = new ArrayList<>();

            // Full Name
            if (StringUtils.hasText(request.getFullName())
                    && !request.getFullName().equals(user.getFullName())) {
                user.setFullName(request.getFullName());
                updatedFields.add("Full name");
            }

            // Phone Number
            if (request.getPhoneNumber() != null
                    && !request.getPhoneNumber().equals(user.getPhoneNumber())) {

                Optional<User> otherUserOptional =
                        userRepository.findByPhoneNumber(request.getPhoneNumber());

                if (otherUserOptional.isPresent()
                        && !otherUserOptional.get().getId().equals(user.getId())) {

                    return new ApiResponse<>(AuthConstant.ERROR,
                            "Phone number already in use by another user", null);
                }

                user.setPhoneNumber(request.getPhoneNumber());
                updatedFields.add("Phone number");
            }

            // Secondary Number
            if (request.getSecondaryNumber() != null
                    && !request.getSecondaryNumber().equals(user.getSecondaryNumber())) {
                user.setSecondaryNumber(request.getSecondaryNumber());
                updatedFields.add("Secondary number");
            }

            // Date of Birth
            if (StringUtils.hasText(request.getDateOfBirth())) {
                try {
                    LocalDate dob = LocalDate.parse(
                            request.getDateOfBirth(),
                            DateTimeFormatter.ISO_LOCAL_DATE
                    );

                    if (!dob.equals(user.getDateOfBirth())) {
                        user.setDateOfBirth(dob);
                        updatedFields.add("Date of birth");
                    }
                } catch (DateTimeParseException e) {
                    return new ApiResponse<>(AuthConstant.ERROR,
                            "Invalid Date of Birth format. Use YYYY-MM-DD", null);
                }
            }

            // Profile Image
            if (profileImage != null && !profileImage.isEmpty()) {
                String profileImageUrl =
                        notificationFeignClientService.uploadImage("profile", profileImage);
                user.setProfilePictureUrl(profileImageUrl);
                updatedFields.add("Profile image");
            }

            userRepository.save(user);

            ApiResponse<CustomerProfileResponse> customerProfileResponse =
                    getCustomerProfileDetails(user.getId());

            // Dynamic message
            String message;
            if (updatedFields.isEmpty()) {
                message = "No changes detected";
            } else {
                message = String.join(", ", updatedFields) + " updated successfully";
            }

            return new ApiResponse<>(
                    AuthConstant.SUCCESS,
                    message,
                    customerProfileResponse.getData()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(AuthConstant.ERROR,
                    "Error on updating profile details", null);
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
    public ApiResponse<?> registerRestaurant(
            RegistrationRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new GenericException("Email is already registered");
            }

            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new GenericException("Phone number already registered");
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                throw new GenericException("Password must be at least 6 characters");
            }


            User user = User.builder()
                    .userType(UserType.RESTAURANT)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .userName(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .subscriptionPlanId(request.getSubscriptionPlanId())
                    .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                    .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                    .accountStatus(AccountStatus.active)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .build();

            User savedUser = userRepository.save(user);

            if (request.getBasicDetails() != null) {
                BasicRestaurantDetails basic = BasicRestaurantDetails.builder()
                        .restaurantId(savedUser.getId())
                        .businessType(request.getBasicDetails().getBusinessType())
                        .websiteDetails(request.getBasicDetails().getWebsiteDetails())
                        .cuisine(request.getBasicDetails().getCuisine())
                        .build();
                basicRepo.save(basic);
            }


            // ---------------- CREATE ADDRESS DETAILS ----------------
            if (request.getAddress() != null) {
                RegistrationRequest.AddressRequest addressReq = request.getAddress();

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


            // ---------------- BANK DETAILS ----------------
            if (request.getBankDetails() != null) {
                RegistrationRequest.BankDetailsRequest bankReq =
                        request.getBankDetails();

                BankDetails bankDetails = BankDetails.builder()
                        .userId(savedUser.getId())
                        .bankName(bankReq.getBankName())
                        .accountHolderName(bankReq.getAccountHolderName())
                        .iBanNumber(bankReq.getIBanNumber())
                        .bicNumber(bankReq.getBicNumber())
                        .status(AuthConstant.ACTIVE)
                        .build();

                bankRepo.save(bankDetails);
            }

            if (request.getCardDetails() != null) {
                RegistrationRequest.CardDetailsRequest cardReq =
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
                RegistrationRequest.PaymentGetWayRequest payReq =
                        request.getPaymentGetWay();
                BankDetails bankDetails = BankDetails.builder()
                        .userId(savedUser.getId())
                        .paymentGatewayId(payReq.getPaymentGatewayId())
                        .paymentGatewayName(payReq.getPaymentGatewayName())
                        .status(AuthConstant.ACTIVE)
                        .build();
                bankRepo.save(bankDetails);
            }
            if (request.getContactAndRegistrationDetails() != null) {
                RegistrationRequest.ContactAndRegistrationDetailsRequest contactReq =
                        request.getContactAndRegistrationDetails();
                ContactRegistrationDetails contactDetails = ContactRegistrationDetails.builder()
                        .userId(savedUser.getId())
                        .contactPersonName(contactReq.getContactPersonName())
                        .contactNumber(contactReq.getContactNumber())
                        .registrationNumber(contactReq.getRegistrationNumber())
                        .vatNumber(contactReq.getVatNumber())
                        .contactEmail(contactReq.getContactEmail())
                        .treadLicenseNumber(contactReq.getTreadLicenseNumber())
                        .build();
                contactAndRegistrationDetailsRepository.save(contactDetails);
            }

            // ---------------- SOCIAL MEDIA LINKS ----------------
            if (request.getSocialMediaList() != null) {
                for (RegistrationRequest.SocialMediaRequest sm : request.getSocialMediaList()) {
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
            return new ApiResponse<>(AuthConstant.SUCCESS, "Restaurant registered successfully", loginResponse);
        } catch (Exception e) {
            log.error("Error registering restaurant:{} ", e.getMessage());
            throw new GenericException("Something went wrong: " + e.getMessage());
        }
    }


    @Transactional
    @Override
    public ApiResponse<?> registerUserWithBankDetails(
            RegistrationRequest request
    ) {

        try {

            // ---------------- VALIDATIONS ----------------
            if (request == null) {
                throw new GenericException("Request body is missing");
            }


            if (userRepository.existsByEmail(request.getEmail())) {
                throw new GenericException("Email already registered");
            }


            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new GenericException("Phone number already registered");
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                throw new GenericException("Password must be at least 6 characters");
            }


            // ---------------- CREATE USER ----------------

            User.UserBuilder userBuilder = User.builder()
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
                    .accountStatus(AccountStatus.active)
                    .emailVerified(false)
                    .phoneVerified(false);

            // 2. ✅ ADD GENDER LOGIC HERE
            if (request.getGender() != null && !request.getGender().isEmpty()) {
                try {
                    // Convert String (e.g., "Male") to Enum (Gender.MALE)
                    userBuilder.gender(Gender.valueOf(request.getGender().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender provided: {}", request.getGender());
                }
            }

            // 3. Build and Save
            User user = userBuilder.build();
            User savedUser = userRepository.save(user);


            // ---------------- CREATE ADDRESS DETAILS ----------------
            if (request.getAddress() != null) {
                RegistrationRequest.AddressRequest addressReq = request.getAddress();

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
                RegistrationRequest.BankDetailsRequest bankReq =
                        request.getBankDetails();

                BankDetails bankDetails = BankDetails.builder()
                        .userId(savedUser.getId())
                        .bankName(bankReq.getBankName())
                        .bicNumber(bankReq.getBicNumber())
                        .iBanNumber(bankReq.getIBanNumber())
                        .accountHolderName(bankReq.getAccountHolderName())
                        .status(AuthConstant.ACTIVE)
                        .build();

                bankRepo.save(bankDetails);
            }

            if (request.getCardDetails() != null) {
                RegistrationRequest.CardDetailsRequest cardReq =
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
                RegistrationRequest.PaymentGetWayRequest payReq =
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

//            // ---------------- SUCCESS RESPONSE ----------------


            return new ApiResponse<>(
                    AuthConstant.SUCCESS,
                    "User registered successfully with bank details",
                    loginResponse
            );


        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Known validation / business errors
            log.error("Registration error With IllegalStateException: {}", ex.getMessage());
            throw new GenericException(ex.getMessage());

        } catch (Exception ex) {
            // Log full error internally
            log.error("Unexpected error during registration:  {} ", ex.getMessage());
            throw new GenericException(ex.getMessage());
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
            // 1. Fetch Active Users
            Page<User> customersPage = userRepository.findByUserTypeAndAccountStatus(
                    UserType.USER,
                    AccountStatus.active,
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
        return userRepository.findRestaurantsByIds(restaurantIds, UserType.RESTAURANT, AccountStatus.active);
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
    public ApiResponse<CustomerProfileResponse> upgradeUserSubscription(SubscriptionRequest subscriptionRequest) {
        Map<String, Object> response = new HashMap<>();
        try {

            Optional<User> userOpt = userRepository.findById(subscriptionRequest.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setSubscriptionPlanId(subscriptionRequest.getSubscriptionPlanId());
                User saveUser = userRepository.save(user);
                CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(saveUser.getId()).getData();
                System.err.println("customerProfileResponse = " + customerProfileResponse);
                return new ApiResponse<>(AuthConstant.SUCCESS, "User subscription updated successfully", customerProfileResponse);
            }

        } catch (Exception e) {
            return new ApiResponse<>(AuthConstant.ERROR, "Failed to upgrade user subscription", null);
        }
        return new ApiResponse<>(AuthConstant.ERROR, "User not found", null);
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
        String baseId = namePart+ "-" + datePart;

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
            if (user.getProfilePictureUrl() != null) {
                String existingImageName = user.getProfilePictureUrl();
                notificationFeignClientService.deleteContainer("profile", existingImageName);
            }

            String profileImageUrl = null;

            if (profileImage != null && !profileImage.isEmpty()) {

                if (profileImage.getContentType() == null ||
                        !profileImage.getContentType().startsWith("image/")) {
                    throw new IllegalArgumentException("Only image files are allowed");
                }

                profileImageUrl = notificationFeignClientService.uploadImage("profile", profileImage);
                System.err.println("profileImageUrl = " + profileImageUrl);
            }


            System.err.println("Uploaded image URL: " + profileImageUrl);
            user.setProfilePictureUrl(profileImageUrl);
            userRepository.save(user);
            return new ApiResponse<>(AuthConstant.SUCCESS, "Profile image updated successfully", profileImageUrl);
        } catch (Exception e) {
            return new ApiResponse<>(AuthConstant.ERROR, "Error updating profile image", null);
        }

    }

    @Override
    public ApiResponse<List<RestaurantListResponse>> getAllActiveRestaurants() {

        List<Object[]> response = userRepository.findAllActiveRestaurants(UserType.RESTAURANT, AccountStatus.active);
        if (CollectionUtils.isEmpty(response)){
            return new ApiResponse<>(AuthConstant.ERROR, "No active restaurants found", null);
        }
        List<RestaurantListResponse> restaurantListResponses = new ArrayList<>();
        for (Object[] obj : response) {
            RestaurantListResponse restaurant = new RestaurantListResponse();
            restaurant.setId(((Number) obj[0]).longValue());
            restaurant.setName((String) obj[1]);
            restaurant.setProfileImageUrl((String) obj[2]);
            restaurant.setAddress(obj[3]+", "+ obj[4]);
            restaurantListResponses.add(restaurant);
        }
        return new ApiResponse<>(AuthConstant.SUCCESS, "Active restaurants fetched successfully", restaurantListResponses);
    }

    @Override
    public ApiResponse<RestaurantDetailsResponse> getRestaurantDetailsById(Long restaurantId) {

        List<Object[]> result = userRepository.findRestaurantDetailsById(restaurantId);

        if (result.isEmpty()) {
            return new ApiResponse<>(AuthConstant.ERROR, "Restaurant details not found", null);
        }

        Object[] row = result.get(0);

        RestaurantDetailsResponse response = new RestaurantDetailsResponse();

        response.setId((Long) row[0]);
        response.setFullName((String) row[1]);
        response.setMobileNumber((String) row[2]);
        response.setSecondaryNumber((String) row[3]);
        response.setPlanId((Integer) row[4]);
        response.setSubscriptionType((String) row[5]);

        // ---- Basic Restaurant Details ----
        BasicRestaurantDetails brd = new BasicRestaurantDetails();
        brd.setId((Long) row[6]);
        brd.setBusinessType((String) row[7]);
        brd.setWebsiteDetails((String) row[8]);
        brd.setCuisine((String) row[9]);
        response.setBasicRestaurantDetails(brd);

        // ---- Contact & Registration ----
        ContactAndRegistrationDetailsResponse crd = new ContactAndRegistrationDetailsResponse();
        crd.setId((Long) row[10]);
        crd.setContactPersonName((String) row[11]);
        crd.setContactEmail((String) row[12]);
        crd.setTreadLicenseNumber((String) row[13]);
        crd.setVatNumber((String) row[14]);
        crd.setContactNumber((String) row[15]);
        crd.setRegistrationNumber((String) row[16]);
        response.setContactAndRegistrationDetailsResponse(crd);

        // ---- Social Media ----
        List<SocialMediaDetails> socialMedia =
                socialMediaDetailsRepository.findByRestaurantId(restaurantId);
        response.setSocialMediaDetailsList(socialMedia);

        // ---- Subscription from Inventory Service ----
        if (response.getPlanId() != null) {

            Map<String, Object> planResp =
                    inventoryFeignClient.getSubscriptionPlanById(response.getPlanId());

            if ("success".equals(planResp.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) planResp.get("data");
                response.setSubscriptionType((String) data.get("planType"));
            }
        }
        return new ApiResponse<>(AuthConstant.SUCCESS, "Restaurant details fetched successfully", response);
    }

    @Override
    public ApiResponse<?> referPartner(ReferPartnerRequest request) {
        try {
            Optional<ReferPartner> existingReferOpt = referPartnerRepository.findByEmailAndPhone(request.getPartnerEmail(), request.getPartnerPhone());
            if (existingReferOpt.isPresent()) {
                ReferPartner existingRefer = existingReferOpt.get();

                // Referred by different same user
                if (!existingRefer.getReferredByUserId().equals(request.getReferredByUserId())) {
                    return new ApiResponse<>(AuthConstant.SUCCESS, "Partner with the same email and phone number has already been referred by someone", null);
                }

                // Update existing record if same user is referring again
                existingRefer.setContactPersonName(request.getPartnerName());
                existingRefer.setBusinessName(request.getBusinessName());
                referPartnerRepository.save(existingRefer);
                return new ApiResponse<>(AuthConstant.SUCCESS, "Partner referred successfully", null);
            }
            ReferPartner referPartner = ReferPartner.builder()
                    .referredByUserId(request.getReferredByUserId())
                    .contactPersonName(request.getPartnerName())
                    .contactEmail(request.getPartnerEmail())
                    .contactPhone(request.getPartnerPhone())
                    .businessName(request.getBusinessName())
                    .status(AuthConstant.ACTIVE)
                    .build();
            referPartnerRepository.save(referPartner);

            return new ApiResponse<>(AuthConstant.SUCCESS, "Partner referred successfully", null);

        } catch (Exception e) {
            log.error("Error referring partner: {}", e.getMessage());
            return new ApiResponse<>(AuthConstant.ERROR, "Error on referring partner", null);
        }
    }

    @Override
    public ApiResponse<CustomerProfileResponse> addBusinessInfo(RegistrationRequest request) {
        try {
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (request.getContactAndRegistrationDetails() != null){
                    RegistrationRequest.ContactAndRegistrationDetailsRequest contactReq =
                            request.getContactAndRegistrationDetails();
                    ContactRegistrationDetails contactDetails = ContactRegistrationDetails.builder()
                            .userId(user.getId())
                            .contactPersonName(contactReq.getContactPersonName())
                            .contactNumber(contactReq.getContactNumber())
                            .registrationNumber(contactReq.getRegistrationNumber())
                            .vatNumber(contactReq.getVatNumber())
                            .contactEmail(contactReq.getContactEmail())
                            .treadLicenseNumber(contactReq.getTreadLicenseNumber())
                            .build();
                    contactAndRegistrationDetailsRepository.save(contactDetails);
                }

                if (request.getBasicDetails() != null) {
                    BasicRestaurantDetails basic = BasicRestaurantDetails.builder()
                            .restaurantId(user.getId())
                            .businessType(request.getBasicDetails().getBusinessType())
                            .websiteDetails(request.getBasicDetails().getWebsiteDetails())
                            .build();
                    basicRepo.save(basic);
                }

                if (!CollectionUtils.isEmpty(request.getSocialMediaList())) {
                    List<SocialMediaDetails> socialMediaDetailsList = new ArrayList<>();
                    for (RegistrationRequest.SocialMediaRequest sm : request.getSocialMediaList()) {
                        SocialMediaDetails media = SocialMediaDetails.builder()
                                .restaurantId(user.getId())
                                .socialMediaType(sm.getSocialMediaType())
                                .link(sm.getLink())
                                .build();

                        socialMediaDetailsList.add(media);
                    }
                    socialRepo.saveAll(socialMediaDetailsList);
                }

                CustomerProfileResponse customerProfileResponse = getCustomerProfileDetails(user.getId()).getData();
                return new ApiResponse<>(AuthConstant.SUCCESS, "Business info added successfully", customerProfileResponse);
            }

            return new ApiResponse<>(AuthConstant.SUCCESS, "Restaurant details not found", null);
        } catch (Exception e) {
            log.error("Error on adding business info: {}", e.getMessage());
            return new ApiResponse<>(AuthConstant.ERROR, "Error on adding business info", null);
        }
    }

    @Override
    public Long getUserIdByCustomerId(String customerId) {
        Long userId = userRepository.findUserIdByCustomerId(customerId);
        if (userId != null) {
            return userId;
        }
        return 0L;
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
                        (String) data.get("planStatus"),
                        (String) data.get("userType"),
                        LocalDateTime.parse((String) data.get("createdAt")),
                        LocalDateTime.parse((String) data.get("updatedAt"))
                );
            }
        } catch (Exception e) {
            System.err.println("Error fetching subscription for planId " + planId + ": " + e.getMessage());
        }
        return null; // Return null if not found or error
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

}
