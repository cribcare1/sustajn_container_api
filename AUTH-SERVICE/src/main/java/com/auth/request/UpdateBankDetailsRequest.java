package com.auth.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBankDetailsRequest {
    private String bankName;
    private String accountNumber; // Acts as "Account Holder Name" or "Number" based on your entity
    private String iBanNumber;
    private String taxNumber;
}