package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/predefined-items")
@Tag(name = "Predefined Items", description = "Predefined items catalog management")
public class PredefinedItemsController {
    
    private static final Logger logger = LoggerFactory.getLogger(PredefinedItemsController.class);
    
    @Autowired
    private PredefinedItemsService predefinedItemsService;
    
    @Autowired
    private SupplyManagementService supplyManagementService;
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Initialization method to log controller registration.
     * This helps verify the controller is properly scanned and endpoints are registered.
     */
    @PostConstruct
    public void init() {
        logger.info("=".repeat(80));
        logger.info("✅ PredefinedItemsController initialized and registered");
        logger.info("📍 Base URL: /predefined-items (full URL with context: /api/predefined-items)");
        logger.info("📍 Endpoints registered:");
        logger.info("   - GET    /predefined-items                 (listItems)");
        logger.info("   - POST   /predefined-items                 (createItem)");
        logger.info("   - PUT    /predefined-items/{id}            (updateItem)");
        logger.info("   - DELETE /predefined-items/{id}            (deleteItem)");
        logger.info("   - POST   /predefined-items/bulk-create     (bulkCreateItems) ← CRITICAL");
        logger.info("   - POST   /predefined-items/import-csv      (importCSV)");
        logger.info("   - GET    /predefined-items/export-csv      (exportCSV)");
        logger.info("   - GET    /predefined-items/{id}/stores     (getAssociatedStores)");
        logger.info("   - POST   /predefined-items/{id}/stores     (associateStores)");
        logger.info("   - GET    /predefined-items/{id}/warehouses (getAssociatedWarehouses)");
        logger.info("   - POST   /predefined-items/{id}/warehouses (associateWarehouses)");
        logger.info("=".repeat(80));
    }
    
