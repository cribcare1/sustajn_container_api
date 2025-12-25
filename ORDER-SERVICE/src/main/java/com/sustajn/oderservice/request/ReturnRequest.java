package com.sustajn.oderservice.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReturnRequest {
    private Long userId;
    private Long restaurantId;
    private List<ReturnItemRequest> items;
}

