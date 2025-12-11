package com.pos.inventsight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryValueDTO {
    
    private BigDecimal totalValue;
    private String currency;
    private LocalDateTime lastUpdated;
    
    // Constructors
    public InventoryValueDTO() {}
    
    public InventoryValueDTO(BigDecimal totalValue, String currency, LocalDateTime lastUpdated) {
        this.totalValue = totalValue;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters and Setters
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
