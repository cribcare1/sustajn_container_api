package com.auth.request;

import com.auth.validation.CreateGroup;
import com.auth.validation.UpdateGroup;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankDetailsRequest {

    @NotNull(message = "Please provide id", groups = UpdateGroup.class)
    private Long id;
    @NotNull(message = "Please provide user id", groups = CreateGroup.class)
    private Long userId;
    private String bankName;
    private String accountNumber; // Acts as "Account Holder Name" or "Number" based on your entity
    private String iBanNumber;
    private String taxNumber;
    private String cardHolderName;
    private String cardNumber;
    private String expiryDate;
    private String cvv;
    private String paymentGatewayId;
    private String paymentGatewayName;
}