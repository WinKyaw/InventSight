package com.pos.inventsight.service;

import com.pos.inventsight.dto.WarehouseRequest;
import com.pos.inventsight.dto.WarehouseResponse;
import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing warehouses
 */
@Service
@Transactional
public class WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityLogService activityLogService;

    /**
     * Create a new warehouse
     */
    public WarehouseResponse createWarehouse(WarehouseRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        // Enhanced logging
        System.out.println("🏢 WarehouseService: Creating warehouse");
        System.out.println("   Name: " + request.getName());
        System.out.println("   Location: " + request.getLocation());

        // Check if warehouse with same name already exists
        if (warehouseRepository.existsByNameIgnoreCaseAndIsActiveTrue(request.getName())) {
            throw new DuplicateResourceException("Warehouse with name '" + request.getName() + "' already exists");
        }

        // Create warehouse entity
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.getName());
        warehouse.setDescription(request.getDescription());
        warehouse.setLocation(request.getLocation());
        warehouse.setAddress(request.getAddress());
        warehouse.setCity(request.getCity());
        warehouse.setState(request.getState());
        warehouse.setPostalCode(request.getPostalCode());
        warehouse.setCountry(request.getCountry());
        warehouse.setPhone(request.getPhone());
        warehouse.setEmail(request.getEmail());
        warehouse.setManagerName(request.getManagerName());
        warehouse.setWarehouseType(request.getWarehouseType() != null ? 
            request.getWarehouseType() : Warehouse.WarehouseType.GENERAL);
        warehouse.setCapacityCubicMeters(request.getCapacityCubicMeters());
        warehouse.setIsActive(request.getIsActive());
        warehouse.setCreatedBy(username);

        warehouse = warehouseRepository.save(warehouse);
        
        System.out.println("✅ Warehouse saved with ID: " + warehouse.getId());

        // Log activity
        activityLogService.logActivity(
            user.getId().toString(),
            username,
            "warehouse_created", 
            "warehouse",
            "Warehouse '" + warehouse.getName() + "' created"
        );

        return new WarehouseResponse(warehouse);
    }

    /**
     * Get all active warehouses
     */
    public List<WarehouseResponse> getAllActiveWarehouses() {
        return warehouseRepository.findByIsActiveTrue()
            .stream()
            .map(WarehouseResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * Get warehouse by ID
     */
    public WarehouseResponse getWarehouseById(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + id));
        
        if (!warehouse.getIsActive()) {
            throw new ResourceNotFoundException("Warehouse is inactive");
        }

        return new WarehouseResponse(warehouse);
    }

    /**
     * Get warehouse entity by ID (for internal use)
     */
    public Warehouse getWarehouseEntityById(UUID id) {
        return warehouseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + id));
    }

    /**
     * Update warehouse
     */
    public WarehouseResponse updateWarehouse(UUID id, WarehouseRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();
        Warehouse warehouse = getWarehouseEntityById(id);

        // Check if new name conflicts with existing warehouse
        if (!warehouse.getName().equalsIgnoreCase(request.getName()) &&
            warehouseRepository.existsByNameIgnoreCaseAndIsActiveTrue(request.getName())) {
            throw new DuplicateResourceException("Warehouse with name '" + request.getName() + "' already exists");
        }

        // Update fields
        warehouse.setName(request.getName());
        warehouse.setDescription(request.getDescription());
        warehouse.setLocation(request.getLocation());
        warehouse.setAddress(request.getAddress());
        warehouse.setCity(request.getCity());
        warehouse.setState(request.getState());
        warehouse.setPostalCode(request.getPostalCode());
        warehouse.setCountry(request.getCountry());
        warehouse.setPhone(request.getPhone());
        warehouse.setEmail(request.getEmail());
        warehouse.setManagerName(request.getManagerName());
        if (request.getWarehouseType() != null) {
            warehouse.setWarehouseType(request.getWarehouseType());
        }
        warehouse.setCapacityCubicMeters(request.getCapacityCubicMeters());
        if (request.getIsActive() != null) {
            warehouse.setIsActive(request.getIsActive());
        }
        warehouse.setUpdatedBy(username);

        warehouse = warehouseRepository.save(warehouse);

        // Log activity
        activityLogService.logActivity(
            authentication.getName(),
            username,
            "warehouse_updated", 
            "warehouse",
            "Warehouse '" + warehouse.getName() + "' updated"
        );

        return new WarehouseResponse(warehouse);
    }

    /**
     * Delete (deactivate) warehouse
     */
    public void deleteWarehouse(UUID id, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();
        Warehouse warehouse = getWarehouseEntityById(id);

        warehouse.setIsActive(false);
        warehouse.setUpdatedBy(username);
        warehouseRepository.save(warehouse);

        // Log activity
        activityLogService.logActivity(
            authentication.getName(),
            username,
            "warehouse_deleted", 
            "warehouse",
            "Warehouse '" + warehouse.getName() + "' deactivated"
        );
    }

    /**
     * Search warehouses by name or location
     */
    public List<WarehouseResponse> searchWarehouses(String searchTerm) {
        return warehouseRepository.searchByNameOrLocation(searchTerm)
            .stream()
            .map(WarehouseResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * Get warehouses by type
     */
    public List<WarehouseResponse> getWarehousesByType(Warehouse.WarehouseType type) {
        return warehouseRepository.findByWarehouseTypeAndIsActiveTrue(type)
            .stream()
            .map(WarehouseResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * Get warehouses by city
     */
    public List<WarehouseResponse> getWarehousesByCity(String city) {
        return warehouseRepository.findByCityIgnoreCaseAndIsActiveTrue(city)
            .stream()
            .map(WarehouseResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * Get warehouse statistics
     */
    public long getActiveWarehouseCount() {
        return warehouseRepository.countByIsActiveTrue();
    }

    /**
     * Validate warehouse exists and is active
     */
    public void validateWarehouseExists(UUID warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found with ID: " + warehouseId);
        }
    }
}