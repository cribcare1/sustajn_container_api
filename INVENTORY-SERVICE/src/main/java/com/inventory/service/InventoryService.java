package com.inventory.service;

import com.inventory.dto.*;
import com.inventory.entity.DamagedContainer;
import com.inventory.entity.RestaurantContainerInventory;
import com.inventory.entity.RestaurantInventoryMaster;
import com.inventory.request.*;
import com.inventory.response.ApiResponse;
import com.inventory.response.RestaurantContainerInventoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public interface InventoryService {
    public Map<String, Object> saveOrUpdate(ContainerTypeRequest request, MultipartFile file);
    public Map<String, Object> getActiveContainerTypes();
    public Map<String, Object> deleteContainerType(Integer id);

    public Map<String, Object> addMultipleInventories(InventoryBulkAddRequest request);
    public Map<String, Object> updateInventory(InventoryUpdateRequest request);
    public Map<String, Object> getAllActiveInventory();
    public Map<String, Object> addRestaurantInventoryBulk(AdminRestaurantInventoryBulkRequest request);

    public Map<String, Object> getRestaurantInventory(Long restaurantId);

    public Map<String,Object> getAdminDashboardData();

    public Map<String, Object> addContainer(AddContainerRequest request, MultipartFile image) ;

    ApiResponse<List<ProductResponse>> getProductsByIds(List<Integer> ids);

    ApiResponse<List<RestaurantContainerInventoryResponse>> getRestaurantContainerInventoryByRestaurantId(Long restaurantId);

    ApiResponse<List<RestaurantInventoryMaster>> reduceAvailableContainers(ReduceInventoryRequest request);
    public ApiResponse<List<RestaurantInventoryMaster>> increaseContainers(
            ReduceInventoryRequest request);

    Map<String, Object> checkAvailability(ReduceInventoryRequest request);

    Map<String, Object> increaseAvailableContainers( ReduceInventoryRequest request);

    ApiResponse<DamagedContainer> reportDamagedContainer(String reportDamagedContainerRequest, List<MultipartFile> damagedContainerImages);

    ApiResponse<List<DamageContainerMonthWiseResponse>> getDamageContainerMonthWiseDetails(Long restaurantId);

    ApiResponse<List<SoldContainerMonthWiseResponse>> getSoldContainerMonthWiseDetails(Long restaurantId);

    ApiResponse<List<DamageContainerMonthWiseResponse>> getDamageContainerMonthWiseDetailsByAllCustomerOrPartner(String damageBy);

    Map<String, Object> getAllContainerTypes();




}
