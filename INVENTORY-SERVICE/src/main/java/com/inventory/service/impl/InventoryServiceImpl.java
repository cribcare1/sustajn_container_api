package com.inventory.service.impl;

import com.inventory.Constant.InventoryConstant;
import com.inventory.dto.ContainerTypeResponse;
import com.inventory.dto.InventoryWithContainerResponse;
import com.inventory.dto.RestaurantInventoryViewResponse;
import com.inventory.entity.AdminInventoryMaster;
import com.inventory.entity.AdminInventoryMasterAudit;
import com.inventory.entity.AdminRestaurantInventoryDetails;
import com.inventory.entity.ContainerType;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.InventoryException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.AdminInventoryMasterAuditRepository;
import com.inventory.repository.AdminInventoryMasterRepository;
import com.inventory.repository.AdminRestaurantInventoryDetailsRepository;
import com.inventory.repository.ContainerTypeRepository;
import com.inventory.request.AdminRestaurantInventoryBulkRequest;
import com.inventory.request.ContainerTypeRequest;
import com.inventory.request.InventoryBulkAddRequest;
import com.inventory.request.InventoryUpdateRequest;
import com.inventory.service.InventoryService;
import com.inventory.util.DateTimeUtil;
import com.inventory.util.FileStorageUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ContainerTypeRepository repository;
    private final FileStorageUtil fileStorageUtil;

    private final AdminInventoryMasterRepository masterRepo;
    private final AdminInventoryMasterAuditRepository auditRepo;
    private final AdminRestaurantInventoryDetailsRepository adminRestaurantInventoryDetailsRepository;

    public Map<String, Object> saveOrUpdate(ContainerTypeRequest request, MultipartFile file) {

        ContainerType containerType;

        if (request.getId() != null) {
            // Update case
            containerType = repository.findById(request.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Container Type not found with ID: " + request.getId()));

            // Name changed? then validate unique
            if (request.getName() != null &&
                    !containerType.getName().equalsIgnoreCase(request.getName()) &&
                    repository.existsByNameIgnoreCase(request.getName())) {

                throw new DuplicateResourceException("Container type name already exists: " + request.getName());
            }

        } else {
            // Create case
            if (repository.existsByNameIgnoreCase(request.getName())) {
                throw new DuplicateResourceException("Container type name already exists: " + request.getName());
            }
            containerType = new ContainerType();
        }

        // Update only fields that are not null
        if (request.getName() != null) containerType.setName(request.getName());
        if (request.getDescription() != null) containerType.setDescription(request.getDescription());
        if (request.getCapacityMl() != null) containerType.setCapacityMl(request.getCapacityMl());
        if (request.getMaterial() != null) containerType.setMaterial(request.getMaterial());
        if (request.getColour() != null) containerType.setColour(request.getColour());
        if (request.getLengthCm() != null) containerType.setLengthCm(request.getLengthCm());
        if (request.getWidthCm() != null) containerType.setWidthCm(request.getWidthCm());
        if (request.getHeightCm() != null) containerType.setHeightCm(request.getHeightCm());
        if (request.getWeightGrams() != null) containerType.setWeightGrams(request.getWeightGrams());
        if (request.getFoodSafe() != null) containerType.setFoodSafe(request.getFoodSafe());
        if (request.getDishwasherSafe() != null) containerType.setDishwasherSafe(request.getDishwasherSafe());
        if (request.getMicrowaveSafe() != null) containerType.setMicrowaveSafe(request.getMicrowaveSafe());
        if (request.getMaxTemperature() != null) containerType.setMaxTemperature(request.getMaxTemperature());
        if (request.getMinTemperature() != null) containerType.setMinTemperature(request.getMinTemperature());
        if (request.getLifespanCycle() != null) containerType.setLifespanCycle(request.getLifespanCycle());
        containerType.setStatus(InventoryConstant.ACTIVE);
        // Update image only when file provided
        if (file != null && !file.isEmpty()) {
            String url = fileStorageUtil.uploadFile(file);
            containerType.setImageUrl(url);
        }

        ContainerType saved = repository.save(containerType);

        Map<String, Object> response = new HashMap<>();
        response.put(InventoryConstant.STATUS, InventoryConstant.SUCCESS);
        response.put(InventoryConstant.MESSAGE, request.getId() == null ?
                "Container Type added successfully" :
                "Container Type updated successfully");
        return response;
    }

    @Override
    public Map<String, Object> getActiveContainerTypes() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ContainerTypeResponse> activeContainers = repository.findActiveContainerTypes();

            if (activeContainers.isEmpty()) {
                response.put(InventoryConstant.STATUS, InventoryConstant.SUCCESS);
                response.put(InventoryConstant.MESSAGE, "No active container types found.");
                response.put(InventoryConstant.INVENTORY_MASTER_DATA, activeContainers);
            } else {
                response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
                response.put(InventoryConstant.MESSAGE, "Active container types fetched successfully.");
                response.put(InventoryConstant.INVENTORY_MASTER_DATA, activeContainers);
            }

        } catch (Exception e) {
            // Log exception (use proper logger in real apps)
            System.err.println("Error fetching active container types: " + e.getMessage());

            response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
            response.put(InventoryConstant.MESSAGE, "Unable to fetch active container types.");

            response.put(InventoryConstant.INVENTORY_MASTER_DATA, null);
        }

        return response;
    }


    @Override
    public Map<String, Object> deleteContainerType(Integer id) {
        Map<String, Object> response = new HashMap<>();

        try {
            ContainerType containerType = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Container Type not found with ID: " + id));

            if (InventoryConstant.INACTIVE.equalsIgnoreCase(containerType.getStatus())) {
                response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);

                response.put(InventoryConstant.MESSAGE, "Container Type is already inactive.");
                return response;
            }

            containerType.setStatus(InventoryConstant.INACTIVE);
            repository.save(containerType);

            response.put(InventoryConstant.STATUS, InventoryConstant.SUCCESS);
            response.put(InventoryConstant.MESSAGE, "Container Type marked as inactive successfully.");
            response.put("data", containerType);

        } catch (Exception e) {
            System.err.println("Error deleting container type: " + e.getMessage());
            response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
            response.put(InventoryConstant.MESSAGE, "Unable to delete Container Type.");
            response.put("data", null);
        }

        return response;
    }



    @Transactional
    public Map<String, Object> addMultipleInventories(InventoryBulkAddRequest request) {

        if (request.getContainers() == null || request.getContainers().isEmpty()) {
            throw new InventoryException("Container list cannot be empty");
        }

        int totalAdded = 0;

        for (InventoryBulkAddRequest.InventorySingleAddRequest item : request.getContainers()) {

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new InventoryException("Quantity must be greater than zero");
            }

            AdminInventoryMaster master = masterRepo.findByContainerTypeId(item.getContainerTypeId())
                    .orElseGet(() -> {
                        AdminInventoryMaster newRecord = new AdminInventoryMaster();
                        newRecord.setContainerTypeId(item.getContainerTypeId());
                        newRecord.setTotalContainers(0);
                        newRecord.setAvailableContainers(0);
                        newRecord.setCreatedBy(request.getCreatedBy());
                        newRecord.setStatus("active");
                        return newRecord;
                    });

            int updatedTotal = master.getTotalContainers() + item.getQuantity();
            int updatedAvailable = master.getAvailableContainers() + item.getQuantity();

            master.setTotalContainers(updatedTotal);
            master.setAvailableContainers(updatedAvailable);

            masterRepo.save(master);

            AdminInventoryMasterAudit audit = AdminInventoryMasterAudit.builder()
                    .inventoryMasterId(master.getId())
                    .quantityChange(item.getQuantity())
                    .balanceAfter(updatedAvailable)
                    .actionType("ADD")
                    .changedBy(request.getCreatedBy())
                    .build();

            auditRepo.save(audit);

            totalAdded += item.getQuantity();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Inventory added successfully");
        response.put("totalAdded", totalAdded);
        response.put("timestamp", DateTimeUtil.nowDubai());

        return response;
    }

    // ------------------------
    // 2. UPDATE SINGLE RECORD
    // ------------------------
    @Transactional
    @Override
    public Map<String, Object> updateInventory(InventoryUpdateRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {

            // Validate: ID missing
            if (request.getId() == null) {
                throw new InventoryException("Inventory ID is required");
            }

            // Fetch master record
            AdminInventoryMaster master = masterRepo.findById(request.getId())
                    .orElseThrow(() -> new InventoryException("Inventory record not found for ID: " + request.getId()));

            // Validate quantity
            if (request.getNewQuantity() == null || request.getNewQuantity() < 0) {
                throw new InventoryException("New quantity must be a non-negative number");
            }

            Integer oldTotal = master.getTotalContainers();
            Integer oldAvailable = master.getAvailableContainers();

            // Difference = newTotal - oldTotal
            Integer difference = request.getNewQuantity() - oldTotal;

            // Update master
            master.setTotalContainers(request.getNewQuantity());
            master.setAvailableContainers(oldAvailable + difference);
            master.setUpdatedBy(request.getUpdatedBy());

            masterRepo.save(master);

            // Create AUDIT entry
            AdminInventoryMasterAudit audit = AdminInventoryMasterAudit.builder()
                    .inventoryMasterId(master.getId())
                    .quantityChange(difference)
                    .balanceAfter(master.getAvailableContainers())
                    .actionType(difference >= 0 ? "ADD" : "REMOVE")
                    .changedBy(request.getUpdatedBy())
                    .build();

            auditRepo.save(audit);

            // SUCCESS response
            response.put("status", "success");
            response.put("message", "Inventory updated successfully");
            response.put("difference", difference);
            response.put("updatedTotal", master.getTotalContainers());
            response.put("updatedAvailable", master.getAvailableContainers());
            response.put("timestamp", LocalDateTime.now());

        } catch (InventoryException e) {
            // USER / VALIDATION EXCEPTION
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            // UNEXPECTED EXCEPTION
            response.put("status", "error");
            response.put("message", "Something went wrong while updating inventory");
            response.put("details", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
        }

        return response;
    }


    @Override
    public Map<String, Object> getAllActiveInventory() {

        try {
            List<InventoryWithContainerResponse> list =
                    masterRepo.getActiveInventoryWithContainerDetails();

            if (list == null || list.isEmpty()) {
                throw new InventoryException("No active inventory records found");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Active inventory fetched successfully");
            response.put("count", list.size());
            response.put("data", list);
            response.put("timestamp", LocalDateTime.now());

            return response;

        } catch (InventoryException ex) {
            throw ex; // handled by global exception handler

        } catch (Exception ex) {
            throw new InventoryException("Failed to fetch inventory records: " + ex.getMessage());
        }
    }


    @Transactional
    @Override
    public Map<String, Object> addRestaurantInventoryBulk(AdminRestaurantInventoryBulkRequest request) {

        if (request.getContainers() == null || request.getContainers().isEmpty()) {
            throw new InventoryException("Container list cannot be empty");
        }

        // Extract all containerTypeIds from request
        Set<Integer> typeIds = request.getContainers().stream()
                .map(AdminRestaurantInventoryBulkRequest.ContainerEntry::getContainerTypeId)
                .collect(Collectors.toSet());

        // === FETCH ALL INVENTORY MASTERS IN ONE QUERY ===
        List<AdminInventoryMaster> masterList = masterRepo.findByContainerTypeIdIn(typeIds);

        // Convert to a map for O(1) lookup
        Map<Integer, AdminInventoryMaster> masterMap = masterList.stream()
                .collect(Collectors.toMap(AdminInventoryMaster::getContainerTypeId, m -> m));

        // Validate missing types
        for (Integer typeId : typeIds) {
            if (!masterMap.containsKey(typeId)) {
                throw new InventoryException("Inventory record not found for containerTypeId: " + typeId);
            }
        }

        int totalEntries = 0;

        // === PROCESS BULK ENTRIES ===
        for (AdminRestaurantInventoryBulkRequest.ContainerEntry entry : request.getContainers()) {

            if (entry.getContainerCount() == null || entry.getContainerCount() <= 0) {
                throw new InventoryException("Container count must be greater than zero");
            }

            AdminInventoryMaster master = masterMap.get(entry.getContainerTypeId());
            Integer available = master.getAvailableContainers();
            Integer count = entry.getContainerCount();
            String action = entry.getActionType().toUpperCase();

            switch (action) {

                case "BORROW":
                    if (available < count) {
                        throw new InventoryException(
                                "Not enough available containers for containerTypeId: "
                                        + entry.getContainerTypeId() +
                                        ". Available: " + available + ", Required: " + count
                        );
                    }
                    master.setAvailableContainers(available - count);
                    break;

                case "RETURN":
                    master.setAvailableContainers(available + count);
                    break;

                default:
                    throw new InventoryException("Invalid actionType: " + action + ". Allowed: BORROW or RETURN");
            }

            master.setUpdatedBy(request.getCreatedBy());
        }

        // === SAVE ALL UPDATED MASTER INVENTORY RECORDS ===
        masterRepo.saveAll(masterMap.values());

        // === SAVE LOG RECORDS (ONLY FOR RESTAURANT) — NO AUDIT TABLE ===
        List<AdminRestaurantInventoryDetails> logs = request.getContainers().stream()
                .map(entry -> AdminRestaurantInventoryDetails.builder()
                        .restaurantId(request.getRestaurantId())
                        .containerTypeId(entry.getContainerTypeId())
                        .containerCount(entry.getContainerCount())
                        .actionType(entry.getActionType().toUpperCase())
                        .createdBy(request.getCreatedBy())
                        .updatedBy(request.getCreatedBy())
                        .build()
                ).collect(Collectors.toList());

        adminRestaurantInventoryDetailsRepository.saveAll(logs);

        totalEntries = logs.size();

        // === RESPONSE ===
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Restaurant inventory updated successfully");
        response.put("totalEntries", totalEntries);
        return response;
    }


    @Override
    public Map<String, Object> getRestaurantInventory(Long restaurantId) {

        Map<String, Object> response = new HashMap<>();

        if (restaurantId == null || restaurantId <= 0) {
            throw new InventoryException("Invalid restaurantId");
        }

        List<RestaurantInventoryViewResponse> records =
                adminRestaurantInventoryDetailsRepository.getRestaurantInventoryLogs(restaurantId);

        if (records == null || records.isEmpty()) {
            throw new InventoryException("No inventory transactions found for restaurantId: " + restaurantId);
        }

        response.put("status", "success");
        response.put("restaurantId", restaurantId);
        response.put("timestamp", LocalDateTime.now());
        response.put("data", records);

        return response;
    }

    @Override
    public Map<String, Object> getAdminDashboardData() {
        return Map.of();
    }

}
