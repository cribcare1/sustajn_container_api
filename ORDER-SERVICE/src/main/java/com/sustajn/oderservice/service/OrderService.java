package com.sustajn.oderservice.service;

import com.sustajn.oderservice.dto.ApiResponse;
import com.sustajn.oderservice.dto.LeasedResponse;
import com.sustajn.oderservice.dto.LeasedReturnedContainerCountResponse;
import com.sustajn.oderservice.dto.OrderHistoryResponse;
import com.sustajn.oderservice.dto.*;
import com.sustajn.oderservice.request.BorrowRequest;
import com.sustajn.oderservice.request.LeasedReturnedGraphInput;
import com.sustajn.oderservice.request.ReturnRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface OrderService {
    public Map<String, Object> borrowContainers(BorrowRequest request);
    public Map<String, Object> returnContainers(ReturnRequest request);


    public ApiResponse<List<ProductDetailsResponse>> getBorrowedProductSummary(Long userId);
    public Map<String, Object> getMonthWiseReturnOrders(Long userId, int year);
    public Map<String,Object> approveOrder(Long orderId);
    public Map<String, Object> getOrderDetailsByOrderId(Long orderId);
    public Map<String, Object> getMonthWiseOrders(Long userId, int year);
    public Map<String, Object> getOrderDetailsListByStatusForUser(Long userId, String status);

    ApiResponse<OrderHistoryResponse> getOrderHistory(Long restaurantId);
    ApiResponse<LeasedReturnedContainerCountResponse> getLeasedAndReturnedContainersCount(Long restaurantId, Integer productId);

    ApiResponse<List<LeasedReturnedMonthYearResponse>> getLeasedReturnedMonthYearDetails(LeasedReturnedGraphInput leasedReturnedGraphInput);

    ApiResponse<List<LeasedReturnedCountWithTimeGraphResponse>> getLeasedReturnedCountWithTimeGraph(LeasedReturnedGraphInput leasedReturnedGraphInput);
}
