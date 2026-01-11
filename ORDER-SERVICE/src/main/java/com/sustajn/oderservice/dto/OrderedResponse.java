package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderedResponse {

    private Integer orderId;
    private String productsName;
    private String orderOn; //yyyy-MM-dd|HH:mm am/pm  (For only Pending status)
    private String updatedDateTime; //yyyy-MM-dd|HH:mm am/pm (For Confirmed/Delivered/Rejected status)
    private String status; //Pending, Confirmed, Delivered, Rejected
}
