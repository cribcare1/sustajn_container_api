package com.inventory.service;

import com.inventory.request.AdminOrderApproveRequest;
import com.inventory.request.AdminOrderCreateRequest;
import com.inventory.response.ApiResponse;
import com.inventory.response.RestaurantOrderedResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface AdminRestaurantOrderService {
    public Map<String, Object> raiseOrderRequest(AdminOrderCreateRequest request);
    public Map<String, Object> markOrderAsDelivered(Long orderId);
    public Map<String, Object> approveOrder(AdminOrderApproveRequest request);
    public Map<String, Object> getAvailableContainers(Long restaurantId);

    ApiResponse<List<RestaurantOrderedResponse>> getRestaurantOrderDetails(Long restaurantId);
}
