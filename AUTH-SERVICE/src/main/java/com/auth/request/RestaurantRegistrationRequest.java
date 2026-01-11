package com.auth.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantRegistrationRequest {

    // ========== USER REGISTRATION PART ==========
    private String fullName;
    private String email;
    private String phoneNumber;
    private String password;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String dateOfBirth;
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

    // ========== SOCIAL MEDIA DETAILS ==========
    private List<SocialMediaRequest> socialMediaList;



    // --------- INNER DTO CLASSES ---------

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BasicDetails {
        private String speciality;
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
        private String taxNumber;
        private String accountNumber;
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
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressRequest {
        private String addressType;
        private String flatDoorHouseDetails;
        private String areaStreetCityBlockDetails;
        private String poBoxOrPostalCode;
        private String status;
    }
}

