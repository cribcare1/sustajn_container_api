package com.auth.request;

import com.auth.validation.CreateGroup;
import com.auth.validation.UpdateGroup;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
public class BankCardPaymentGetWayDetailsRequest {

    private BankDetailsRequest bankDetailsRequest;
    private CardDetailsRequest cardDetailsRequest;
    private PaymentGetWayRequest paymentGetWayRequest;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BankDetailsRequest {
        @NotNull(message = "Please provide id", groups = UpdateGroup.class)
        private Long id;
        @NotNull(message = "Please provide user id", groups = CreateGroup.class)
        private Long userId;
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
        @NotNull(message = "Please provide id", groups = UpdateGroup.class)
        private Long id;
        @NotNull(message = "Please provide user id", groups = CreateGroup.class)
        private Long userId;
        private String cardHolderName;
        private String cardNumber;
        private String expiryDate;
        private String cvv;
        private String paymentGatewayId;
        private String paymentGatewayName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentGetWayRequest {
        @NotNull(message = "Please provide id", groups = UpdateGroup.class)
        private Long id;
        @NotNull(message = "Please provide user id", groups = CreateGroup.class)
        private Long userId;
        private String paymentGatewayId;
        private String paymentGatewayName;
    }
}