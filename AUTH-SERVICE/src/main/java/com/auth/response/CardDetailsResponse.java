package com.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CardDetailsResponse {
    private Long id;
    private String cardHolderName;
    private String cardNumber;
    private String expiryDate;
}
