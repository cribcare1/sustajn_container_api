package com.auth.response;
import com.auth.model.BankDetails;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String address;
    private String phoneNumber;
    private String profilePictureUrl;
    private BankDetailsResponse bankDetails;
    private BusinessInfoResponse businessInfo;


    @Builder
    public static class BusinessInfoResponse {
        private String businessType;
        private String website;
    }
}