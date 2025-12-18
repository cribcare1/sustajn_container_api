package com.inventory.service;

import com.inventory.dto.ContainerTypeResponse;
import com.inventory.dto.InventoryWithContainerResponse;
import com.inventory.request.*;
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
    }
