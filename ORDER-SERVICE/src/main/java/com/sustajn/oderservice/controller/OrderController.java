package com.sustajn.oderservice.controller;

import com.sustajn.oderservice.dto.*;
import com.sustajn.oderservice.repository.SoldOrderRepository;
import com.sustajn.oderservice.request.BorrowRequest;
import com.sustajn.oderservice.request.LeasedReturnedGraphInput;
import com.sustajn.oderservice.request.ReturnRequest;
import com.sustajn.oderservice.request.SoldRequest;
import com.sustajn.oderservice.service.OrderService;
import com.sustajn.oderservice.service.SseService;
import com.sustajn.oderservice.service.impl.OrderNotificationService;
import com.sustajn.oderservice.repository.BorrowOrderRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final BorrowOrderRepository borrowOrderRepository;
    private final SoldOrderRepository soldOrderRepository;
    private final OrderNotificationService borrowOrderService;
    private final SseService sseService;

    /**
     * Borrow containers
     */
        @PostMapping("/borrowContainers")
    public ResponseEntity<Map<String, Object>> borrowContainers(
            @RequestBody BorrowRequest request
    ) {
        Map<String, Object> response = orderService.borrowContainers(request);
        try { sseService.notifyAllClients(); } catch (Exception ignored) {}
        return ResponseEntity.ok(response);
    }

    /**
     * Return containers
     */
    @PostMapping("/returnContainers")
    public ResponseEntity<Map<String, Object>> returnContainers(
            @RequestBody ReturnRequest request
    ) {
        Map<String, Object> response = orderService.returnContainers(request);
        try { sseService.notifyAllClients(); } catch (Exception ignored) {}
        return ResponseEntity.ok(response);
    }

    @GetMapping("/borrowed/{userId}")
    public ResponseEntity<Map<String, Object>> getBorrowedContainersForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "APPROVED") String status) {

        Map<String, Object> response = orderService
                .getOrderDetailsListByStatusForUser(userId, status);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthWiseBorrowedDetails")
    public ResponseEntity<Map<String, Object>> getMonthWiseOrders(
            @RequestParam Long userId) {
        Map<String, Object> response = orderService.getMonthWiseOrders(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/details/{orderId}")
    public ResponseEntity<?> getOrderDetailsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetailsByOrderId(orderId));
    }


    @PostMapping("/approve/{orderId}")
    public ResponseEntity<Map<String,Object>> approveOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.approveOrder(orderId));
    }


    @GetMapping("/monthWiseReturnedDetails")
    public ResponseEntity<Map<String, Object>> getMonthWiseReturnOrders(
            @RequestParam Long userId) {
        Map<String, Object> response = orderService.getMonthWiseReturnOrders(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getBorrowedProduct")
    public ResponseEntity<?> getBorrowSummary(@RequestParam(required = false) Long userId, @RequestParam(required = false) String customerId) {
        return ResponseEntity.ok(orderService.getBorrowedProductSummary(userId, customerId));
    }

    @GetMapping("/orderHistory/{restaurantId}")
    public ResponseEntity<?> getOrderHistory(@PathVariable @NotNull(message = "please provide restaurant id") Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrderHistory(restaurantId));
    }

    @GetMapping("/getLeasedReturnedCount")
    public ResponseEntity<?> getLeasedAndReturnedContainersCount(
            @RequestParam @NotNull(message = "please provide restaurant id") Long restaurantId,
            @RequestParam @NotNull(message = "please provide product id") Integer productId) {
        return ResponseEntity.ok(orderService.getLeasedAndReturnedContainersCount(restaurantId, productId));
    }

    @GetMapping("/getLeasedReturnedMonthYearDetails")
    public ResponseEntity<?> getLeasedReturnedMonthYearDetails(@RequestBody @Validated LeasedReturnedGraphInput leasedReturnedGraphInput
    ) {
        return ResponseEntity.ok(orderService.getLeasedReturnedMonthYearDetails(leasedReturnedGraphInput));
    }

    @GetMapping("/getLeasedReturnedCountWithTimeGraph")
    public ResponseEntity<?> getLeasedReturnedCountWithTimeGraph(@RequestBody @Validated LeasedReturnedGraphInput leasedReturnedGraphInput
    ) {
        return ResponseEntity.ok(orderService.getLeasedReturnedCountWithTimeGraph(leasedReturnedGraphInput));
    }

    /**
     * Extend all borrow orders of a given order by 5 days.
     */
    @PostMapping("/{orderId}/extendOrder")
    public ResponseEntity<ApiResponse<Integer>> extendOrder(@PathVariable Long orderId) {

        // Returns number of items extended
         borrowOrderService.extendBorrowOrder(orderId);
        return ResponseEntity.ok(
                new ApiResponse<>("success",
                        "Order extended by 5 days successfully",
                        null)
        );
    }


    // Get most and least used container for a restaurant
    @GetMapping("/mostLeastUsedContainer/{restaurantId}")
    public ResponseEntity<?> getMostAndLeastUsedContainer(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getMostAndLeastUsedContainer(restaurantId));
    }

    // Get Borrowed details by order id
    @PostMapping("/getBorrowedDetailsByOrderId/{orderId}")
    public ResponseEntity<?> getBorrowedDetailsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getBorrowedOrderByOrderId(orderId));
    }
  
    @GetMapping("/restaurantOrders/chart-stats")
    public ResponseEntity<Map<String, Object>> getContainerChartStats(
            @RequestParam Long restaurantId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long productId ,// Acts as container ID
            @RequestParam(required = false) Integer planId
    ) {
        ContainerChartResponse stats = orderService.getChartStatistics(restaurantId, month, year, productId,planId);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Chart data fetched successfully",
                "data", stats
        ));

    }

    @GetMapping(value = "/restaurantOrders/chart-stats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamContainerChartStats(
            @RequestParam Long restaurantId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long productId ,// Acts as container ID
            @RequestParam(required = false) Integer planId
    ) {
        SseEmitter emitter = sseService.subscribe(restaurantId);

        // send initial snapshot
        try {
            ContainerChartResponse stats = orderService.getChartStatistics(restaurantId, month, year, productId, planId);
            sseService.sendUpdate(restaurantId, stats);
        } catch (Exception ignored) {}

        return emitter;
    }


    @PostMapping("/markSold")
    public ResponseEntity<Void> markSold(@RequestBody SoldRequest request) {
        orderService.markAsSold(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/internal/bulk-circulation-counts")
    public Map<Long, Integer> getBulkCirculationCounts() {
        List<Object[]> results = borrowOrderRepository.getCirculationCountsForAllProducts();
        Map<Long, Integer> countsMap = new HashMap<>();

        for (Object[] row : results) {
            Long productId = (Long) row[0];
            Integer count = ((Number) row[1]).intValue();
            countsMap.put(productId, count);
        }
        return countsMap;
    }

    // Internal API to get circulation grouped by users
    @GetMapping("/internal/circulation-by-user/{productId}")
    public Map<Long, Integer> getCirculationByUser(@PathVariable Long productId) {
        List<Object[]> results = borrowOrderRepository.getCirculationPerUserForProduct(productId);
        Map<Long, Integer> userCounts = new HashMap<>();

        for (Object[] row : results) {
            Long userId = (Long) row[0];
            Integer count = ((Number) row[1]).intValue();
            userCounts.put(userId, count);
        }
        return userCounts;
    }

    @GetMapping("/internal/sold-history-dates/{restaurantId}")
    public List<com.sustajn.oderservice.dto.SoldHistoryRawData> getRealSoldHistoryDates(@PathVariable Long restaurantId) {
        return soldOrderRepository.getRealSoldHistory(restaurantId);
    }

    @GetMapping("/partner/{restaurantId}/daily-graph")
    public ResponseEntity<ApiResponse<PartnerGraphBridgeResponse>> getPartnerDailyGraph(
            @PathVariable Long restaurantId,
            @RequestParam String monthName,
            @RequestParam int year,
            @RequestParam(required = false) Long productId) {

        ApiResponse<PartnerGraphBridgeResponse> response =
                orderService.getPartnerDailyGraph(restaurantId, monthName, year, productId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/dashboard-insights")
    public ResponseEntity<ApiResponse<UserDetailsInsightsResponse>> getUserDashboardInsights(
            @PathVariable Long userId) {

        ApiResponse<UserDetailsInsightsResponse> response =
                orderService.getUserDetailsDashboardInsights(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/partner/{restaurantId}/leasing-insights")
    public ResponseEntity<ApiResponse<MostAndLeastLeasedResponse>> getLeasingInsights(
            @PathVariable Long restaurantId) {

        ApiResponse<MostAndLeastLeasedResponse> response =
                orderService.getMostAndLeastLeasedContainer(restaurantId);

        return ResponseEntity.ok(response);
    }

    /**
     * Fetch checkout layout configurations for the "Extend Lease Period" mobile application screen
     */
    @GetMapping("/{orderId}/extension-preview")
    public ResponseEntity<ApiResponse<ExtensionPreviewResponse>> getExtensionPreview(
            @PathVariable Long orderId) {

        ExtensionPreviewResponse data = orderService.getExtensionPreview(orderId);

        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Extension screen data layout provided successfully",
                data
        ));
    }

    @GetMapping("/admin/transactions/extended-fees-dashboard")
    public ResponseEntity<ApiResponse<List<ExtendedFeeMonthWiseResponse>>> getGlobalExtendedFeeDashboard() {

        List<ExtendedFeeMonthWiseResponse> dataset = orderService.getGlobalExtendedFeeDashboard();
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "  lease extension logs provided successfully",
                dataset
        ));
    }

    @GetMapping("/user/{userId}/extended-fees-timeline")
    public ResponseEntity<ApiResponse<List<UserExtendedFeeMonthWiseResponse>>> getUserExtendedFeeHistory(
            @PathVariable Long userId) {

        List<UserExtendedFeeMonthWiseResponse> timeline = orderService.getUserExtendedFeeHistory(userId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "User profile  extension timelines loaded successfully",
                timeline
        ));
    }

    @GetMapping("/internal/restaurant/user-balances/{restaurantId}")
    public ResponseEntity<Map<Long, Map<String, Integer>>> getRestaurantUserBalances(@PathVariable Long restaurantId) {
        List<Object[]> results = borrowOrderRepository.getRestaurantUserBalances(restaurantId);
        Map<Long, Map<String, Integer>> balanceMap = new HashMap<>();

        for (Object[] row : results) {
            if (row[0] != null) {
                Map<String, Integer> metrics = new HashMap<>();
                metrics.put("borrowedByUser", row[1] != null ? ((Long) row[1]).intValue() : 0);
                metrics.put("returnedByUser", row[2] != null ? ((Long) row[2]).intValue() : 0);
                balanceMap.put((Long) row[0], metrics);
            }
        }
        return ResponseEntity.ok(balanceMap);
    }

    @GetMapping(value = "/admin/dashboard/metrics/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAdminDashboardMetrics() {
        return orderService.subscribeAdminDashboard();
    }

}
