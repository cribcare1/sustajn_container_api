package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeasedResponse {

    private String productsName; //if multiple product have one order id then | separated names
    private Long orderId;
    private String transactionId;
    private String leasedStartDateTime; //yyyy-MM-dd|HH:mm am/pm
    private Integer leasedQuantity;
    private List<ProductOrderListResponse> productOrderListResponses;
}
