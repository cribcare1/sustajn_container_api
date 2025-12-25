package com.sustajn.oderservice.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnItemRequest {
    private Long productId;
    private int quantity;
}

