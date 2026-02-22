package com.pos.inventsight.dto;

import java.math.BigDecimal;

public class TopSellingProduct {
    
    private String name;
    private Long quantitySold;
    private BigDecimal totalRevenue;
    private String category;
    
    // Constructors
    public TopSellingProduct() {}
    
    public TopSellingProduct(String name, Long quantitySold, BigDecimal totalRevenue, String category) {
        this.name = name;
        this.quantitySold = quantitySold;
        this.totalRevenue = totalRevenue;
        this.category = category;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Long getQuantitySold() { return quantitySold; }
    public void setQuantitySold(Long quantitySold) { this.quantitySold = quantitySold; }
    
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
