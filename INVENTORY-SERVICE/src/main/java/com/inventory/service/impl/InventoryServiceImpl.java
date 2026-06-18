package com.inventory.service.impl;

import com.inventory.Constant.InventoryConstant;
import com.inventory.dto.*;
import com.inventory.entity.*;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.InventoryException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.feignClient.AuthFeignClient;
import com.inventory.feignClient.OrderFeignClient;
import com.inventory.feignClient.service.NotificationFeignClientService;
import com.inventory.repository.*;
import com.inventory.request.*;
import com.inventory.response.ApiResponse;
import com.inventory.response.RestaurantContainerInventoryResponse;
import com.inventory.service.InventoryService;
import com.inventory.util.DateTimeUtil;
import com.inventory.util.FileStorageUtil;
import com.inventory.util.InventoryUtils;
import com.sustajn.oderservice.dto.UserResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ContainerTypeRepository containerTypeRepository;
    private final FileStorageUtil fileStorageUtil;
    private final NotificationFeignClientService notificationFeignClientService;
    private final AdminInventoryMasterRepository masterRepo;
    private final AdminInventoryMasterAuditRepository auditRepo;
    private final AdminRestaurantInventoryDetailsRepository adminRestaurantInventoryDetailsRepository;
    private final RestaurantInventoryMasterRepository restaurantInventoryMasterRepository;
    private final DamagedContainerRepository damagedContainerRepository;
    private final DamagedContainerImagesRepository damagedContainerImagesRepository;
    private final SoldContainerRepository soldContainerRepository;
    private final AuthFeignClient authFeignClient;
    private final OrderFeignClient orderFeignClient;

    public Map<String, Object> saveOrUpdate(ContainerTypeRequest request, MultipartFile file) {

        ContainerType containerType;

        if (request.getId() != null) {
            // Update case
            containerType = containerTypeRepository.findById(request.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Container Type not found with ID: " + request.getId()));

            // Name changed? then validate unique
            if (request.getName() != null &&
                    !containerType.getName().equalsIgnoreCase(request.getName()) &&
                    containerTypeRepository.existsByNameIgnoreCase(request.getName())) {

                throw new DuplicateResourceException("Container type name already exists: " + request.getName());
            }

        } else {
            // Create case
            if (containerTypeRepository.existsByNameIgnoreCase(request.getName())) {
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
            String url = notificationFeignClientService.uploadImage("container",file);
            containerType.setImageUrl(url);
        }

        ContainerType saved = containerTypeRepository.save(containerType);

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
            List<ContainerTypeResponse> activeContainers = containerTypeRepository.findActiveContainerTypes();

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
            ContainerType containerType = containerTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Container Type not found with ID: " + id));

            if (InventoryConstant.INACTIVE.equalsIgnoreCase(containerType.getStatus())) {
                response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);

                response.put(InventoryConstant.MESSAGE, "Container Type is already inactive.");
                return response;
            }

            containerType.setStatus(InventoryConstant.INACTIVE);
            containerTypeRepository.save(containerType);

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
            response.put("inventory_data", list);
            return response;

        } catch (InventoryException ex) {
            throw ex; // handled by global exception handler

        } catch (Exception ex) {
            throw new InventoryException("Failed to fetch inventory records: " + ex.getMessage());
        }
    }

    @Override
    public Map<String, Object> getAllContainerTypes() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. Fetch the data using your original, untouched class
            List<ContainerType> allContainers = containerTypeRepository.findAll();

            // 2. Map it to the new file you just created
            List<ContainerTypeWithCount> combinedDataList = allContainers.stream().map(container -> {

                // Fetch the count from Admin Inventory Master
                AdminInventoryMaster master = masterRepo.findByContainerTypeId(container.getId())
                        .orElse(null);

                int total = (master != null && master.getTotalContainers() != null) ? master.getTotalContainers() : 0;
                int available = (master != null && master.getAvailableContainers() != null) ? master.getAvailableContainers() : 0;

                // 3. Return the new object (Original Data, Total Count, Available Count)
                return new ContainerTypeWithCount(container, total, available);

            }).collect(Collectors.toList());

            // 4. Put the new list into the response
            response.put("status", "SUCCESS");
            response.put("message", "All container types fetched successfully");
            response.put("data", combinedDataList);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to fetch container types: " + e.getMessage());
            response.put("data", null);
        }
        return response;
    }

    @Override
    public Map<String, Object> getContainerTypeById(Integer id) {
        Map<String, Object> response = new java.util.HashMap<>();
        try {
            // containerTypeRepository should be your JPA interface tracking the ContainerType entity
            return containerTypeRepository.findById(id)
                    .map(container -> {
                        response.put("status", "success");
                        response.put("message", "Container type found");
                        response.put("data", container);
                        return response;
                    })
                    .orElseGet(() -> {
                        response.put("status", "error");
                        response.put("message", "Container type not found for ID: " + id);
                        response.put("data", null);
                        return response;
                    });
        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Database retrieval failure: " + ex.getMessage());
            response.put("data", null);
            return response;
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





    @Transactional
    @Override
    public Map<String, Object> addContainer(AddContainerRequest request, MultipartFile image) {

        Map<String, Object> response = new HashMap<>();
        String imageUrl = null;

        try {
            // 1️⃣ Upload image
            if (image != null && !image.isEmpty()) {
                 imageUrl = notificationFeignClientService.uploadImage(InventoryConstant.CONTAINER,image);
            }

            // 2️⃣ Check container existence
            ContainerType containerType = containerTypeRepository
                    .findByNameIgnoreCase(request.getContainerName())
                    .orElse(null);

            boolean isNewContainer = false;

            // 3️⃣ Create container if not exists
            if (containerType == null) {
                isNewContainer = true;

                containerType = ContainerType.builder()
                        .name(request.getContainerName())
                        .description(request.getDescription())
                        .capacityMl(request.getCapacityMl())
                        .material(request.getMaterial())
                        .productId(request.getProductId())
                        .colour(request.getColour())
                        .lengthCm(request.getLengthCm())
                        .widthCm(request.getWidthCm())
                        .heightCm(request.getHeightCm())
                        .weightGrams(request.getWeightGrams())
                        .foodSafe(request.getFoodSafe())
                        .dishwasherSafe(request.getDishwasherSafe())
                        .microwaveSafe(request.getMicrowaveSafe())
                        .maxTemperature(request.getMaxTemperature())
                        .minTemperature(request.getMinTemperature())
                        .lifespanCycle(request.getLifespanCycle())
                        .costPerUnit(request.getPrice())
                        .imageUrl(imageUrl)
                        .status("active")
                        .build();

                containerType = containerTypeRepository.save(containerType);
            }

            // 4️⃣ Fetch or create inventory master
            ContainerType finalContainerType = containerType;
            AdminInventoryMaster inventory = masterRepo
                    .findByContainerTypeId(containerType.getId())
                    .orElseGet(() -> {
                        AdminInventoryMaster inv = new AdminInventoryMaster();
                        inv.setContainerTypeId(finalContainerType.getId());
                        inv.setTotalContainers(0);
                        inv.setAvailableContainers(0);
                        inv.setCreatedBy(request.getUserId());
                        inv.setStatus("active");
                        return inv;
                    });

            // 5️⃣ Quantity logic
            int addedQty = request.getQuantity();
            int newTotal = inventory.getTotalContainers() + addedQty;
            int newAvailable = inventory.getAvailableContainers() + addedQty;

            inventory.setTotalContainers(newTotal);
            inventory.setAvailableContainers(newAvailable);
            inventory.setUpdatedBy(request.getUserId());

            masterRepo.save(inventory);

            // 6️⃣ Audit
            AdminInventoryMasterAudit audit = AdminInventoryMasterAudit.builder()
                    .inventoryMasterId(inventory.getId())
                    .quantityChange(addedQty)
                    .balanceAfter(newAvailable)
                    .actionType("ADD")
                    .reason(isNewContainer ? "New container added" : "Stock added")
                    .changedBy(request.getUserId())
                    .build();

            auditRepo.save(audit);

            // ✅ SUCCESS RESPONSE
            response.put("status", "success");
            response.put("message", isNewContainer
                    ? "Container created and inventory added successfully"
                    : "Inventory updated successfully");
            response.put("containerTypeId", containerType.getId());
            response.put("totalContainers", newTotal);
            response.put("availableContainers", newAvailable);

        } catch (Exception ex) {

            // Rollback uploaded image
            if (imageUrl != null) {
                notificationFeignClientService.deleteContainer(InventoryConstant.CONTAINER,imageUrl);
            }

            response.put("status", "error");
            response.put("message", "Failed to add container"+ex.getMessage());
        }

        return response;
    }
    @Override
    public List<DetailedSoldMonthResponse> getDetailedSoldHistoryByRestaurant(Long restaurantId) {

        // 1. Fetch real dates from Order Service
        List<SoldHistoryRawData> rawData = orderFeignClient.getRealSoldHistoryDates(restaurantId);

        if (rawData == null || rawData.isEmpty()) {
            return new ArrayList<>();
        }

        java.time.format.DateTimeFormatter monthYearFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM-yyyy", java.util.Locale.ENGLISH);
        java.time.format.DateTimeFormatter dotFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // 2. GROUP BY MONTH ONLY
        java.util.Map<String, List<com.inventory.dto.SoldHistoryRawData>> groupedByMonth = rawData.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        data -> data.getSoldAt().format(monthYearFormatter),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        List<com.inventory.dto.DetailedSoldMonthResponse> finalResponse = new java.util.ArrayList<>();

        // 3. PROCESS EACH MONTH
        for (java.util.Map.Entry<String, List<com.inventory.dto.SoldHistoryRawData>> monthEntry : groupedByMonth.entrySet()) {
            String monthYear = monthEntry.getKey();
            List<com.inventory.dto.SoldHistoryRawData> monthItems = monthEntry.getValue();

            int monthTotal = 0;

            // 👉 The direct list of products for this month
            List<com.inventory.dto.DetailedSoldProductResponse> productsList = new java.util.ArrayList<>();

            for (com.inventory.dto.SoldHistoryRawData item : monthItems) {
                monthTotal += item.getSoldQuantity();

                // Fetch product details
                com.inventory.entity.ContainerType product = containerTypeRepository.findById(item.getProductId().intValue()).orElse(null);

                // Add directly to the products list
                productsList.add(com.inventory.dto.DetailedSoldProductResponse.builder()
                        .productId(item.getProductId().intValue())
                        .productName(product != null ? product.getName() : "Unknown")
                        .productDescription(product != null ? product.getDescription() : "")
                        .productImageUrl(product != null ? product.getImageUrl() : "")
                        .capacity(product != null ? product.getCapacityMl() : 0)
                        .productUniqueId(product != null ? product.getProductId() : "")
                        .soldAmount(item.getUnitPrice() * item.getSoldQuantity())
                        .soldQuantity(item.getSoldQuantity())
                        // The 3 Dates for the UI
                        .borrowedOn(item.getBorrowedAt() != null ? item.getBorrowedAt().format(dotFormatter) : "")
                        .dueOn(item.getDueDate() != null ? item.getDueDate().format(dotFormatter) : "")
                        .soldOn(item.getSoldAt() != null ? item.getSoldAt().format(dotFormatter) : "")
                        .build());
            }

            // Build the final Month Object
            finalResponse.add(new com.inventory.dto.DetailedSoldMonthResponse(monthYear, monthTotal, productsList));
        }

        return finalResponse;
    }
//    @Override
//    public List<ProductResponse> getProductsByIds(List<Integer> ids) {
//        if (ids == null || ids.isEmpty()) {
//            throw new IllegalArgumentException("Product ID list cannot be empty");
//        }
//        return containerTypeRepository.findProductResponsesByIds(ids);
//    }


    public ApiResponse<List<ProductResponse>> getProductsByIds(
            @RequestBody List<Integer> ids) {

        try {

            // If ids null or empty → return empty list safely
            if (ids == null || ids.isEmpty()) {
                return new ApiResponse<>(
                        "No product ids provided",
                        "SUCCESS",
                        new ArrayList<>()
                );
            }

            List<ContainerType> products = containerTypeRepository.findAllById(ids);

            List<ProductResponse> responseList = products.stream()
                    .map(product -> new ProductResponse(
                            product.getId(),
                            product.getName(),
                            product.getDescription(),
                            product.getCostPerUnit().doubleValue(),
                            product.getImageUrl(),
                            product.getCapacityMl(),
                            product.getProductId()
                    ))
                    .collect(Collectors.toList());

            // 🔥 IMPORTANT: Always return empty list instead of null
            return new ApiResponse<>(
                    responseList.isEmpty()
                            ? "No products found"
                            : "Products fetched successfully",
                    "SUCCESS",
                    responseList
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(
                    "Failed to fetch products",
                    "ERROR",
                    new ArrayList<>()
            );
        }
    }


    @Override
    public ApiResponse<List<RestaurantContainerInventoryResponse>> getRestaurantContainerInventoryByRestaurantId(Long restaurantId) {
        try {
                List<RestaurantContainerInventoryResponse> restaurantContainerInventoryResponses = adminRestaurantInventoryDetailsRepository.getRestaurantContainerInventoryByRestaurantId(restaurantId);

                if (CollectionUtils.isEmpty(restaurantContainerInventoryResponses)) {
                    return new ApiResponse<>(InventoryConstant.SUCCESS, "No inventory data found for restaurantId: " + restaurantId, null);
                }
                return new ApiResponse<>(InventoryConstant.SUCCESS, "Inventory data found successfully ", restaurantContainerInventoryResponses);
        } catch (Exception e) {
            return new ApiResponse<>(InventoryConstant.ERROR, "Failed to fetch inventory data for restaurantId: " + restaurantId, null);
        }
    }

    @Override
    public ApiResponse<List<RestaurantInventoryMaster>> reduceAvailableContainers(ReduceInventoryRequest request) {
        try {
            Long restaurantId = request.getRestaurantId();
            Map<Integer, Integer> qtyMap = request.getContainerQtyMap();

            List<RestaurantInventoryMaster> masters =
                    restaurantInventoryMasterRepository
                            .findAllByRestaurantIdAndContainerTypeIdIn(
                                    restaurantId, qtyMap.keySet());

            for (RestaurantInventoryMaster master : masters) {
                int reduceQty = qtyMap.get(master.getContainerTypeId());

                if (master.getAvailableContainers() < reduceQty) {
                    return new ApiResponse<>(InventoryConstant.ERROR, "Not enough containers available", null);
                }

                master.setAvailableContainers(
                        master.getAvailableContainers() - reduceQty);

                master.setBorrowedContainers(
                        master.getBorrowedContainers() + reduceQty);
            }

            List<RestaurantInventoryMaster> inventoryMasters = restaurantInventoryMasterRepository.saveAll(masters);
            return new ApiResponse<>(InventoryConstant.SUCCESS, "Available containers reduced successfully ", inventoryMasters);
        } catch (Exception e) {
            return new ApiResponse<>(InventoryConstant.ERROR, "Failed to reduce available container ", null);
        }
    }



    @Override
    @Transactional
    public ApiResponse<List<RestaurantInventoryMaster>> increaseContainers(
            ReduceInventoryRequest request) {

        try {

            Long restaurantId = request.getRestaurantId();

            Map<Integer, Integer> qtyMap =
                    request.getContainerQtyMap();

            List<RestaurantInventoryMaster> masters =
                    restaurantInventoryMasterRepository
                            .findAllByRestaurantIdAndContainerTypeIdIn(
                                    restaurantId,
                                    qtyMap.keySet()
                            );

            for (RestaurantInventoryMaster master : masters) {

                int returnQty =
                        qtyMap.get(master.getContainerTypeId());

//                // Safety check
//                if (master.getBorrowedContainers() < returnQty) {
//
//                    return new ApiResponse<>(
//                            InventoryConstant.ERROR,
//                            "Return quantity exceeds borrowed containers",
//                            null
//                    );
//                }

                // Increase available containers
                master.setAvailableContainers(
                        master.getAvailableContainers() + returnQty
                );

//                // Decrease borrowed containers
//                master.setBorrowedContainers(
//                        master.getBorrowedContainers() - returnQty
//                );

                // Increase returned containers
                master.setReturnedContainers(
                        master.getReturnedContainers() + returnQty
                );
            }

            List<RestaurantInventoryMaster> inventoryMasters =
                    restaurantInventoryMasterRepository.saveAll(masters);

            return new ApiResponse<>(
                    InventoryConstant.SUCCESS,
                    "Containers returned successfully",
                    inventoryMasters
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(
                    InventoryConstant.ERROR,
                    "Failed to return containers",
                    null
            );
        }
    }

    public Map<String, Object> checkAvailability(
            ReduceInventoryRequest request) {

        Long restaurantId = request.getRestaurantId();
        Map<Integer, Integer> qtyMap = request.getContainerQtyMap();

        List<RestaurantInventoryMaster> masters =
                restaurantInventoryMasterRepository
                        .findAllByRestaurantIdAndContainerTypeIdIn(
                                restaurantId, qtyMap.keySet());

        if (CollectionUtils.isEmpty(masters)) {
            return Map.of(InventoryConstant.STATUS, InventoryConstant.ERROR,
                    InventoryConstant.MESSAGE, "No inventory records found for the requested container types");
        }

        for (RestaurantInventoryMaster master : masters) {
            int requested = qtyMap.get(master.getContainerTypeId());

            if (master.getAvailableContainers() < requested) {
                return Map.of(InventoryConstant.STATUS, InventoryConstant.ERROR,
                        InventoryConstant.MESSAGE, "Not enough containers for type " + master.getContainerTypeId());
            }
        }

        return Map.of(InventoryConstant.STATUS, "success");
    }


    @Transactional
    public Map<String, Object> increaseAvailableContainers(
            ReduceInventoryRequest request) {

        List<RestaurantInventoryMaster> masters =
                restaurantInventoryMasterRepository
                        .findAllByRestaurantIdAndContainerTypeIdIn(
                                request.getRestaurantId(),
                                request.getContainerQtyMap().keySet()
                        );

        for (RestaurantInventoryMaster master : masters) {
            int qty = request.getContainerQtyMap()
                    .get(master.getContainerTypeId());

            master.setAvailableContainers(
                    master.getAvailableContainers() + qty);

            master.setBorrowedContainers(
                    master.getBorrowedContainers() - qty);
        }

        restaurantInventoryMasterRepository.saveAll(masters);
        return Map.of(InventoryConstant.STATUS, InventoryConstant.SUCCESS);
    }

    @Transactional(rollbackOn = Exception.class)
    @Override
    public ApiResponse<DamagedContainer> reportDamagedContainer(
            String reportDamagedContainerRequest,
            List<MultipartFile> damagedContainerImages) {

        ReportDamagedContainerRequest request = InventoryUtils.convertToJson(reportDamagedContainerRequest, ReportDamagedContainerRequest.class);

        ContainerType containerType = containerTypeRepository.findByProductId(request.getContainerTypeId())
                .orElseThrow(() -> new InventoryException("Container type not found"));

        if (request.getIsDamagedByRestaurant()){
            DamagedContainer damagedContainer = DamagedContainer.builder()
                    .containerTypeId(containerType.getId())
                    .remark(request.getRemark())
                    .restaurantId(request.getRestaurantId())
                    .damagedByRestaurant(true)
                    .damagedByUser(false)
                    .damagedCount(request.getDamagedCount())
                    .build();

            DamagedContainer savedDamagedContainer =
                    damagedContainerRepository.save(damagedContainer);

            if (!CollectionUtils.isEmpty(damagedContainerImages)) {
                List<String> imageUrls = damagedContainerImages.stream()
                        .filter(f -> f != null && !f.isEmpty())
                        .map(file -> notificationFeignClientService.uploadImage("damaged-container", file))
                        .toList();

                if (imageUrls.isEmpty()) {
                    throw new InventoryException("Image upload failed");
                }

                List<DamagedContainerImages> images = imageUrls.stream()
                        .map(url -> DamagedContainerImages.builder()
                                .damageId(savedDamagedContainer.getId())
                                .damageImageUrl(url)
                                .build())
                        .toList();

                damagedContainerImagesRepository.saveAll(images);
            }

            RestaurantInventoryMaster master =
                    restaurantInventoryMasterRepository
                            .findByRestaurantIdAndContainerTypeId(
                                    request.getRestaurantId(),
                                    containerType.getId());

            if (master == null) {
                throw new InventoryException("Inventory master not found");
            }

            if (master.getAvailableContainers() <= 0) {
                throw new InventoryException("No containers available");
            }

            master.setTotalContainers(master.getTotalContainers() - 1);
            master.setAvailableContainers(master.getAvailableContainers() - 1);
            restaurantInventoryMasterRepository.save(master);

            return new ApiResponse<>(InventoryConstant.SUCCESS,
                    "Damaged container reported successfully",
                    savedDamagedContainer);

        }

        if (request.getIsDamagedByUser()){
            ApiResponse<UserResponse> userresponse = authFeignClient.getUserByCustomerId(request.getUserId());
            if (
                    userresponse.getStatus().equals("error")
            ) {
                throw new InventoryException("User not found");
            }
            DamagedContainer damagedContainer = DamagedContainer.builder()
                    .containerTypeId(containerType.getId())
                    .remark(request.getRemark())
                    .userId(userresponse.getData().getId())
                    .restaurantId(request.getRestaurantId())
                    .damagedByRestaurant(false)
                    .damagedByUser(true)
                    .damagedCount(request.getDamagedCount())
                    .build();

            DamagedContainer savedDamagedContainer =
                    damagedContainerRepository.save(damagedContainer);

            if (!CollectionUtils.isEmpty(damagedContainerImages)) {
                List<String> imageUrls = damagedContainerImages.stream()
                        .filter(f -> f != null && !f.isEmpty())
                        .map(file -> notificationFeignClientService.uploadImage("damaged-container", file))
                        .toList();

                if (imageUrls.isEmpty()) {
                    throw new InventoryException("Image upload failed");
                }

                List<DamagedContainerImages> images = imageUrls.stream()
                        .map(url -> DamagedContainerImages.builder()
                                .damageId(savedDamagedContainer.getId())
                                .damageImageUrl(url)
                                .build())
                        .toList();

                damagedContainerImagesRepository.saveAll(images);
            }
// TODO  we have to implement at the time of Admin Module Implementation
//            AdminInventoryMaster master =
//                    masterRepo.findByContainerTypeId(containerType.getId())
//                            .orElseThrow(() -> new InventoryException("Inventory master not found by User"));
//
//            if (master.getAvailableContainers() <= 0) {
//                throw new InventoryException("No containers available");
//            }
//
//            master.setTotalContainers(master.getTotalContainers() - 1);
//            masterRepo.save(master);

            return new ApiResponse<>(InventoryConstant.SUCCESS,
                    "Damaged container reported successfully",
                    savedDamagedContainer);
        }

        throw new InventoryException("Invalid damage report: must be either by restaurant or user");

    }

    @Override
    public ApiResponse<List<DamageContainerMonthWiseResponse>>
    getDamageContainerMonthWiseDetails(Long restaurantId) {

        try {

            // ================= FETCH DAMAGED CONTAINERS =================
            List<DamagedContainer> damagedContainers =
                    damagedContainerRepository.findByRestaurantId(restaurantId);

            if (damagedContainers == null || damagedContainers.isEmpty()) {
                return new ApiResponse<>(
                        InventoryConstant.SUCCESS,
                        "No damaged containers found",
                        Collections.emptyList()
                );
            }

            // ================= BULK FETCH CONTAINER TYPES =================
            Set<Integer> containerTypeIds = damagedContainers.stream()
                    .map(DamagedContainer::getContainerTypeId)
                    .collect(Collectors.toSet());

            Map<Integer, ContainerType> containerTypeMap =
                    containerTypeRepository.findByIdIn(containerTypeIds)
                            .stream()
                            .collect(Collectors.toMap(ContainerType::getId, ct -> ct));

            // ================= BULK FETCH DAMAGE IMAGES =================
            Set<Long> damageIds = damagedContainers.stream()
                    .map(DamagedContainer::getId)
                    .collect(Collectors.toSet());

            Map<Long, List<DamagedContainerImages>> damageImagesMap =
                    damagedContainerImagesRepository.findByDamageIdIn(damageIds)
                            .stream()
                            .collect(Collectors.groupingBy(DamagedContainerImages::getDamageId));

            // ================= GROUP BY MONTH-YEAR =================
            Map<String, List<DamagedContainer>> monthWiseMap =
                    damagedContainers.stream()
                            .collect(Collectors.groupingBy(dc -> {
                                LocalDateTime date = dc.getCreatedAt();
                                return date.getMonth()
                                        .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                        + "-" + date.getYear();
                            }));

            List<DamageContainerMonthWiseResponse> monthWiseResponses = new ArrayList<>();

            for (Map.Entry<String, List<DamagedContainer>> monthEntry : monthWiseMap.entrySet()) {

                String monthYear = monthEntry.getKey();
                List<DamagedContainer> monthContainers = monthEntry.getValue();

                Integer monthWiseTotal = monthContainers.size();

                // ================= GROUP BY DATE-TIME =================
                Map<String, List<DamagedContainer>> dateWiseMap =
                        monthContainers.stream()
                                .collect(Collectors.groupingBy(dc ->
                                        dc.getCreatedAt()
                                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm"))
                                ));

                List<DamageContainerDateWiseResponse> dateResponses = new ArrayList<>();

                for (Map.Entry<String, List<DamagedContainer>> dateEntry : dateWiseMap.entrySet()) {

                    String dateTime = dateEntry.getKey();
                    List<DamagedContainer> dateContainers = dateEntry.getValue();

                    Integer dateWiseTotal = dateContainers.size();

                    List<DamageProductResponse> productResponses = new ArrayList<>();
                    Set<String> productIds = new LinkedHashSet<>();

                    for (DamagedContainer dc : dateContainers) {

                        ContainerType containerType =
                                containerTypeMap.get(dc.getContainerTypeId());

                        if (containerType == null) continue;

                        // Get images
                        List<DamagedContainerImages> images =
                                damageImagesMap.getOrDefault(dc.getId(), Collections.emptyList());

                        String imageUrls = images.stream()
                                .map(DamagedContainerImages::getDamageImageUrl)
                                .collect(Collectors.joining("#"));

                        DamageProductResponse product = new DamageProductResponse(
                                null,
                                null,
                                containerType.getId(),
                                containerType.getName(),
                                containerType.getDescription(),
                                containerType.getImageUrl(),
                                containerType.getCapacityMl(),
                                containerType.getProductId(),
                                dc.getRemark(),
                                imageUrls
                        );

                        productResponses.add(product);

                        if (containerType.getProductId() != null) {
                            productIds.add(containerType.getProductId());
                        }
                    }

                    DamageContainerDateWiseResponse damageResponse =
                            new DamageContainerDateWiseResponse(
                                    String.join(" | ", productIds),
                                    dateTime,
                                    dateWiseTotal,
                                    productResponses
                            );

                    dateResponses.add(damageResponse);
                }

                DamageContainerMonthWiseResponse monthResponse =
                        new DamageContainerMonthWiseResponse(
                                monthYear,
                                monthWiseTotal,
                                dateResponses
                        );

                monthWiseResponses.add(monthResponse);
            }

            return new ApiResponse<>(
                    InventoryConstant.SUCCESS,
                    "Damaged container details fetched successfully",
                    monthWiseResponses
            );

        } catch (Exception e) {

            log.error("Error fetching month-wise damaged container details for restaurantId {}: {}",
                    restaurantId, e.getMessage(), e);

            return new ApiResponse<>(
                    InventoryConstant.ERROR,
                    "Failed to fetch month-wise damaged container details",
                    null
            );
        }
    }

    @Override
    public ApiResponse<List<SoldContainerMonthWiseResponse>> getSoldContainerMonthWiseDetails(Long restaurantId) {

        try {

            // ================= FETCH SOLD CONTAINERS =================
            List<SoldContainers> soldContainers =
                    soldContainerRepository.findByRestaurantId(restaurantId);

            if (soldContainers == null || soldContainers.isEmpty()) {
                return new ApiResponse<>(
                        InventoryConstant.SUCCESS,
                        "No sold containers found",
                        Collections.emptyList()
                );
            }

            // ================= BULK FETCH CONTAINER TYPES =================
            Set<Integer> containerIds = soldContainers.stream()
                    .map(SoldContainers::getContainerId)
                    .collect(Collectors.toSet());

            Map<Integer, ContainerType> containerTypeMap =
                    containerTypeRepository.findByIdIn(containerIds)
                            .stream()
                            .collect(Collectors.toMap(ContainerType::getId, ct -> ct));

            // ================= GROUP BY MONTH-YEAR =================
            Map<String, List<SoldContainers>> monthWiseMap =
                    soldContainers.stream()
                            .collect(Collectors.groupingBy(sc -> {
                                LocalDateTime date = sc.getCreatedAt();
                                return date.getMonth()
                                        .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                        + "-" + date.getYear();
                            }));

            List<SoldContainerMonthWiseResponse> monthWiseResponses = new ArrayList<>();

            for (Map.Entry<String, List<SoldContainers>> monthEntry : monthWiseMap.entrySet()) {

                String monthYear = monthEntry.getKey();
                List<SoldContainers> monthContainers = monthEntry.getValue();

                Integer monthWiseTotal = monthContainers.stream()
                        .mapToInt(SoldContainers::getSoldQuantity)
                        .sum();

                // ================= GROUP BY DATE-TIME =================
                Map<String, List<SoldContainers>> dateWiseMap =
                        monthContainers.stream()
                                .collect(Collectors.groupingBy(sc ->
                                        sc.getCreatedAt()
                                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm"))
                                ));

                List<SoldContainersDateWiseResponse> dateResponses = new ArrayList<>();

                for (Map.Entry<String, List<SoldContainers>> dateEntry : dateWiseMap.entrySet()) {

                    String dateTime = dateEntry.getKey();
                    List<SoldContainers> dateContainers = dateEntry.getValue();

                    Integer dateWiseTotal = dateContainers.stream()
                            .mapToInt(SoldContainers::getSoldQuantity)
                            .sum();

                    List<SoldProductResponse> productResponses = new ArrayList<>();
                    Set<String> productIds = new LinkedHashSet<>();

                    for (SoldContainers sc : dateContainers) {

                        ContainerType containerType =
                                containerTypeMap.get(sc.getContainerId());

                        if (containerType == null) continue;

                        SoldProductResponse product = new SoldProductResponse(
                                containerType.getId(),
                                containerType.getName(),
                                containerType.getDescription(),
                                containerType.getImageUrl(),
                                containerType.getCapacityMl(),
                                containerType.getProductId(),
                                sc.getSoldPrice()
                        );

                        productResponses.add(product);

                        if (containerType.getProductId() != null) {
                            productIds.add(containerType.getProductId());
                        }
                    }

                    dateResponses.add(
                            new SoldContainersDateWiseResponse(
                                    String.join(" | ", productIds),
                                    dateTime,
                                    dateWiseTotal,
                                    productResponses
                            )
                    );
                }

                monthWiseResponses.add(
                        new SoldContainerMonthWiseResponse(
                                monthYear,
                                monthWiseTotal,
                                dateResponses
                        )
                );
            }

            return new ApiResponse<>(
                    InventoryConstant.SUCCESS,
                    "Sold container details fetched successfully",
                    monthWiseResponses
            );

        } catch (Exception e) {

            log.error("Error fetching month-wise sold container details for restaurantId {}: {}",
                    restaurantId, e.getMessage(), e);

            return new ApiResponse<>(
                    InventoryConstant.ERROR,
                    "Failed to fetch month-wise sold container details",
                    null
            );
        }
    }

    @Override
    public TrueInventoryStatsDto getContainerStats(Long restaurantId, Integer containerTypeId, Integer month, Integer year) {

        Integer trueTotal = restaurantInventoryMasterRepository.getTrueTotalContainers(restaurantId, containerTypeId);
        Integer trueAvailable = restaurantInventoryMasterRepository.getTrueAvailableContainers(restaurantId, containerTypeId);

        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.now().plusYears(10);

        if (month != null && year != null) {
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusSeconds(1);
        }

        Integer trueDamage = damagedContainerRepository.getMonthlyDamageCount(restaurantId, containerTypeId, startDate, endDate);

        return TrueInventoryStatsDto.builder()
                .total(trueTotal)
                .available(trueAvailable)
                .damageCount(trueDamage)
                .build();
    }

    @Override
    public ApiResponse<List<DamageContainerMonthWiseResponse>> getDamageContainerMonthWiseDetailsByAllCustomerOrPartner(String damageBy) {

        try {

            List<DamagedContainer> damagedContainers = new ArrayList<>();

            if (InventoryConstant.USER.equalsIgnoreCase(damageBy)) {
                damagedContainers = damagedContainerRepository.findAllIsDamageByCustomer();
            }

            if (InventoryConstant.RESTAURANT.equalsIgnoreCase(damageBy)) {
                damagedContainers = damagedContainerRepository.findAllIsDamageByRestaurant();
            }

            if (damagedContainers == null || damagedContainers.isEmpty()) {
                return new ApiResponse<>(
                        InventoryConstant.SUCCESS,
                        "No damaged containers found",
                        Collections.emptyList()
                );
            }

            // ================= BULK FETCH CONTAINER TYPES =================
            Set<Integer> containerTypeIds = damagedContainers.stream()
                    .map(DamagedContainer::getContainerTypeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Integer, ContainerType> containerTypeMap =
                    containerTypeRepository.findByIdIn(containerTypeIds)
                            .stream()
                            .collect(Collectors.toMap(
                                    ContainerType::getId,
                                    ct -> ct
                            ));

            // ================= BULK FETCH DAMAGE IMAGES =================
            Set<Long> damageIds = damagedContainers.stream()
                    .map(DamagedContainer::getId)
                    .collect(Collectors.toSet());

            Map<Long, List<DamagedContainerImages>> damageImagesMap =
                    damagedContainerImagesRepository.findByDamageIdIn(damageIds)
                            .stream()
                            .collect(Collectors.groupingBy(
                                    DamagedContainerImages::getDamageId
                            ));

            // ================= BULK FETCH USER DETAILS (Feign) =================
            Set<Long> userIds = new HashSet<>();

            if (InventoryConstant.USER.equalsIgnoreCase(damageBy)) {
                userIds = damagedContainers.stream()
                        .map(DamagedContainer::getUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
            if (InventoryConstant.RESTAURANT.equalsIgnoreCase(damageBy)) {
                userIds = damagedContainers.stream()
                        .map(DamagedContainer::getRestaurantId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            Map<Long, Map<String, Object>> userDetailsMap = new HashMap<>();

            for (Long userId : userIds) {
                try {
                    Map<String, Object> userDetails =
                            authFeignClient.getUserDetails(userId);

                    userDetailsMap.put(userId, userDetails);
                } catch (Exception ex) {
                    log.error("Failed to fetch user details for userId {}", userId);
                }
            }

            // ================= GROUP BY MONTH =================
            Map<String, List<DamagedContainer>> monthWiseMap =
                    damagedContainers.stream()
                            .collect(Collectors.groupingBy(dc -> {
                                LocalDateTime date = dc.getCreatedAt();
                                return date.getMonth()
                                        .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                        + "-" + date.getYear();
                            }));

            List<DamageContainerMonthWiseResponse> monthWiseResponses = new ArrayList<>();

            for (Map.Entry<String, List<DamagedContainer>> monthEntry : monthWiseMap.entrySet()) {

                String monthYear = monthEntry.getKey();
                List<DamagedContainer> monthContainers = monthEntry.getValue();

                Map<String, List<DamagedContainer>> dateWiseMap =
                        monthContainers.stream()
                                .collect(Collectors.groupingBy(dc ->
                                        dc.getCreatedAt()
                                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm"))
                                ));

                List<DamageContainerDateWiseResponse> dateResponses = new ArrayList<>();

                for (Map.Entry<String, List<DamagedContainer>> dateEntry : dateWiseMap.entrySet()) {

                    String dateTime = dateEntry.getKey();
                    List<DamagedContainer> dateContainers = dateEntry.getValue();

                    List<DamageProductResponse> productResponses = new ArrayList<>();
                    Set<String> productIds = new LinkedHashSet<>();

                    for (DamagedContainer dc : dateContainers) {

                        ContainerType containerType =
                                containerTypeMap.get(dc.getContainerTypeId());

                        if (containerType == null) continue;

                        // ===== Fetch user details from Map =====
                        Map<String, Object> userDetails = new HashMap<>();

                        if (InventoryConstant.USER.equalsIgnoreCase(damageBy)) {
                            userDetails = userDetailsMap.get(dc.getUserId());
                        }
                        if (InventoryConstant.RESTAURANT.equalsIgnoreCase(damageBy)) {
                            userDetails = userDetailsMap.get(dc.getRestaurantId());
                        }

                        String customerId = null;
                        String restaurantName = null;

                        if (userDetails != null && userDetails.get("data") != null) {

                            Map<String, Object> data =
                                    (Map<String, Object>) userDetails.get("data");

                            if (InventoryConstant.USER.equalsIgnoreCase(damageBy)) {
                                customerId = data.get("customerId") != null
                                        ? data.get("customerId").toString()
                                        : null;
                            }

                            if (InventoryConstant.RESTAURANT.equalsIgnoreCase(damageBy)) {
                                restaurantName = data.get("fullName") != null
                                        ? data.get("fullName").toString()
                                        : null;
                            }
                        }

                        // ===== Fetch Images =====
                        List<DamagedContainerImages> images =
                                damageImagesMap.getOrDefault(
                                        dc.getId(),
                                        Collections.emptyList()
                                );

                        String imageUrls = images.stream()
                                .map(DamagedContainerImages::getDamageImageUrl)
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining("#"));

                        DamageProductResponse product =
                                new DamageProductResponse(
                                        customerId,
                                        restaurantName,
                                        containerType.getId(),
                                        containerType.getName(),
                                        containerType.getDescription(),
                                        containerType.getImageUrl(),
                                        containerType.getCapacityMl(),
                                        containerType.getProductId(),
                                        dc.getRemark(),
                                        imageUrls
                                );

                        productResponses.add(product);

                        if (containerType.getProductId() != null) {
                            productIds.add(containerType.getProductId());
                        }
                    }

                    dateResponses.add(
                            new DamageContainerDateWiseResponse(
                                    String.join(" | ", productIds),
                                    dateTime,
                                    dateContainers.size(),
                                    productResponses
                            )
                    );
                }

                monthWiseResponses.add(
                        new DamageContainerMonthWiseResponse(
                                monthYear,
                                monthContainers.size(),
                                dateResponses
                        )
                );
            }

            return new ApiResponse<>(
                    InventoryConstant.SUCCESS,
                    "Damaged container details fetched successfully",
                    monthWiseResponses
            );

        } catch (Exception e) {

            log.error("Error fetching month-wise damaged container details", e);

            return new ApiResponse<>(
                    InventoryConstant.ERROR,
                    "Failed to fetch month-wise damaged container details",
                    null
            );
        }
    }
}
