package com.sustajn.oderservice.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BorrowItemRequest {
    private Long productId;
    private int quantity;
}

