package com.auth.response;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDetailsResponse {

    private Long id;
    private Long userId;
    private String bankName;
    private String accountHolderName;
    private String iBanNumber;
    private String bicNumber;
}
