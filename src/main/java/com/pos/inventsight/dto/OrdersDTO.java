package com.pos.inventsight.dto;

public class OrdersDTO {
    
    private Long totalOrders;
    private Long todayOrders;
    private Double growthPercentage;
    private String period;
    
    // Constructors
    public OrdersDTO() {}
    
    public OrdersDTO(Long totalOrders, Long todayOrders, Double growthPercentage, String period) {
        this.totalOrders = totalOrders;
        this.todayOrders = todayOrders;
        this.growthPercentage = growthPercentage;
        this.period = period;
    }
    
    // Getters and Setters
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    
    public Long getTodayOrders() { return todayOrders; }
    public void setTodayOrders(Long todayOrders) { this.todayOrders = todayOrders; }
    
    public Double getGrowthPercentage() { return growthPercentage; }
    public void setGrowthPercentage(Double growthPercentage) { this.growthPercentage = growthPercentage; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
