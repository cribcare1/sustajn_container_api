package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.Constant.InventoryConstant;
import com.inventory.dto.ErrorResponses;
import com.inventory.dto.ProductResponse;
import com.inventory.exception.InventoryException;
import com.inventory.request.*;
import com.inventory.service.AdminRestaurantOrderService;
import com.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public List<ProductResponse> getProductsByIds(@RequestBody List<Integer> ids) {
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
    @GetMapping("/getAvailableContainers/{restaurantId}")
    public ResponseEntity<?> getAvailableContainers(
            @PathVariable("restaurantId") Long restaurantId
    ) {
        Map<String, Object> response = adminOrderService.getAvailableContainers(restaurantId);
        return ResponseEntity.ok(response);
    }
}
