package com.auth.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String secondaryNumber;
    private String dateOfBirth;
    private String customerId;
    private String emailId;
    private String profileImageUrl;
    private Integer subscriptionPlanId;
    private BankDetailsResponse bankDetailsResponse;
    private CardDetailsResponse cardDetailsResponse;
    private PaymentGetWayResponse paymentGetWayResponse;
    private List<AddressResponse> addressResponses;
    private SubscriptionResponse subscriptionResponse;
    private ContactAndRegistrationDetailsResponse contactAndRegistrationDetailsResponse;
    private List<SocialMediaResponse> socialMediaResponse;
    private BusinessDetailsResponse businessDetailsResponse;

}
