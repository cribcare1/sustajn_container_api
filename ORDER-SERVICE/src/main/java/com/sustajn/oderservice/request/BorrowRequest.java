package com.sustajn.oderservice.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BorrowRequest {
    private Long userId;
    private Long restaurantId;
    private List<BorrowItemRequest> items;
}
