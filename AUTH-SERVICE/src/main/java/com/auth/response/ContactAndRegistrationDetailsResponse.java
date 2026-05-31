package com.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContactAndRegistrationDetailsResponse {

    private Long id;
    private String contactPersonName;
    private String contactEmail;
    private String treadLicenseNumber;
    private String vatNumber;
    private String contactNumber;
    private String registrationNumber;

}
