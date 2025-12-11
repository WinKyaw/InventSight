package com.pos.inventsight.dto;

import java.math.BigDecimal;

public class AvgOrderValueDTO {
    
    private BigDecimal avgOrderValue;
    private String currency;
    private String period;
    private Long totalOrders;
    
    // Constructors
    public AvgOrderValueDTO() {}
    
    public AvgOrderValueDTO(BigDecimal avgOrderValue, String currency, String period, Long totalOrders) {
        this.avgOrderValue = avgOrderValue;
        this.currency = currency;
        this.period = period;
        this.totalOrders = totalOrders;
    }
    
    // Getters and Setters
    public BigDecimal getAvgOrderValue() { return avgOrderValue; }
    public void setAvgOrderValue(BigDecimal avgOrderValue) { this.avgOrderValue = avgOrderValue; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
}
