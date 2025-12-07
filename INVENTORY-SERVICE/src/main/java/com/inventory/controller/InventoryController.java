package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.Constant.InventoryConstant;
import com.inventory.dto.ErrorResponses;
import com.inventory.exception.InventoryException;
import com.inventory.request.AdminRestaurantInventoryBulkRequest;
import com.inventory.request.ContainerTypeRequest;
import com.inventory.request.InventoryBulkAddRequest;
import com.inventory.request.InventoryUpdateRequest;
import com.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;
    private final ObjectMapper objectMapper;

    @PostMapping("/saveOrUpdateContainerType")
    public ResponseEntity<?> saveOrUpdateContainerType(
            @RequestPart("request") ContainerTypeRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
//            // Convert JSON string to ContainerTypeRequest
//            ContainerTypeRequest request = objectMapper.readValue(requestString, ContainerTypeRequest.class);

            Map<String, Object> response = service.saveOrUpdate(request, file);
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
        Map<String, Object> response = service.getActiveContainerTypes();
        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete (mark inactive) a Container Type by ID
     * @param id Container Type ID
     * @return Map with success status, message, and deleted data
     */
    @PostMapping("/delete-container-type/{id}")
    public ResponseEntity<?> deleteContainerType(@PathVariable Integer id) {
        Map<String, Object> response = service.deleteContainerType(id);
        return ResponseEntity.ok(response);
    }




    // -----------------------------------------
    //  UPDATE INVENTORY
    // -----------------------------------------
    @PostMapping("/updateInventory")
    public ResponseEntity<?> updateInventory(@RequestBody InventoryUpdateRequest request) {
        return ResponseEntity.ok(service.updateInventory(request));
    }

    // -----------------------------------------
    //  GET ALL ACTIVE INVENTORY WITH CONTAINER DETAILS
    // -----------------------------------------
    @GetMapping("/getAllActiveInventory")
    public ResponseEntity<?> getAllActiveInventory() {
        return ResponseEntity.ok(service.getAllActiveInventory());
    }

    @PostMapping("/restaurant/addRestaurantInventory")
    public ResponseEntity<Map<String, Object>> addRestaurantInventoryBulk(
            @RequestBody AdminRestaurantInventoryBulkRequest request) {

        try {
            Map<String, Object> response = service.addRestaurantInventoryBulk(request);
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
            Map<String, Object> response = service.getRestaurantInventory(restaurantId);
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
}
