package com.auth.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequest {

    // ========== USER REGISTRATION PART ==========
    private String fullName;
    @NotNull(message = "Email cannot be null")
    private String email;
    @NotNull(message = "Phone number cannot be null")
    private String phoneNumber;
    private String password;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private AddressRequest address;
    private Double latitude;
    private Double longitude;
    private Integer subscriptionPlanId;

    // ========== BASIC RESTAURANT DETAILS ==========
    private BasicDetails basicDetails;

    // ========== BANK DETAILS ==========
    private BankDetailsRequest bankDetails;
    private CardDetailsRequest cardDetails;
    private PaymentGetWayRequest paymentGetWay;
    private ContactAndRegistrationDetailsRequest contactAndRegistrationDetails;
    // ========== SOCIAL MEDIA DETAILS ==========
    private List<SocialMediaRequest> socialMediaList;



    // --------- INNER DTO CLASSES ---------

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BasicDetails {
        private String businessType;
        private String websiteDetails;
        private String cuisine;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BankDetailsRequest {
        private String bankName;
        private String bicNumber;
        private String accountHolderName;
        public String iBanNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardDetailsRequest {
        private String cardHolderName;
        private String cardNumber;
        private String expiryDate;
        private String cvv;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentGetWayRequest {
        private String paymentGatewayId;
        private String paymentGatewayName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SocialMediaRequest {
        private String socialMediaType;
        private String link;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ContactAndRegistrationDetailsRequest {
        private String contactPersonName;
        private String contactEmail;
        private String treadLicenseNumber;
        private String vatNumber;
        private String contactNumber;
        private String registrationNumber;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressRequest {
        private String addressType;
        private String flatDoorHouseDetails;
        private String areaStreetCityBlockDetails;
        private String poBoxOrPostalCode;
//        private String status;
    }
}