    /**
     * List predefined items for a company (paginated, filterable)
     */
    @GetMapping
    @Operation(summary = "List predefined items", description = "Get all predefined items for a company")
    public ResponseEntity<GenericApiResponse<Map<String, Object>>> listItems(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @Parameter(description = "Category filter") @RequestParam(required = false) String category,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<PredefinedItem> itemsPage;
            
            if (search != null && !search.trim().isEmpty()) {
                itemsPage = predefinedItemsService.searchItems(company, search, pageable);
            } else if (category != null && !category.trim().isEmpty()) {
                itemsPage = predefinedItemsService.getItemsByCategory(company, category, pageable);
            } else {
                itemsPage = predefinedItemsService.getCompanyItems(company, pageable);
            }
            
            List<PredefinedItemResponse> items = itemsPage.getContent().stream()
                .map(PredefinedItemResponse::new)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", items);
            response.put("totalElements", itemsPage.getTotalElements());
            response.put("totalPages", itemsPage.getTotalPages());
            response.put("currentPage", itemsPage.getNumber());
            response.put("pageSize", itemsPage.getSize());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Items retrieved successfully", response));
            
        } catch (Exception e) {
            logger.error("Error listing predefined items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Create a new predefined item
     */
    @PostMapping
    @Operation(summary = "Create predefined item", description = "Create a new predefined item")
    public ResponseEntity<GenericApiResponse<PredefinedItemResponse>> createItem(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @Valid @RequestBody PredefinedItemRequest request,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            PredefinedItem item = predefinedItemsService.createItem(
                request.getName(),
                request.getSku(),
                request.getCategory(),
                request.getUnitType(),
                request.getDescription(),
                request.getDefaultPrice(),
                company,
                user,
                request.getStoreIds(),
                request.getWarehouseIds()
            );
            
            PredefinedItemResponse response = new PredefinedItemResponse(item);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericApiResponse<>(true, "Item created successfully", response));
            
        } catch (Exception e) {
            logger.error("Error creating predefined item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Update a predefined item
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update predefined item", description = "Update an existing predefined item")
    public ResponseEntity<GenericApiResponse<PredefinedItemResponse>> updateItem(
            @Parameter(description = "Item ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @Valid @RequestBody PredefinedItemRequest request,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            PredefinedItem item = predefinedItemsService.updateItem(
                id,
                request.getName(),
                request.getSku(),
                request.getCategory(),
                request.getUnitType(),
                request.getDescription(),
                request.getDefaultPrice(),
                company
            );
            
            PredefinedItemResponse response = new PredefinedItemResponse(item);
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Item updated successfully", response));
            
        } catch (Exception e) {
            logger.error("Error updating predefined item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Soft delete a predefined item
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete predefined item", description = "Soft delete a predefined item")
    public ResponseEntity<GenericApiResponse<Void>> deleteItem(
            @Parameter(description = "Item ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            predefinedItemsService.deleteItem(id, company, user);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Item deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Error deleting predefined item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage()));
        }
    }
    
    /**
     * Bulk create predefined items
     */
    @PostMapping("/bulk-create")
    @Operation(summary = "Bulk create items", description = "Create multiple predefined items at once")
    public ResponseEntity<GenericApiResponse<Map<String, Object>>> bulkCreateItems(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @Parameter(description = "Store IDs to associate with all items") @RequestParam(required = false) List<UUID> storeIds,
            @Parameter(description = "Warehouse IDs to associate with all items") @RequestParam(required = false) List<UUID> warehouseIds,
            @RequestBody List<Map<String, String>> itemsData,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            Map<String, Object> result = predefinedItemsService.bulkCreateItems(itemsData, company, user, storeIds, warehouseIds);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Bulk create completed", result));
            
        } catch (Exception e) {
            logger.error("Error bulk creating items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Import items from CSV
     */
    @PostMapping("/import-csv")
    @Operation(summary = "Import from CSV", description = "Import predefined items from CSV file")
    public ResponseEntity<GenericApiResponse<Map<String, Object>>> importCSV(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @Parameter(description = "Store IDs to associate with all imported items") @RequestParam(required = false) List<UUID> storeIds,
            @Parameter(description = "Warehouse IDs to associate with all imported items") @RequestParam(required = false) List<UUID> warehouseIds,
            @Parameter(description = "CSV file") @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, "CSV file is required", null));
            }
            
            Map<String, Object> result = predefinedItemsService.importFromCSV(file, company, user, storeIds, warehouseIds);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "CSV import completed", result));
            
        } catch (IOException e) {
            logger.error("Error reading CSV file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, "Error reading CSV file: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error importing CSV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Export items to CSV
     */
    @GetMapping("/export-csv")
    @Operation(summary = "Export to CSV", description = "Export predefined items to CSV file")
    public ResponseEntity<byte[]> exportCSV(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            String csvContent = predefinedItemsService.exportToCSV(company);
            
            String filename = "predefined_items_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.getBytes());
            
        } catch (Exception e) {
            logger.error("Error exporting CSV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get associated stores for an item
     */
    @GetMapping("/{id}/stores")
    @Operation(summary = "Get associated stores", description = "Get stores associated with a predefined item")
    public ResponseEntity<GenericApiResponse<List<StoreResponse>>> getAssociatedStores(
            @Parameter(description = "Item ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            PredefinedItem item = predefinedItemsService.getItemById(id, company);
            List<Store> stores = predefinedItemsService.getAssociatedStores(item);
            
            List<StoreResponse> response = stores.stream()
                .map(StoreResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Stores retrieved successfully", response));
            
        } catch (Exception e) {
            logger.error("Error getting associated stores: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Associate stores with an item
     */
    @PostMapping("/{id}/stores")
    @Operation(summary = "Associate stores", description = "Associate stores with a predefined item")
    public ResponseEntity<GenericApiResponse<Void>> associateStores(
            @Parameter(description = "Item ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @Valid @RequestBody AssociateLocationsRequest request,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            PredefinedItem item = predefinedItemsService.getItemById(id, company);
            predefinedItemsService.associateStores(item, request.getLocationIds(), user);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Stores associated successfully"));
            
        } catch (Exception e) {
            logger.error("Error associating stores: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage()));
        }
    }
    
    /**
     * Get associated warehouses for an item
     */
    @GetMapping("/{id}/warehouses")
    @Operation(summary = "Get associated warehouses", description = "Get warehouses associated with a predefined item")
    public ResponseEntity<GenericApiResponse<List<WarehouseResponse>>> getAssociatedWarehouses(
            @Parameter(description = "Item ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            PredefinedItem item = predefinedItemsService.getItemById(id, company);
            List<Warehouse> warehouses = predefinedItemsService.getAssociatedWarehouses(item);
            
            List<WarehouseResponse> response = warehouses.stream()
                .map(WarehouseResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Warehouses retrieved successfully", response));
            
        } catch (Exception e) {
            logger.error("Error getting associated warehouses: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Associate warehouses with an item
     */
    @PostMapping("/{id}/warehouses")
    @Operation(summary = "Associate warehouses", description = "Associate warehouses with a predefined item")
    public ResponseEntity<GenericApiResponse<Void>> associateWarehouses(
            @Parameter(description = "Item ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @Valid @RequestBody AssociateLocationsRequest request,
            Authentication authentication) {
        
        try {
            User user = supplyManagementService.getUserAndVerifyCompanyAccess(authentication, companyId);
            Company company = companyService.getCompany(companyId, authentication);
            
            // Verify permission
            supplyManagementService.verifyCanManagePredefinedItems(user, company);
            
            PredefinedItem item = predefinedItemsService.getItemById(id, company);
            predefinedItemsService.associateWarehouses(item, request.getLocationIds(), user);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Warehouses associated successfully"));
            
        } catch (Exception e) {
            logger.error("Error associating warehouses: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage()));
        }
    }
}
