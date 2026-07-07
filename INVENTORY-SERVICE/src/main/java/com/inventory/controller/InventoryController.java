package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.Constant.InventoryConstant;
import com.inventory.dto.*;
import com.inventory.entity.ContainerType;
import com.inventory.entity.DamagedContainer;
import com.inventory.exception.InventoryException;
import com.inventory.repository.ContainerTypeRepository;
import com.inventory.request.*;
import com.inventory.response.ApiResponse;
import com.inventory.service.AdminRestaurantOrderService;
import com.inventory.service.InventoryService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final AdminRestaurantOrderService adminOrderService;
    private final ContainerTypeRepository containerTypeRepository;

    @PostMapping("/saveOrUpdateContainerType")
    public ResponseEntity<?> saveOrUpdateContainerType(
            @RequestPart("request") ContainerTypeRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
//            // Convert JSON string to ContainerTypeRequest
//            ContainerTypeRequest request = objectMapper.readValue(requestString, ContainerTypeRequest.class);

            Map<String, Object> response = inventoryService.saveOrUpdate(request, file);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponses(e.getMessage(), InventoryConstant.ERROR, null));
        }
    }


//    // -----------------------------------------
//    //  ADD MULTIPLE INVENTORY ITEMS
//    // -----------------------------------------
//    @PostMapping("/addMultipleInventories")
//    public ResponseEntity<?> addMultipleInventories(@RequestBody InventoryBulkAddRequest request) {
//        return ResponseEntity.ok(service.addMultipleInventories(request));
//    }
    /**
     * Get all active Container Types
     * @return Map with success status, message, and list of active container types
     */
    @GetMapping("/getContainerTypes")
    public ResponseEntity<?> getActiveContainerTypes() {
        Map<String, Object> response = inventoryService.getActiveContainerTypes();
        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete (mark inactive) a Container Type by ID
     * @param id Container Type ID
     * @return Map with success status, message, and deleted data
     */
    @PostMapping("/delete-container-type/{id}")
    public ResponseEntity<?> deleteContainerType(@PathVariable Integer id) {
        Map<String, Object> response = inventoryService.deleteContainerType(id);
        return ResponseEntity.ok(response);
    }




    // -----------------------------------------
    //  UPDATE INVENTORY
    // -----------------------------------------
    @PostMapping("/updateInventory")
    public ResponseEntity<?> updateInventory(@RequestBody InventoryUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventory(request));
    }

    // -----------------------------------------
    //  GET ALL ACTIVE INVENTORY WITH CONTAINER DETAILS
    // -----------------------------------------
    @GetMapping("/getAllActiveInventory")
    public ResponseEntity<?> getAllActiveInventory() {
        return ResponseEntity.ok(inventoryService.getAllActiveInventory());
    }

    @PostMapping("/restaurant/addRestaurantInventory")
    public ResponseEntity<Map<String, Object>> addRestaurantInventoryBulk(
            @RequestBody AdminRestaurantInventoryBulkRequest request) {

        try {
            Map<String, Object> response = inventoryService.addRestaurantInventoryBulk(request);
            return ResponseEntity.ok(response);

        } catch (InventoryException ex) {
            // Custom business exception
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(err);

        } catch (Exception ex) {
            // Unexpected error
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", "Failed to update restaurant inventory");
            err.put("details", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    /**
     * FETCH ALL INVENTORY TRANSACTIONS FOR A RESTAURANT
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<Map<String, Object>> getRestaurantInventory(
            @PathVariable Long restaurantId) {

        try {
            Map<String, Object> response = inventoryService.getRestaurantInventory(restaurantId);
            return ResponseEntity.ok(response);

        } catch (InventoryException ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(err);

        } catch (Exception ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", "Failed to fetch restaurant inventory");
            err.put("details", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping(
            value = "/addContainerByAdmin"
    )
    public ResponseEntity<?> addContainer(
            @RequestParam("data") String data,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {

        Map<String, Object> response;

        try {
            // Convert String JSON → DTO
            AddContainerRequest request =
                    objectMapper.readValue(data, AddContainerRequest.class);

            response = inventoryService.addContainer(request, image);

        } catch (Exception e) {
            response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Invalid request data");
            response.put("details", e.getMessage());
        }

        // ALWAYS return 200
        return ResponseEntity.ok(response);
    }

    @PostMapping("/getProductsByIds")
    public ApiResponse<List<ProductResponse>> getProductsByIds(@RequestBody List<Integer> ids) {
        return inventoryService.getProductsByIds(ids);
    }


    // 1️⃣ Raise a new order request
    @PostMapping("/raiseOrderRequest")
    public ResponseEntity<?> raiseOrderRequest(
            @RequestBody AdminOrderCreateRequest request
    ) {
        Map<String, Object> response = adminOrderService.raiseOrderRequest(request);
        return ResponseEntity.ok(response);
    }

    // 2️⃣ Approve an order by Admin
    @PostMapping("/approveOrder")
    public ResponseEntity<?> approveOrder(
            @RequestBody AdminOrderApproveRequest request
    ) {
        Map<String, Object> response = adminOrderService.approveOrder(request);
        return ResponseEntity.ok(response);
    }

    // 3️⃣ Mark order as delivered (restaurant received the containers)
    @PostMapping("/markOrderAsDelivered/{orderId}")
    public ResponseEntity<?> markOrderAsDelivered(
            @PathVariable("orderId") Long orderId
    ) {
        Map<String, Object> response = adminOrderService.markOrderAsDelivered(orderId);
        return ResponseEntity.ok(response);
    }

    // 4️⃣ Get available containers for a restaurant
//    @GetMapping("/restaurant/getAvailableContainers/{restaurantId}")
    @GetMapping({
            "/restaurant/getAvailableContainers/{restaurantId}"
    })
    public ResponseEntity<?> getAvailableContainers(
            @PathVariable(required = false)
          @NotNull(message = "restaurantId must not be null")Long restaurantId
    ) {
        Map<String, Object> response = adminOrderService.getAvailableContainers(restaurantId);
        return ResponseEntity.ok(response);
    }

    // get Inventory details
    @GetMapping("/getAllResturantInventory/{restaurantId}")
    public ResponseEntity<?> getAllResturantInventory(
            @PathVariable Long restaurantId
    ) {
        return ResponseEntity.ok(inventoryService.getRestaurantContainerInventoryByRestaurantId(restaurantId));
    }


    @PostMapping("/reduceAvailableContainers")
    public ResponseEntity<?> reduceAvailableContainers(@RequestBody ReduceInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.reduceAvailableContainers(request));
    }

    @PostMapping("/increaseAvailableContainers")
    public ResponseEntity<?> increaseAvailableContainers(@RequestBody ReduceInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.increaseContainers(request));
    }

    @PostMapping("/checkAvailabilityOfContainers")
    public ResponseEntity<?> checkAvailability(@RequestBody ReduceInventoryRequest request) {
        Map<String, Object> result = inventoryService.checkAvailability(request);

//        if (InventoryConstant.ERROR.equals(result.get(InventoryConstant.STATUS))) {
//            return ResponseEntity.ok(result);  // business error, not 500
//        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/increaseAvailableContainersOld")
    public Map<String, Object> increaseContainers(@RequestBody ReduceInventoryRequest request) {
        return inventoryService.increaseAvailableContainers(request);
    }

    @PostMapping(
            value = "/reportDamagedContainer",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<DamagedContainer>> reportDamagedContainer(@RequestPart("request") String request, @RequestPart("images") List<MultipartFile> images) {
        return ResponseEntity.ok(inventoryService.reportDamagedContainer(request, images));
    }


    @GetMapping("/getDamagedContainersByRestaurant")
    public ResponseEntity<ApiResponse<List<DamageContainerMonthWiseResponse>>> getDamageContainerMonthWiseDetails(@RequestParam Long restaurantId){
        return ResponseEntity.ok(inventoryService.getDamageContainerMonthWiseDetails(restaurantId));
    }

    @GetMapping("/getSoldContainersByRestaurant")
    public ResponseEntity<ApiResponse<List<SoldContainerMonthWiseResponse>>> getSoldContainerMonthWiseDetails(@RequestParam Long restaurantId){
        return ResponseEntity.ok(inventoryService.getSoldContainerMonthWiseDetails(restaurantId));
    }

    @GetMapping("/getDamageContainerByUserType")
    public ResponseEntity<ApiResponse<List<DamageContainerMonthWiseResponse>>> getDamageContainerDamagedByCustomerOrPartner(@RequestParam String damageBy){
        return ResponseEntity.ok(inventoryService.getDamageContainerMonthWiseDetailsByAllCustomerOrPartner(damageBy));
    }

    @GetMapping("/getDetailedSoldHistoryByRestaurant")
    public ResponseEntity<ApiResponse<List<DetailedSoldMonthResponse>>> getDetailedSoldHistoryByRestaurant(@RequestParam Long restaurantId) {

        List<DetailedSoldMonthResponse> data = inventoryService.getDetailedSoldHistoryByRestaurant(restaurantId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Detailed sold history fetched successfully", data));
    }

    @GetMapping("/getAllContainerTypes")
    public ResponseEntity<Map<String, Object>> getAllContainerTypes() {
        Map<String, Object> response = inventoryService.getAllContainerTypes();

        if ("ERROR".equals(response.get("status"))) {
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/container-stats")
    public ResponseEntity<TrueInventoryStatsDto> getContainerStats(
            @RequestParam Long restaurantId,
            @RequestParam(required = false) Integer containerTypeId,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) Integer year) {

        return ResponseEntity.ok(inventoryService.getContainerStats(restaurantId, containerTypeId, month, year));
    }

    @GetMapping("/container-types/{id}")
    public ResponseEntity<Map<String, Object>> getContainerTypeById(@PathVariable Integer id) {

        // 1. Single execution of the service layer lookup
        Map<String, Object> response = inventoryService.getContainerTypeById(id);

        // 2. Check if the service returned a business error status
        if ("error".equals(response.get("status"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // 3. Return the successful payload wrapper
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/orders/pending")
    public ResponseEntity<ApiResponse<List<PendingOrderRequestResponse>>> getPendingOrderRequests() {

        // 🟢 FIXED: Routed directly through your unified inventoryService instance
        List<PendingOrderRequestResponse> data = inventoryService.getPendingOrderRequests();

        return ResponseEntity.ok(new ApiResponse<List<PendingOrderRequestResponse>>(
                "SUCCESS",
                "Pending order requests fetched successfully",
                data
        ));
    }

    /**
     * Fetch complete structured details data model for a single order request card screen view
     */
    @GetMapping("/admin/orders/details/{id}")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> getAdminOrderDetailById(@PathVariable Long id) {

        AdminOrderDetailResponse detailedPayload = inventoryService.getAdminOrderDetailById(id);

        // 🟢 FIXED: Removed extra '>' and separated the message and "SUCCESS" status with a comma
        return ResponseEntity.ok(new ApiResponse<AdminOrderDetailResponse>(
                "SUCCESS",
                "Order detailed insights view model Provided successfully",
                detailedPayload
        ));
    }

    @PostMapping("/rejectOrder")
    public ResponseEntity<?> rejectOrder(
            @RequestBody AdminOrderBulkRejectRequest request
    ) {
        Map<String, Object> response = inventoryService.rejectOrder(request);
        return ResponseEntity.ok(response);
    }
    /**
     * Get list of all approved orders for the "Confirmed" tab view dashboard
     */
    @GetMapping("/admin/orders/confirmed")
    public ResponseEntity<ApiResponse<List<ConfirmedOrderResponse>>> getConfirmedOrderRequests() {
        List<ConfirmedOrderResponse> data = inventoryService.getConfirmedOrderRequests();
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Confirmed order requests fetched successfully",
                data
        ));
    }

    /**
     * Get deep insights view details card data matching the single "Confirmed Details" layout screen
     */
    @GetMapping("/admin/orders/confirmed/details/{id}")
    public ResponseEntity<ApiResponse<ConfirmedOrderDetailResponse>> getConfirmedOrderDetailById(@PathVariable Long id) {
        ConfirmedOrderDetailResponse data = inventoryService.getConfirmedOrderDetailById(id);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Confirmed order deep details insights compiled successfully",
                data
        ));
    }

    /**
     * Get a list of all rejected requests for the "Rejected" tab dashboard list view
     */
    @GetMapping("/admin/orders/rejected")
    public ResponseEntity<ApiResponse<List<RejectedOrderResponse>>> getRejectedOrderRequests() {
        List<RejectedOrderResponse> data = inventoryService.getRejectedOrderRequests();
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Rejected order requests tab dataset fetched successfully",
                data
        ));
    }

    /**
     * Get details for a single rejected order card timeline view screen matching the UI specs
     */
    @GetMapping("/admin/orders/rejected/details/{id}")
    public ResponseEntity<ApiResponse<RejectedOrderDetailResponse>> getRejectedOrderDetailById(@PathVariable Long id) {
        RejectedOrderDetailResponse data = inventoryService.getRejectedOrderDetailById(id);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Rejected order specific details dataset provided successfully",
                data
        ));
    }

    /**
     * Get list dataset of all closed requests for the "Delivered" tab panel dashboard view
     */
    @GetMapping("/admin/orders/delivered")
    public ResponseEntity<ApiResponse<List<DeliveredOrderResponse>>> getDeliveredOrderRequests() {
        List<DeliveredOrderResponse> data = inventoryService.getDeliveredOrderRequests();
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Delivered order requests tab dataset fetched successfully",
                data
        ));
    }

    /**
     * Get specific milestone insight tracking metrics matching the detailed "Delivered Details" layout screen
     */
    @GetMapping("/admin/orders/delivered/details/{id}")
    public ResponseEntity<ApiResponse<DeliveredOrderDetailResponse>> getDeliveredOrderDetailById(@PathVariable Long id) {
        DeliveredOrderDetailResponse data = inventoryService.getDeliveredOrderDetailById(id);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Delivered order specific detail metrics provided successfully",
                data
        ));
    }

    /**
     * Fetch complete unified dashboard list for the "Transactions -> Subscriptions" tab flow layout screen
     */
    @GetMapping("/admin/transactions/subscriptions")
    public ResponseEntity<ApiResponse<List<SubscriptionMonthWiseResponse>>> getSubscriptionTransactionsDashboard() {

        List<SubscriptionMonthWiseResponse> data = inventoryService.getSubscriptionTransactionsDashboard();

        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Subscription details list fetched successfully.",
                data
        ));
    }

    @GetMapping("/getDamagedContainersByUser")
    public ResponseEntity<ApiResponse<List<DamageContainerMonthWiseResponse>>> getDamageContainerMonthWiseDetailsByUser(
            @RequestParam Long userId) {

        return ResponseEntity.ok(inventoryService.getDamageContainerMonthWiseDetailsByUserId(userId));
    }
    @PostMapping("/internal/container-fees")
    public ResponseEntity<Map<Integer, BigDecimal>> getContainerExtendFees(
            @RequestBody List<Integer> containerTypeIds) {

        Map<Integer, BigDecimal> feeMap = new HashMap<>();
        if (containerTypeIds == null || containerTypeIds.isEmpty()) {
            return ResponseEntity.ok(feeMap);
        }

        List<ContainerType> types = containerTypeRepository.findAllByIdIn(containerTypeIds);

        for (ContainerType type : types) {
            BigDecimal fee = type.getExtendFee() != null ? type.getExtendFee() : BigDecimal.ZERO;
            feeMap.put(type.getId(), fee);
        }

        return ResponseEntity.ok(feeMap);
    }

    /**
     * Internal endpoint for Order-Service to fetch full container data + fees for preview screens
     */
    @PostMapping("/internal/container-extension-details")
    public ResponseEntity<List<ContainerExtensionInfo>> getContainerExtensionDetails(
            @RequestBody List<Integer> containerTypeIds) {

        List<ContainerExtensionInfo> details = new ArrayList<>();
        if (containerTypeIds == null || containerTypeIds.isEmpty()) {
            return ResponseEntity.ok(details);
        }

        List<ContainerType> types = containerTypeRepository.findAllByIdIn(containerTypeIds);
        for (ContainerType type : types) {
            details.add(ContainerExtensionInfo.builder()
                    .id(type.getId())
                    .name(type.getName())
                    .productId(type.getProductId())
                    .capacityMl(type.getCapacityMl())
                    .imageUrl(type.getImageUrl())
                    .extendFee(type.getExtendFee() != null ? type.getExtendFee() : BigDecimal.ZERO) // 🟢 FIXED
                    .build());
        }
        return ResponseEntity.ok(details);
    }

    @GetMapping("/admin/transactions/sold-dashboard")
    public ResponseEntity<com.inventory.response.ApiResponse<List<com.inventory.dto.SoldMonthWiseDashboardResponse>>> getSoldContainersDashboard() {

        List<com.inventory.dto.SoldMonthWiseDashboardResponse> dataset = inventoryService.getSoldContainersComprehensiveDashboard();

        return ResponseEntity.ok(new com.inventory.response.ApiResponse<>(
                "SUCCESS",
                "sold items statement records provided successfully",
                dataset
        ));
    }
}
