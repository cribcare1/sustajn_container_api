package com.sustajn.oderservice.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReduceInventoryRequest {

    private Long restaurantId;
    private Map<Integer, Integer> containerQtyMap;
}
