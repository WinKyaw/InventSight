package com.pos.inventsight.dto;

import java.util.List;
import java.util.ArrayList;

/**
 * Response DTO for batch add inventory operation
 */
public class StoreInventoryBatchAddResponse {
    
    private int totalItems;
    private int successfulItems;
    private int failedItems;
    private List<StoreInventoryAdditionResponse> additions;
    private List<BatchError> errors;
    
    public StoreInventoryBatchAddResponse() {
        this.additions = new ArrayList<>();
        this.errors = new ArrayList<>();
    }
    
    /**
     * Error for a failed item in the batch
     */
    public static class BatchError {
        private String productId;
        private String productName;
        private String error;
        
        public BatchError(String productId, String productName, String error) {
            this.productId = productId;
            this.productName = productName;
            this.error = error;
        }
        
        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    // Getters and setters
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    
    public int getSuccessfulItems() { return successfulItems; }
    public void setSuccessfulItems(int successfulItems) { this.successfulItems = successfulItems; }
    
    public int getFailedItems() { return failedItems; }
    public void setFailedItems(int failedItems) { this.failedItems = failedItems; }
    
    public List<StoreInventoryAdditionResponse> getAdditions() { return additions; }
    public void setAdditions(List<StoreInventoryAdditionResponse> additions) { this.additions = additions; }
    
    public List<BatchError> getErrors() { return errors; }
    public void setErrors(List<BatchError> errors) { this.errors = errors; }
}
