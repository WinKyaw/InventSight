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
import org.springframework.transaction.annotation.Propagation;
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
    private ProductRepository productRepository;
    
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
            User createdBy,
            List<UUID> storeIds,
            List<UUID> warehouseIds) {
        
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
        
        // Auto-associate with stores if provided
        if (storeIds != null && !storeIds.isEmpty()) {
            associateStores(saved, storeIds, createdBy);
            logger.info("Auto-associated {} stores with predefined item {}", storeIds.size(), saved.getId());
        }
        
        // Auto-associate with warehouses if provided
        if (warehouseIds != null && !warehouseIds.isEmpty()) {
            associateWarehouses(saved, warehouseIds, createdBy);
            logger.info("Auto-associated {} warehouses with predefined item {}", warehouseIds.size(), saved.getId());
        }
        
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
     * Accepts 4 required fields: name, category, unitType, defaultprice
     * SKU is always auto-generated and ignored if provided
     */
    public Map<String, Object> bulkCreateItems(
            List<Map<String, String>> itemsData,
            Company company,
            User createdBy,
            List<UUID> storeIds,
            List<UUID> warehouseIds) {
        
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
                
                logger.debug("Row {}: Normalized data = {}", i + 1, normalizedData);
                
                // Extract required fields
                String name = normalizedData.get("name");
                String category = normalizedData.get("category");
                String unitType = normalizedData.get("unittype");
                String defaultPriceStr = normalizedData.get("defaultprice");
                
                // Validate required fields
                if (name == null || name.trim().isEmpty()) {
                    errors.add("Row " + (i + 1) + ": 'name' is required");
                    failed++;
                    continue;
                }
                
                if (category == null || category.trim().isEmpty()) {
                    errors.add("Row " + (i + 1) + ": 'category' is required");
                    failed++;
                    continue;
                }
                
                if (unitType == null || unitType.trim().isEmpty()) {
                    errors.add("Row " + (i + 1) + ": 'unitType' is required");
                    failed++;
                    continue;
                }
                
                if (defaultPriceStr == null || defaultPriceStr.trim().isEmpty()) {
                    errors.add("Row " + (i + 1) + ": 'defaultprice' is required");
                    failed++;
                    continue;
                }
                
                // Parse price
                BigDecimal defaultPrice;
                try {
                    defaultPrice = new BigDecimal(defaultPriceStr);
                    if (defaultPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Row " + (i + 1) + ": Price must be greater than zero");
                        failed++;
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add("Row " + (i + 1) + ": Invalid price format: " + defaultPriceStr);
                    failed++;
                    continue;
                }
                
                // Check for duplicates
                if (predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType)) {
                    logger.debug("Skipping duplicate: {} ({})", name, unitType);
                    failed++;
                    errors.add("Row " + (i + 1) + ": Duplicate item '" + name + "' with unit type '" + unitType + "'");
                    continue;
                }
                
                // Create item (SKU is auto-generated, description is optional)
                String description = normalizedData.getOrDefault("description", null);
                
                PredefinedItem item = createItem(
                    name,
                    null,  // sku - will be auto-generated
                    category,
                    unitType,
                    description,
                    defaultPrice,
                    company,
                    createdBy,
                    storeIds,
                    warehouseIds
                );
                
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
    public Map<String, Object> importFromCSV(MultipartFile file, Company company, User createdBy, List<UUID> storeIds, List<UUID> warehouseIds) throws IOException {
        logger.info("Starting CSV import for company {}", company.getId());
        
        List<Map<String, String>> itemsData = csvService.parseImportCSV(file);
        return bulkCreateItems(itemsData, company, createdBy, storeIds, warehouseIds);
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
        logger.info("Associating {} stores with predefined item {}", storeIds.size(), item.getId());
        
        // Create associations only for stores that don't already have them
        for (UUID storeId : storeIds) {
            logger.debug("Looking up store: {}", storeId);
            
            Store store = storeRepository.findByIdWithCompany(storeId)
                .orElseThrow(() -> {
                    logger.error("Store not found with ID: {}", storeId);
                    return new ResourceNotFoundException("Store not found with ID: " + storeId);
                });
            
            logger.debug("Found store: {} (company: {})", store.getStoreName(), 
                        store.getCompany() != null ? store.getCompany().getId() : "NULL");
            
            // Verify store belongs to same company
            if (store.getCompany() == null) {
                logger.error("Store {} has no company set!", storeId);
                throw new IllegalStateException("Store has no company assigned");
            }
            
            if (!store.getCompany().getId().equals(item.getCompany().getId())) {
                logger.error("Store {} belongs to different company. Expected: {}, Got: {}", 
                            storeId, item.getCompany().getId(), store.getCompany().getId());
                throw new IllegalArgumentException("Store does not belong to the same company");
            }
            
            logger.debug("Creating association between item {} and store {}", item.getId(), store.getId());
            
            // 1. Create product in separate transaction (won't rollback if association fails)
            createProductForStore(item, store, user);
            
            // 2. Check if association already exists
            Optional<PredefinedItemStore> existingAssociation = predefinedItemStoreRepository
                .findByPredefinedItemIdAndStoreId(item.getId(), store.getId());
            
            if (existingAssociation.isEmpty()) {
                // Create association record only if it doesn't exist
                PredefinedItemStore association = new PredefinedItemStore(item, store, user);
                predefinedItemStoreRepository.save(association);
                logger.debug("Created association for store {} (ID: {})", store.getStoreName(), store.getId());
            } else {
                logger.debug("Association already exists for store {} (ID: {}), skipping", store.getStoreName(), store.getId());
            }
        }
        
        logger.info("Successfully associated {} stores with predefined item {}", storeIds.size(), item.getId());
    }
    
    /**
     * Associate warehouses with a predefined item
     */
    public void associateWarehouses(PredefinedItem item, List<UUID> warehouseIds, User user) {
        logger.info("Associating {} warehouses with predefined item {}", warehouseIds.size(), item.getId());
        
        int associatedCount = 0;
        int skippedCount = 0;
        
        // Create associations only for warehouses that don't already have them
        for (UUID warehouseId : warehouseIds) {
            logger.debug("Looking up warehouse: {}", warehouseId);
            
            Warehouse warehouse = warehouseRepository.findByIdWithCompany(warehouseId)
                .orElseThrow(() -> {
                    logger.error("Warehouse not found with ID: {}", warehouseId);
                    return new ResourceNotFoundException("Warehouse not found with ID: " + warehouseId);
                });
            
            logger.debug("Found warehouse: {} (company: {})", warehouse.getName(), 
                        warehouse.getCompany() != null ? warehouse.getCompany().getId() : "NULL");
            
            // Verify warehouse belongs to same company
            if (warehouse.getCompany() == null) {
                logger.error("Warehouse {} has no company set!", warehouseId);
                throw new IllegalStateException("Warehouse has no company assigned");
            }
            
            if (!warehouse.getCompany().getId().equals(item.getCompany().getId())) {
                logger.error("Warehouse {} belongs to different company. Expected: {}, Got: {}", 
                            warehouseId, item.getCompany().getId(), warehouse.getCompany().getId());
                throw new IllegalArgumentException("Warehouse does not belong to the same company");
            }
            
            logger.debug("Creating association between item {} and warehouse {}", item.getId(), warehouse.getId());
            
            // 1. Create product in separate transaction (won't rollback if association fails)
            createProductForWarehouse(item, warehouse, user);
            
            // 2. Check if association already exists
            Optional<PredefinedItemWarehouse> existingAssociation = predefinedItemWarehouseRepository
                .findByPredefinedItemIdAndWarehouseId(item.getId(), warehouse.getId());
            
            if (existingAssociation.isEmpty()) {
                // Only insert if it doesn't exist
                PredefinedItemWarehouse association = new PredefinedItemWarehouse(item, warehouse, user);
                predefinedItemWarehouseRepository.save(association);
                
                logger.debug("Created association for warehouse {} (ID: {})", warehouse.getName(), warehouse.getId());
                associatedCount++;
            } else {
                logger.debug("Association already exists for warehouse {} (ID: {}), skipping", warehouse.getName(), warehouse.getId());
                skippedCount++;
            }
        }
        
        logger.info("Successfully associated {} warehouses (skipped {} existing) with predefined item {}", 
            associatedCount, skippedCount, item.getId());
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
    
    /**
     * Create product for store in a separate transaction
     * This ensures product creation won't rollback if association insert fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void createProductForStore(PredefinedItem predefinedItem, Store store, User user) {
        // Check if product already exists
        Optional<Product> existingProduct = productRepository
            .findByPredefinedItemAndStore(predefinedItem, store);
        
        if (existingProduct.isEmpty()) {
            Product product = new Product();
            product.setPredefinedItem(predefinedItem);
            product.setStore(store);
            product.setCompany(store.getCompany());
            product.setName(predefinedItem.getName());
            product.setSku(predefinedItem.getSku());
            product.setCategory(predefinedItem.getCategory());
            product.setUnit(predefinedItem.getUnitType());
            product.setDescription(predefinedItem.getDescription());
            
            // Copy price from default_price to all price fields for backward compatibility
            BigDecimal defaultPrice = predefinedItem.getDefaultPrice();
            product.setPrice(defaultPrice); // Legacy price field
            product.setOriginalPrice(defaultPrice);
            product.setOwnerSetSellPrice(defaultPrice);
            product.setRetailPrice(defaultPrice);
            
            product.setQuantity(0); // Initial stock is 0
            product.setLowStockThreshold(5); // Default low stock threshold
            product.setCreatedBy(user.getUsername());
            product.setIsActive(true);
            
            productRepository.save(product);
            logger.debug("Created product for store {} (ID: {})", store.getStoreName(), store.getId());
        } else {
            logger.debug("Product already exists for store {} (ID: {})", store.getStoreName(), store.getId());
        }
    }
    
    /**
     * Create product for warehouse in a separate transaction
     * This ensures product creation won't rollback if association insert fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void createProductForWarehouse(PredefinedItem predefinedItem, Warehouse warehouse, User user) {
        // Check if product already exists
        Optional<Product> existingProduct = productRepository
            .findByPredefinedItemAndWarehouse(predefinedItem, warehouse);
        
        if (existingProduct.isEmpty()) {
            Product product = new Product();
            product.setPredefinedItem(predefinedItem);
            product.setWarehouse(warehouse);
            product.setCompany(warehouse.getCompany());
            product.setName(predefinedItem.getName());
            product.setSku(predefinedItem.getSku());
            product.setCategory(predefinedItem.getCategory());
            product.setUnit(predefinedItem.getUnitType());
            product.setDescription(predefinedItem.getDescription());
            
            // Copy price from default_price to all price fields for backward compatibility
            BigDecimal defaultPrice = predefinedItem.getDefaultPrice();
            product.setPrice(defaultPrice); // Legacy price field
            product.setOriginalPrice(defaultPrice);
            product.setOwnerSetSellPrice(defaultPrice);
            product.setRetailPrice(defaultPrice);
            
            product.setQuantity(0); // Initial stock is 0
            product.setLowStockThreshold(5); // Default low stock threshold
            product.setCreatedBy(user.getUsername());
            product.setIsActive(true);
            
            productRepository.save(product);
            logger.debug("Created product for warehouse {} (ID: {})", warehouse.getName(), warehouse.getId());
        } else {
            logger.debug("Product already exists for warehouse {} (ID: {})", warehouse.getName(), warehouse.getId());
        }
    }
}
