package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.PreexistingItem;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.repository.sql.PreexistingItemRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing pre-existing catalog items
 */
@Service
public class PreexistingItemService {
    
    @Autowired
    private PreexistingItemRepository itemRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    private final ObjectMapper objectMapper;
    
    public PreexistingItemService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Get all non-deleted items for a store
     */
    public List<PreexistingItem> getItemsByStore(UUID storeId) {
        System.out.println("📦 Fetching preexisting items for store: " + storeId);
        List<PreexistingItem> items = itemRepository.findByStoreIdAndIsDeletedFalse(storeId);
        System.out.println("✅ Found " + items.size() + " items");
        return items;
    }
    
    /**
     * Get item by ID
     */
    public Optional<PreexistingItem> getItemById(UUID itemId) {
        return itemRepository.findById(itemId);
    }
    
    /**
     * Create a new preexisting item
     */
    @Transactional
    public PreexistingItem createItem(PreexistingItem item, String createdBy) {
        System.out.println("➕ Creating new preexisting item: " + item.getItemName());
        
        // Validate store exists
        Store store = storeRepository.findById(item.getStore().getId())
            .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Check if SKU already exists for this store
        if (itemRepository.existsByStoreIdAndSku(store.getId(), item.getSku())) {
            throw new RuntimeException("Item with SKU " + item.getSku() + 
                                     " already exists for this store");
        }
        
        item.setStore(store);
        item.setCreatedBy(createdBy);
        
        PreexistingItem saved = itemRepository.save(item);
        System.out.println("✅ Item created successfully with ID: " + saved.getId());
        
        return saved;
    }
    
    /**
     * Update an existing item
     */
    @Transactional
    public PreexistingItem updateItem(UUID itemId, PreexistingItem updatedItem) {
        System.out.println("✏️ Updating preexisting item: " + itemId);
        
        PreexistingItem existingItem = itemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found"));
        
        // Check if updating SKU and if new SKU already exists
        if (!existingItem.getSku().equals(updatedItem.getSku())) {
            if (itemRepository.existsByStoreIdAndSku(
                existingItem.getStore().getId(), updatedItem.getSku())) {
                throw new RuntimeException("Item with SKU " + updatedItem.getSku() + 
                                         " already exists for this store");
            }
        }
        
        // Update fields
        existingItem.setItemName(updatedItem.getItemName());
        existingItem.setCategory(updatedItem.getCategory());
        existingItem.setDefaultPrice(updatedItem.getDefaultPrice());
        existingItem.setDescription(updatedItem.getDescription());
        existingItem.setSku(updatedItem.getSku());
        
        PreexistingItem saved = itemRepository.save(existingItem);
        System.out.println("✅ Item updated successfully");
        
        return saved;
    }
    
    /**
     * Soft delete an item
     */
    @Transactional
    public void deleteItem(UUID itemId) {
        System.out.println("🗑️ Soft deleting preexisting item: " + itemId);
        
        PreexistingItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found"));
        
        item.softDelete();
        itemRepository.save(item);
        
        System.out.println("✅ Item soft deleted successfully");
    }
    
    /**
     * Restore a soft-deleted item
     */
    @Transactional
    public void restoreItem(UUID itemId) {
        System.out.println("♻️ Restoring preexisting item: " + itemId);
        
        PreexistingItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found"));
        
        item.restore();
        itemRepository.save(item);
        
        System.out.println("✅ Item restored successfully");
    }
    
    /**
     * Export items to JSON
     */
    public String exportItemsToJson(UUID storeId) throws IOException {
        System.out.println("📤 Exporting items for store: " + storeId);
        
        List<PreexistingItem> items = itemRepository.findByStoreIdAndIsDeletedFalse(storeId);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
        
        System.out.println("✅ Exported " + items.size() + " items to JSON");
        return json;
    }
    
    /**
     * Import items from JSON file
     */
    @Transactional
    public int importItemsFromJson(UUID targetStoreId, MultipartFile file, String createdBy) 
            throws IOException {
        System.out.println("📥 Importing items to store: " + targetStoreId);
        
        Store targetStore = storeRepository.findById(targetStoreId)
            .orElseThrow(() -> new RuntimeException("Target store not found"));
        
        // Parse JSON
        String json = new String(file.getBytes());
        PreexistingItem[] items = objectMapper.readValue(json, PreexistingItem[].class);
        
        int importedCount = 0;
        int skippedCount = 0;
        
        for (PreexistingItem item : items) {
            try {
                // Check if SKU already exists
                if (itemRepository.existsByStoreIdAndSku(targetStoreId, item.getSku())) {
                    System.out.println("⚠️ Skipping item with duplicate SKU: " + item.getSku());
                    skippedCount++;
                    continue;
                }
                
                // Create new item for target store
                PreexistingItem newItem = new PreexistingItem();
                newItem.setStore(targetStore);
                newItem.setItemName(item.getItemName());
                newItem.setCategory(item.getCategory());
                newItem.setDefaultPrice(item.getDefaultPrice());
                newItem.setDescription(item.getDescription());
                newItem.setSku(item.getSku());
                newItem.setCreatedBy(createdBy);
                
                itemRepository.save(newItem);
                importedCount++;
                
            } catch (Exception e) {
                System.out.println("❌ Error importing item: " + e.getMessage());
                skippedCount++;
            }
        }
        
        System.out.println("✅ Import completed: " + importedCount + " imported, " + 
                         skippedCount + " skipped");
        return importedCount;
    }
    
    /**
     * Search items by name
     */
    public List<PreexistingItem> searchItems(UUID storeId, String searchTerm) {
        return itemRepository.searchByItemName(storeId, searchTerm);
    }
    
    /**
     * Get items by category
     */
    public List<PreexistingItem> getItemsByCategory(UUID storeId, String category) {
        return itemRepository.findByStoreIdAndCategory(storeId, category);
    }
    
    /**
     * Get item count for a store
     */
    public long getItemCount(UUID storeId) {
        return itemRepository.countByStoreId(storeId);
    }
}
