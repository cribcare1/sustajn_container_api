package com.sustajn.oderservice.dto;

import com.inventory.response.RestaurantOrderedResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderHistoryResponse {

    private List<LeasedResponse> leasedResponses;

    private List<ReceivedResponse> receivedResponses;

    private List<RestaurantOrderedResponse> orderedResponses;
}
