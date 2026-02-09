package com.inventory.service.impl;

import com.inventory.Constant.AdminOrderStatus;
import com.inventory.Constant.InventoryConstant;
import com.inventory.Constant.TransactionType;
import com.inventory.dto.RestaurantContainerDetails;
import com.inventory.entity.AdminOrder;
import com.inventory.entity.AdminOrderItem;
import com.inventory.entity.RestaurantContainerInventory;
import com.inventory.entity.RestaurantInventoryMaster;
import com.inventory.exception.BusinessException;
import com.inventory.repository.AdminOrderItemRepository;
import com.inventory.repository.AdminOrderRepository;
import com.inventory.repository.RestaurantContainerInventoryRepository;
import com.inventory.repository.RestaurantInventoryMasterRepository;
import com.inventory.request.AdminOrderApproveRequest;
import com.inventory.request.AdminOrderCreateRequest;
import com.inventory.response.*;
import com.inventory.service.AdminRestaurantOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRestaurantOrderServiceImpl implements AdminRestaurantOrderService {

    private final AdminOrderRepository adminOrderRepository;
    private final AdminOrderItemRepository adminOrderItemRepository;
    private final RestaurantContainerInventoryRepository inventoryRepository;
    private final RestaurantInventoryMasterRepository restaurantInventoryMasterRepository;

    @Override
    @Transactional
    public Map<String, Object> raiseOrderRequest(AdminOrderCreateRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {

            // ----- Basic Validations -----
            if (request.getRestaurantId() == null)
                throw new BusinessException("Restaurant ID is required");

            if (request.getItems() == null || request.getItems().isEmpty())
                throw new BusinessException("At least one order item is required");

            TransactionType transactionType;
            try {
                transactionType = TransactionType.valueOf(request.getType());
            } catch (Exception ex) {
                throw new BusinessException("Invalid transaction type. Allowed: BORROW / RETURN");
            }

            // ----- Create Order -----
            AdminOrder order = new AdminOrder();
            order.setRestaurantId(request.getRestaurantId());
            order.setOrderDate(LocalDateTime.now());
            order.setRestaurantRemark(request.getRestaurantRemark());
            order.setType(transactionType);
            order.setStatus(AdminOrderStatus.PENDING);

            AdminOrder savedOrder = adminOrderRepository.save(order);

            // ----- Save Items -----
            List<AdminOrderItem> items = request.getItems()
                    .stream()
                    .map(i -> {
                        AdminOrderItem item = new AdminOrderItem();
                        item.setContainerTypeId(i.getContainerTypeId());
                        item.setRequestedQty(i.getRequestedQty());
                        item.setApprovedQty(0);
                        item.setOrder(savedOrder);
                        return item;
                    })
                    .collect(Collectors.toList());

            adminOrderItemRepository.saveAll(items);


            response.put("success", true);
            response.put("message", "Order request submitted successfully");

        } catch (BusinessException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());

        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Something went wrong while creating order");
            response.put("error", ex.getMessage());
        }

        return response;
    }

    @Transactional
    @Override
    public Map<String, Object> approveOrder(AdminOrderApproveRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {

            if (request.getOrderId() == null)
                throw new BusinessException("Order ID is required");

            if (request.getItems() == null || request.getItems().isEmpty())
                throw new BusinessException("Approval items cannot be empty");

            // ----- Load Order -----
            AdminOrder order = adminOrderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new BusinessException("Order not found"));

            if (order.getStatus() != AdminOrderStatus.PENDING)
                throw new BusinessException("Only PENDING orders can be approved");

            // ----- Load Order Items -----
            List<AdminOrderItem> orderItems =
                    adminOrderItemRepository.findAllByOrder(order);

            Map<Long, AdminOrderItem> itemMap = orderItems.stream()
                    .collect(Collectors.toMap(AdminOrderItem::getId, it -> it));

            // ----- Apply Approvals -----
            for (AdminOrderApproveRequest.ItemApproval approval : request.getItems()) {

                AdminOrderItem item = itemMap.get(approval.getItemId());
                if (item == null)
                    throw new BusinessException("Invalid item ID: " + approval.getItemId());

                if (approval.getApprovedQty() == null || approval.getApprovedQty() < 0)
                    throw new BusinessException("Approved quantity must be >= 0");

                if (approval.getApprovedQty() > item.getRequestedQty())
                    throw new BusinessException(
                            "Approved qty cannot be greater than requested qty for item " + item.getId());

                item.setApprovedQty(approval.getApprovedQty());
            }

            adminOrderItemRepository.saveAll(orderItems);

            // ----- Update Order Status -----
            order.setStatus(AdminOrderStatus.APPROVED);
            order.setAdminRemark(request.getAdminRemark());
            order.setDecisionAt(LocalDateTime.now());

            adminOrderRepository.save(order);

            response.put("status", "success");
            response.put("message", "Order approved successfully");

        } catch (BusinessException ex) {
            response.put("status", "error");
            response.put("message", ex.getMessage());

        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Something went wrong while approving order"+ex.getMessage());
        }

        return response;
    }

