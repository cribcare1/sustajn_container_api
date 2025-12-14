package com.sustajn.oderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sustajn.oderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;
    private final ObjectMapper objectMapper;

/*    @PostMapping("/borrowContainer")
    public ResponseEntity<?> borrowContainer(
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
    }*/


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

}
