package com.pos.inventsight.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for batch adding inventory to store (multi-item restock)
 */
public class StoreInventoryBatchAddRequest {
    
    @NotNull(message = "Store ID is required")
    private UUID storeId;
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<BatchItem> items;
    
    private String globalNotes; // Optional notes applied to all items
    
    /**
     * Individual item in the batch
     */
    public static class BatchItem {
        @NotNull(message = "Product ID is required")
        private UUID productId;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        
        private String notes; // Item-specific notes (overrides globalNotes if present)
        
        // Getters and setters
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    // Getters and setters
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    
    public List<BatchItem> getItems() { return items; }
    public void setItems(List<BatchItem> items) { this.items = items; }
    
    public String getGlobalNotes() { return globalNotes; }
    public void setGlobalNotes(String globalNotes) { this.globalNotes = globalNotes; }
}
