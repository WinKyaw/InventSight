package com.pos.inventsight.dto;

import java.util.List;
import java.util.UUID;

public class LowStockDTO {
    
    private Long lowStockCount;
    private List<LowStockProduct> products;
    
    // Constructors
    public LowStockDTO() {}
    
    public LowStockDTO(Long lowStockCount, List<LowStockProduct> products) {
        this.lowStockCount = lowStockCount;
        this.products = products;
    }
    
    // Getters and Setters
    public Long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(Long lowStockCount) { this.lowStockCount = lowStockCount; }
    
    public List<LowStockProduct> getProducts() { return products; }
    public void setProducts(List<LowStockProduct> products) { this.products = products; }
    
    // Inner class for product details
    public static class LowStockProduct {
        private UUID id;
        private String name;
        private Integer currentStock;
        private Integer minStock;
        private String sku;
        
        public LowStockProduct() {}
        
        public LowStockProduct(UUID id, String name, Integer currentStock, Integer minStock, String sku) {
            this.id = id;
            this.name = name;
            this.currentStock = currentStock;
            this.minStock = minStock;
            this.sku = sku;
        }
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Integer getCurrentStock() { return currentStock; }
        public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }
        
        public Integer getMinStock() { return minStock; }
        public void setMinStock(Integer minStock) { this.minStock = minStock; }
        
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
    }
}
