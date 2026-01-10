package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedResponse {

    private String productsName; //if multiple product have one order id then | separated names
    private Long orderId;
    private String transactionId;
    private String returnDateTime; //yyyy-MM-dd|HH:mm am/pm
    private Integer returnedQuantity;
}
