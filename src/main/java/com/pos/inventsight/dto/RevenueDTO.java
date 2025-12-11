package com.pos.inventsight.dto;

import java.math.BigDecimal;

public class RevenueDTO {
    
    private BigDecimal totalRevenue;
    private Double growthPercentage;
    private String period;
    private BigDecimal previousPeriodRevenue;
    private String currency;
    
    // Constructors
    public RevenueDTO() {}
    
    public RevenueDTO(BigDecimal totalRevenue, Double growthPercentage, String period, 
                     BigDecimal previousPeriodRevenue, String currency) {
        this.totalRevenue = totalRevenue;
        this.growthPercentage = growthPercentage;
        this.period = period;
        this.previousPeriodRevenue = previousPeriodRevenue;
        this.currency = currency;
    }
    
    // Getters and Setters
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public Double getGrowthPercentage() { return growthPercentage; }
    public void setGrowthPercentage(Double growthPercentage) { this.growthPercentage = growthPercentage; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    public BigDecimal getPreviousPeriodRevenue() { return previousPeriodRevenue; }
    public void setPreviousPeriodRevenue(BigDecimal previousPeriodRevenue) { 
        this.previousPeriodRevenue = previousPeriodRevenue; 
    }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
