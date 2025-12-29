package com.pos.inventsight.service;

import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import com.pos.inventsight.util.SkuGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class PredefinedItemsService {
    
    private static final Logger logger = LoggerFactory.getLogger(PredefinedItemsService.class);
    
    @Autowired
    private PredefinedItemRepository predefinedItemRepository;
    
    @Autowired
    private PredefinedItemStoreRepository predefinedItemStoreRepository;
    
    @Autowired
    private PredefinedItemWarehouseRepository predefinedItemWarehouseRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private SupplyManagementService supplyManagementService;
    
    @Autowired
    private CSVService csvService;
    
    @Autowired
    private SkuGenerator skuGenerator;
    
    /**
     * Get all predefined items for a company (paginated)
     */
    public Page<PredefinedItem> getCompanyItems(Company company, Pageable pageable) {
        return predefinedItemRepository.findByCompanyAndIsActiveTrueOrderByNameAsc(company, pageable);
    }
    
    /**
     * Get predefined items filtered by category
     */
    public Page<PredefinedItem> getItemsByCategory(Company company, String category, Pageable pageable) {
        return predefinedItemRepository.findByCompanyAndCategoryAndIsActiveTrueOrderByNameAsc(
            company, category, pageable);
    }
    
    /**
     * Search predefined items by name
     */
    public Page<PredefinedItem> searchItems(Company company, String searchTerm, Pageable pageable) {
        return predefinedItemRepository.findByCompanyAndNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(
            company, searchTerm, pageable);
    }
    
    /**
     * Get a single predefined item by ID
     */
    public PredefinedItem getItemById(UUID itemId, Company company) {
        PredefinedItem item = predefinedItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Predefined item not found with ID: " + itemId));
        
        // Verify item belongs to company
        if (!item.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("Predefined item not found in your company");
        }
        
        return item;
    }
    
    /**
     * Create a new predefined item
     * NOTE: The sku parameter is ignored; SKU is auto-generated
     */
    public PredefinedItem createItem(
            String name,
            String sku,
            String category,
            String unitType,
            String description,
            BigDecimal defaultPrice,
            Company company,
            User createdBy) {
        
        // Check for duplicates
        if (predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType)) {
            throw new DuplicateResourceException(
                String.format("Item '%s' with unit type '%s' already exists", name, unitType));
        }
        
        // Auto-generate unique SKU (user-provided SKU is ignored)
        String generatedSku = skuGenerator.generateUniqueSku(predefinedItemRepository::existsBySku);
        logger.info("Auto-generated SKU: {} for item: {}", generatedSku, name);
        
        PredefinedItem item = new PredefinedItem(name, unitType, company, createdBy);
        item.setSku(generatedSku); // Use auto-generated SKU
        item.setCategory(category);
        item.setDescription(description);
        item.setDefaultPrice(defaultPrice);
        
        PredefinedItem saved = predefinedItemRepository.save(item);
        logger.info("Created predefined item '{}' with SKU {} in company {}", name, generatedSku, company.getId());
        
        return saved;
    }
    
    /**
     * Update a predefined item
     */
    public PredefinedItem updateItem(
            UUID itemId,
            String name,
            String sku,
            String category,
            String unitType,
            String description,
            BigDecimal defaultPrice,
            Company company) {
        
        PredefinedItem item = getItemById(itemId, company);
        
        // Check for duplicates if name or unitType changed
        if (!item.getName().equals(name) || !item.getUnitType().equals(unitType)) {
            if (predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType)) {
                throw new DuplicateResourceException(
                    String.format("Item '%s' with unit type '%s' already exists", name, unitType));
            }
        }
        
        item.setName(name);
        item.setSku(sku);
        item.setCategory(category);
        item.setUnitType(unitType);
        item.setDescription(description);
        item.setDefaultPrice(defaultPrice);
        
        PredefinedItem updated = predefinedItemRepository.save(item);
        logger.info("Updated predefined item {} in company {}", itemId, company.getId());
        
        return updated;
    }
    
    /**
     * Soft delete a predefined item
     */
    public void deleteItem(UUID itemId, Company company, User deletedBy) {
        PredefinedItem item = getItemById(itemId, company);
        
        item.softDelete(deletedBy);
        predefinedItemRepository.save(item);
        
        logger.info("Soft deleted predefined item {} in company {} by user {}", 
                   itemId, company.getId(), deletedBy.getUsername());
    }
    
    /**
     * Bulk create predefined items
     */
    public Map<String, Object> bulkCreateItems(
            List<Map<String, String>> itemsData,
            Company company,
            User createdBy) {
        
        int successful = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        List<PredefinedItem> createdItems = new ArrayList<>();
        
        for (int i = 0; i < itemsData.size(); i++) {
            Map<String, String> itemData = itemsData.get(i);
            
            try {
                // Normalize keys to lowercase for case-insensitive processing
                Map<String, String> normalizedData = new HashMap<>();
                itemData.forEach((key, value) -> normalizedData.put(key.toLowerCase(), value));
                
                logger.debug("Processing item with fields: {}", normalizedData.keySet());
                
                // Validate using normalized data
                List<String> itemErrors = new ArrayList<>();
                if (!csvService.validateItem(normalizedData, itemErrors)) {
                    errors.add("Row " + (i + 1) + ": " + String.join(", ", itemErrors));
                    failed++;
                    continue;
                }
                
                // Use normalized data for retrieval
                String name = normalizedData.get("name");
                String unitType = normalizedData.get("unittype");  // Now works for both "unitType" and "unittype"
                
                // Skip duplicates
                if (predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType)) {
                    logger.debug("Skipping duplicate: {} ({})", name, unitType);
                    failed++;
                    errors.add("Row " + (i + 1) + ": Duplicate item '" + name + "' with unit type '" + unitType + "'");
                    continue;
                }
                
                // Create item using normalized data
                String sku = normalizedData.getOrDefault("sku", null);
                String category = normalizedData.getOrDefault("category", null);
                String description = normalizedData.getOrDefault("description", null);
                BigDecimal defaultPrice = null;
                
                if (normalizedData.containsKey("defaultprice") && !normalizedData.get("defaultprice").isEmpty()) {
                    try {
                        defaultPrice = new BigDecimal(normalizedData.get("defaultprice"));
                    } catch (NumberFormatException e) {
                        errors.add("Row " + (i + 1) + ": Invalid price format");
                        failed++;
                        continue;
                    }
                }
                
                PredefinedItem item = createItem(name, sku, category, unitType, description, defaultPrice, company, createdBy);
                createdItems.add(item);
                successful++;
                
            } catch (Exception e) {
                logger.error("Error creating item from row {}: {}", i + 1, e.getMessage());
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
                failed++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", itemsData.size());
        result.put("successful", successful);
        result.put("failed", failed);
        result.put("errors", errors);
        result.put("createdItems", createdItems);
        
        logger.info("Bulk create completed: {} successful, {} failed out of {} total", 
                   successful, failed, itemsData.size());
        
        return result;
    }
    
    /**
     * Import items from CSV file
     */
    public Map<String, Object> importFromCSV(MultipartFile file, Company company, User createdBy) throws IOException {
        logger.info("Starting CSV import for company {}", company.getId());
        
        List<Map<String, String>> itemsData = csvService.parseImportCSV(file);
        return bulkCreateItems(itemsData, company, createdBy);
    }
    
    /**
     * Export items to CSV
     */
    public String exportToCSV(Company company) {
        List<PredefinedItem> items = predefinedItemRepository.findByCompanyAndIsActiveTrueOrderByNameAsc(company);
        logger.info("Exporting {} items to CSV for company {}", items.size(), company.getId());
        
        return csvService.generateExportCSV(items);
    }
    
    /**
     * Associate stores with a predefined item
     */
    public void associateStores(PredefinedItem item, List<UUID> storeIds, User user) {
        // Remove existing associations
        predefinedItemStoreRepository.deleteByPredefinedItem(item);
        
        // Create new associations
        for (UUID storeId : storeIds) {
            Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + storeId));
            
            // Verify store belongs to same company
            if (!store.getCompany().getId().equals(item.getCompany().getId())) {
                throw new IllegalArgumentException("Store does not belong to the same company");
            }
            
            PredefinedItemStore association = new PredefinedItemStore(item, store, user);
            predefinedItemStoreRepository.save(association);
        }
        
        logger.info("Associated {} stores with predefined item {}", storeIds.size(), item.getId());
    }
    
    /**
     * Associate warehouses with a predefined item
     */
    public void associateWarehouses(PredefinedItem item, List<UUID> warehouseIds, User user) {
        // Remove existing associations
        predefinedItemWarehouseRepository.deleteByPredefinedItem(item);
        
        // Create new associations
        for (UUID warehouseId : warehouseIds) {
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + warehouseId));
            
            // Verify warehouse belongs to same company
            if (!warehouse.getCompany().getId().equals(item.getCompany().getId())) {
                throw new IllegalArgumentException("Warehouse does not belong to the same company");
            }
            
            PredefinedItemWarehouse association = new PredefinedItemWarehouse(item, warehouse, user);
            predefinedItemWarehouseRepository.save(association);
        }
        
        logger.info("Associated {} warehouses with predefined item {}", warehouseIds.size(), item.getId());
    }
    
    /**
     * Get stores associated with a predefined item
     */
    public List<Store> getAssociatedStores(PredefinedItem item) {
        return predefinedItemStoreRepository.findStoresByPredefinedItem(item);
    }
    
    /**
     * Get warehouses associated with a predefined item
     */
    public List<Warehouse> getAssociatedWarehouses(PredefinedItem item) {
        return predefinedItemWarehouseRepository.findWarehousesByPredefinedItem(item);
    }
    
    /**
     * Get distinct categories for a company
     */
    public List<String> getCategories(Company company) {
        return predefinedItemRepository.findDistinctCategoriesByCompany(company);
    }
    
    /**
     * Get distinct unit types for a company
     */
    public List<String> getUnitTypes(Company company) {
        return predefinedItemRepository.findDistinctUnitTypesByCompany(company);
    }
}