//    @Override
//    @Transactional
//    public Map<String, Object> markOrderAsDelivered(Long orderId) {
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            // 1️⃣ Fetch the order
//            Optional<AdminOrder> optionalOrder = adminOrderRepository.findById(orderId);
//            if (optionalOrder.isEmpty()) {
//                response.put("status", "error");
//                response.put("message", "Order not found");
//                response.put("value", null);
//                return response;
//            }
//
//            AdminOrder order = optionalOrder.get();
//
//            // Only APPROVED orders can be delivered
//            if (order.getStatus() != AdminOrderStatus.APPROVED) {
//                response.put("status", "error");
//                response.put("message", "Only approved orders can be delivered");
//                response.put("value", null);
//                return response;
//            }
//
//            // 2️⃣ Load order items
//            List<AdminOrderItem> orderItems = adminOrderItemRepository.findAllByOrder(order);
//            if (orderItems.isEmpty()) {
//                response.put("status", "error");
//                response.put("message", "No items found for this order");
//                response.put("value", null);
//                return response;
//            }
//
//            Long restaurantId = order.getRestaurantId();
//            Set<Integer> containerTypeIds = orderItems.stream()
//                    .map(AdminOrderItem::getContainerTypeId)
//                    .collect(Collectors.toSet());
//
//            // 3️⃣ Pre-fetch existing inventory for all container types at once
//            List<RestaurantContainerInventory> inventories =
//                    inventoryRepository.findAllByRestaurantIdAndContainerTypeIdIn(restaurantId, containerTypeIds);
//
//            List<RestaurantInventoryMaster> inventoryMasters = restaurantInventoryMasterRepository.findAllByRestaurantIdAndContainerTypeIdIn(restaurantId, containerTypeIds);
//
//            // Create a map for quick lookup
//            Map<Integer, RestaurantContainerInventory> inventoryMap = inventories.stream()
//                    .collect(Collectors.toMap(RestaurantContainerInventory::getContainerTypeId, inv -> inv));
//
//            // Create a map for inventory master lookup
//            Map<Integer, RestaurantInventoryMaster> inventoryMasterMap = inventoryMasters.stream()
//                    .collect(Collectors.toMap(RestaurantInventoryMaster::getContainerTypeId, im -> im));
//
//            // 4️⃣ Update inventory in memory
//            List<RestaurantContainerInventory> inventoriesToSave = new ArrayList<>();
//            LocalDateTime now = LocalDateTime.now();
//
//            for (AdminOrderItem item : orderItems) {
//                Integer containerTypeId = item.getContainerTypeId();
//                Integer approvedQty = item.getApprovedQty();
//
//                if (approvedQty == null || approvedQty <= 0) continue;
//
//                RestaurantContainerInventory inventory = inventoryMap.get(containerTypeId);
//                if (inventory == null) {
//                    // If inventory does not exist, create new
//                    inventory = new RestaurantContainerInventory();
//                    inventory.setRestaurantId(restaurantId);
//                    inventory.setContainerTypeId(containerTypeId);
//                    inventory.setCurrentQuantity(0);
//                    inventoryMap.put(containerTypeId, inventory);
//                }
//
//                inventory.setCurrentQuantity(inventory.getCurrentQuantity() + approvedQty);
//                inventory.setLastUpdated(now);
//
//                inventoriesToSave.add(inventory);
//            }
//
//            // 5️⃣ Save all inventory updates in batch
//            inventoryRepository.saveAll(inventoriesToSave);
//
//            // 6️⃣ Update order status to DELIVERED
//            order.setStatus(AdminOrderStatus.DELIVERED);
//            adminOrderRepository.save(order);
//
//            response.put("status", "success");
//            response.put("message", "Order delivered and inventory updated successfully");
//            return response;
//
//        } catch (Exception e) {
//            response.put("status", "error");
//            response.put("message", "Failed to mark order as delivered"+e.getMessage());
//            return response;
//        }
//    }


    @Override
    @Transactional
    public Map<String, Object> markOrderAsDelivered(Long orderId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Fetch order
            AdminOrder order = adminOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (order.getStatus() != AdminOrderStatus.APPROVED) {
                throw new RuntimeException("Only approved orders can be delivered");
            }

            // 2. Load order items
            List<AdminOrderItem> orderItems =
                    adminOrderItemRepository.findAllByOrder(order);

            if (orderItems.isEmpty()) {
                throw new RuntimeException("No items found for this order");
            }

            Long restaurantId = order.getRestaurantId();
            Set<Integer> containerTypeIds = orderItems.stream()
                    .map(AdminOrderItem::getContainerTypeId)
                    .collect(Collectors.toSet());

            // 3. Fetch inventories
            List<RestaurantContainerInventory> inventories =
                    inventoryRepository.findAllByRestaurantIdAndContainerTypeIdIn(
                            restaurantId, containerTypeIds);

            List<RestaurantInventoryMaster> masters =
                    restaurantInventoryMasterRepository
                            .findAllByRestaurantIdAndContainerTypeIdIn(
                                    restaurantId, containerTypeIds);

            Map<Integer, RestaurantContainerInventory> inventoryMap =
                    inventories.stream().collect(Collectors.toMap(
                            RestaurantContainerInventory::getContainerTypeId,
                            inv -> inv));

            Map<Integer, RestaurantInventoryMaster> masterMap =
                    masters.stream().collect(Collectors.toMap(
                            RestaurantInventoryMaster::getContainerTypeId,
                            im -> im));

            List<RestaurantContainerInventory> inventoriesToSave = new ArrayList<>();
            List<RestaurantInventoryMaster> mastersToSave = new ArrayList<>();

            LocalDateTime now = LocalDateTime.now();

            // 4. Update both inventories
            for (AdminOrderItem item : orderItems) {

                Integer containerTypeId = item.getContainerTypeId();
                Integer approvedQty = item.getApprovedQty();

                if (approvedQty == null || approvedQty <= 0) continue;

                // ---------- CHILD INVENTORY ----------
                RestaurantContainerInventory inventory =
                        inventoryMap.get(containerTypeId);

                if (inventory == null) {
                    inventory = new RestaurantContainerInventory();
                    inventory.setRestaurantId(restaurantId);
                    inventory.setContainerTypeId(containerTypeId);
                    inventory.setCurrentQuantity(0); // CRITICAL FIX
                }

                inventory.setCurrentQuantity(
                        inventory.getCurrentQuantity() + approvedQty);
                inventory.setLastUpdated(now);

                inventoriesToSave.add(inventory);

                // ---------- MASTER INVENTORY ----------
                RestaurantInventoryMaster master =
                        masterMap.get(containerTypeId);

                if (master == null) {
                    master = RestaurantInventoryMaster.builder()
                            .restaurantId(restaurantId)
                            .containerTypeId(containerTypeId)
                            .totalContainers(0)
                            .availableContainers(0)
                            .borrowedContainers(0)
                            .build();
                }

                master.setTotalContainers(
                        master.getTotalContainers() + approvedQty);

                master.setAvailableContainers(
                        master.getAvailableContainers() + approvedQty);

                mastersToSave.add(master);
            }

            // 5. Save both in batch
            inventoryRepository.saveAll(inventoriesToSave);
            restaurantInventoryMasterRepository.saveAll(mastersToSave);

            // 6. Update order
            order.setStatus(AdminOrderStatus.DELIVERED);
            adminOrderRepository.save(order);

            response.put(InventoryConstant.STATUS, InventoryConstant.SUCCESS);
            response.put(InventoryConstant.MESSAGE,
                    "Order delivered & both inventories updated successfully");
            return response;

        } catch (Exception e) {
            response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
            response.put(InventoryConstant.MESSAGE, e.getMessage());
            return response;
        }
    }


    @Override
    public Map<String, Object> getAvailableContainers(Long restaurantId) {
        Map<String, Object> response = new HashMap<>();

        try {
            //  Fetch containers
            List<RestaurantContainerDetails> containers = inventoryRepository.findContainersWithDetails(restaurantId);

            // 3️⃣ Check if data is empty
            if (containers == null || containers.isEmpty()) {
                response.put("status", "success");
                response.put("message", "No containers available for this restaurant");
                response.put("containersDetails", Collections.emptyList());
                return response;
            }

            // 4️⃣ Success response
            response.put("status", "success");
            response.put("message", "Available containers fetched successfully");
            response.put("containersDetails", containers);
            return response;

        } catch (Exception e) {
            // 5️⃣ Exception handling
            response.put("status", "error");
            response.put("message", "Failed to fetch available containers"+e.getMessage());
            response.put("containersDetails", null);
            return response;
        }
    }

    @Override
    public ApiResponse<List<RestaurantOrderedResponse>> getRestaurantOrderDetails(Long restaurantId) {
        try {
            List<RestaurantOrderedResponse> response =
                    adminOrderRepository.findOrdersByRestaurantId(restaurantId);

            return new ApiResponse<>("Order details fetched successfully",
                    InventoryConstant.SUCCESS, response);

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Failed to fetch order details",
                    InventoryConstant.ERROR, null);
        }
    }

    @Override
    public ApiResponse<List<IssuedProductsResponse>> getAllIssuedProductsToRestaurant(Long restaurantId) {
        try {
            List<Object[]> response =  adminOrderItemRepository.findIssuedProducts(restaurantId);
            if (CollectionUtils.isEmpty(response)) {
                return new ApiResponse<>(InventoryConstant.SUCCESS, "No issued products found",
                         null);
            }
            List<IssuedProductsResponse> issuedProducts = response.stream()
                    .map(record -> {
                        IssuedProductsResponse issuedProductsResponse = new IssuedProductsResponse();
                        issuedProductsResponse.setId((Integer)  record[0]);
                        issuedProductsResponse.setProductName((String) record[1]);
                        issuedProductsResponse.setProductId((String) record[2]);
                        issuedProductsResponse.setCapacityMl((Integer) record[3]);
                        issuedProductsResponse.setTotalIssuedQuantity(((Long) record[4]).intValue());
                        return issuedProductsResponse;
                    })
                    .collect(Collectors.toList());
            return new ApiResponse<>(InventoryConstant.SUCCESS, "Issued products fetched successfully", issuedProducts);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            return new ApiResponse<>(InventoryConstant.ERROR, "Failed to fetch issued products", null);
        }
    }

    @Override
    public ApiResponse<List<MonthWiseIssuedResponse>> getMonthWiseIssuedProductsToRestaurant(Long restaurantId, Integer productId) {
        try {
            List<Object[]> response = adminOrderItemRepository.findDateWiseIssuedQty(restaurantId, productId);

            if (response.isEmpty()) {
                return new ApiResponse<>(InventoryConstant.SUCCESS, "No issued products", List.of());
            }

            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM-yyyy");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            Map<String, List<Object[]>> monthMap = response.stream()
                    .collect(Collectors.groupingBy(
                            r -> ((LocalDate) r[0]).format(monthFormatter),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<MonthWiseIssuedResponse> monthWiseResponse = new ArrayList<>();

            for (Map.Entry<String, List<Object[]>> entry : monthMap.entrySet()) {

                String monthYear = entry.getKey();
                List<Object[]> monthRows = entry.getValue();

                int monthTotal = monthRows.stream()
                        .mapToInt(r -> ((Long) r[1]).intValue())
                        .sum();

                Map<String, Integer> dateMap = new LinkedHashMap<>();

                for (Object[] r : monthRows) {
                    LocalDate date = (LocalDate) r[0];
                    int qty = ((Long) r[1]).intValue();
                    dateMap.merge(date.format(dateFormatter), qty, Integer::sum);
                }

                List<DateWiseIssuedResponse> dateWiseList = dateMap.entrySet().stream()
                        .map(e -> new DateWiseIssuedResponse(e.getKey(), e.getValue()))
                        .toList();

                monthWiseResponse.add(
                        new MonthWiseIssuedResponse(monthYear, monthTotal, dateWiseList)
                );
            }


            return new ApiResponse<>(InventoryConstant.SUCCESS, "Month-wise issued products fetched", monthWiseResponse);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            return new ApiResponse<>(InventoryConstant.ERROR, "Failed to fetch month-wise issued products", null);
        }
    }

    @Override
    public ApiResponse<List<ReturnedProductsResponse>> getAllReturnedProductsToRestaurant(Long restaurantId) {
        try {
            // Fetch data
            List<Object[]> response = adminOrderItemRepository.findReturnedProducts(restaurantId);

            if (CollectionUtils.isEmpty(response)) {
                return new ApiResponse<>(InventoryConstant.SUCCESS, "No returned products found", null);
            }

            // Map to DTO
            List<ReturnedProductsResponse> returnedProducts = response.stream()
                    .map(record -> {
                        ReturnedProductsResponse res = new ReturnedProductsResponse();
                        res.setId((Integer) record[0]);
                        res.setProductName((String) record[1]);
                        res.setProductId((String) record[2]);
                        res.setCapacityMl((Integer) record[3]);
                        res.setTotalReturnedQuantity(((Long) record[4]).intValue());
                        return res;
                    })
                    .collect(Collectors.toList());

            return new ApiResponse<>(InventoryConstant.SUCCESS, "Returned products fetched successfully", returnedProducts);
        } catch (Exception e) {
            log.error("Error fetching returned products: {}", e.getMessage(), e);
            return new ApiResponse<>(InventoryConstant.ERROR, "Failed to fetch returned products", null);
        }
    }

    @Override
    public ApiResponse<List<MonthWiseReturnedResponse>> getMonthWiseReturnedProductsToRestaurant(Long restaurantId, Integer productId) {
        try {
            List<Object[]> response = adminOrderItemRepository.findDateWiseReturnedQty(restaurantId, productId);

            if (response.isEmpty()) {
                return new ApiResponse<>(InventoryConstant.SUCCESS, "No return history found", List.of());
            }

            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM-yyyy");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            // Group by Month
            Map<String, List<Object[]>> monthMap = response.stream()
                    .collect(Collectors.groupingBy(
                            r -> {
                                java.sql.Date sqlDate = (java.sql.Date) r[0];
                                return sqlDate.toLocalDate().format(monthFormatter);
                            },
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<MonthWiseReturnedResponse> monthWiseResponse = new ArrayList<>();

            for (Map.Entry<String, List<Object[]>> entry : monthMap.entrySet()) {
                String monthYear = entry.getKey();
                List<Object[]> monthRows = entry.getValue();

                int monthTotal = monthRows.stream()
                        .mapToInt(r -> ((Long) r[1]).intValue())
                        .sum();

                Map<String, Integer> dateMap = new LinkedHashMap<>();
                for (Object[] r : monthRows) {
                    java.sql.Date sqlDate = (java.sql.Date) r[0];
                    LocalDate date = sqlDate.toLocalDate();
                    int qty = ((Long) r[1]).intValue();
                    dateMap.merge(date.format(dateFormatter), qty, Integer::sum);
                }

                List<DateWiseReturnedResponse> dateWiseList = dateMap.entrySet().stream()
                        .map(e -> new DateWiseReturnedResponse(e.getKey(), e.getValue()))
                        .toList();

                monthWiseResponse.add(new MonthWiseReturnedResponse(monthYear, monthTotal, dateWiseList));
            }

            return new ApiResponse<>(InventoryConstant.SUCCESS, "Month-wise returned details fetched", monthWiseResponse);

        } catch (Exception e) {
            log.error("Error fetching month-wise returns: {}", e.getMessage(), e);
            return new ApiResponse<>(InventoryConstant.ERROR, "Failed to fetch return details", null);
        }
    }

}
