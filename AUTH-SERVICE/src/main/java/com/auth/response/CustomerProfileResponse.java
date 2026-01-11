package com.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerProfileResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String customerId;
    private String emailId;
    private String profileImageUrl;
    private BankDetailsResponse bankDetailsResponse;
    private CardDetailsResponse cardDetailsResponse;
    private PaymentGetWayResponse paymentGetWayResponse;
    private List<AddressResponse> addressResponses;

}
