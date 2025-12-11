package com.pos.inventsight.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class BestPerformerDTO {
    
    private ProductInfo product;
    private String period;
    
    // Constructors
    public BestPerformerDTO() {}
    
    public BestPerformerDTO(ProductInfo product, String period) {
        this.product = product;
        this.period = period;
    }
    
    // Getters and Setters
    public ProductInfo getProduct() { return product; }
    public void setProduct(ProductInfo product) { this.product = product; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    // Inner class for product info
    public static class ProductInfo {
        private UUID id;
        private String name;
        private String sku;
        private Long unitsSold;
        private BigDecimal revenue;
        private String category;
        
        public ProductInfo() {}
        
        public ProductInfo(UUID id, String name, String sku, Long unitsSold, BigDecimal revenue, String category) {
            this.id = id;
            this.name = name;
            this.sku = sku;
            this.unitsSold = unitsSold;
            this.revenue = revenue;
            this.category = category;
        }
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public Long getUnitsSold() { return unitsSold; }
        public void setUnitsSold(Long unitsSold) { this.unitsSold = unitsSold; }
        
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}
