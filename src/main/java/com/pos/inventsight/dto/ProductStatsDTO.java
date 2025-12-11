package com.pos.inventsight.dto;

public class ProductStatsDTO {
    
    private Long totalProducts;
    private Long activeProducts;
    private Long inactiveProducts;
    
    // Constructors
    public ProductStatsDTO() {}
    
    public ProductStatsDTO(Long totalProducts, Long activeProducts, Long inactiveProducts) {
        this.totalProducts = totalProducts;
        this.activeProducts = activeProducts;
        this.inactiveProducts = inactiveProducts;
    }
    
    // Getters and Setters
    public Long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(Long totalProducts) { this.totalProducts = totalProducts; }
    
    public Long getActiveProducts() { return activeProducts; }
    public void setActiveProducts(Long activeProducts) { this.activeProducts = activeProducts; }
    
    public Long getInactiveProducts() { return inactiveProducts; }
    public void setInactiveProducts(Long inactiveProducts) { this.inactiveProducts = inactiveProducts; }
}
