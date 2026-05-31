package com.auth.request;

import com.auth.validation.CreateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReferPartnerRequest {

    @NotBlank(message = "Business name cannot be null", groups = CreateGroup.class)
    private String businessName;
    @NotBlank(message = "Partner name cannot be null", groups = CreateGroup.class)
    private String partnerName;
    @NotBlank(message = "Partner email cannot be null", groups = CreateGroup.class)
    private String partnerEmail;
    @NotBlank(message = "Partner phone cannot be null", groups = CreateGroup.class)
    private String partnerPhone;
    @NotNull(message = "Referred by user id cannot be null", groups = CreateGroup.class)
    private Long referredByUserId;

}
