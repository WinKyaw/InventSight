package com.pos.inventsight.dto;

import java.util.List;

public class CategoryStatsDTO {
    
    private Long totalCategories;
    private List<TopCategory> topCategories;
    
    // Constructors
    public CategoryStatsDTO() {}
    
    public CategoryStatsDTO(Long totalCategories, List<TopCategory> topCategories) {
        this.totalCategories = totalCategories;
        this.topCategories = topCategories;
    }
    
    // Getters and Setters
    public Long getTotalCategories() { return totalCategories; }
    public void setTotalCategories(Long totalCategories) { this.totalCategories = totalCategories; }
    
    public List<TopCategory> getTopCategories() { return topCategories; }
    public void setTopCategories(List<TopCategory> topCategories) { this.topCategories = topCategories; }
    
    // Inner class for top category details
    public static class TopCategory {
        private String name;
        private Long productCount;
        
        public TopCategory() {}
        
        public TopCategory(String name, Long productCount) {
            this.name = name;
            this.productCount = productCount;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Long getProductCount() { return productCount; }
        public void setProductCount(Long productCount) { this.productCount = productCount; }
    }
}
