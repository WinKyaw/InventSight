package com.pos.inventsight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for setting product prices (original, owner-set sell, or retail)
 * Restricted to FOUNDER and GENERAL_MANAGER roles only
 */
public class SetPriceRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be greater than or equal to 0")
    private BigDecimal amount;
    
    private String reason;
    
    // Constructors
    public SetPriceRequest() {
    }
    
    public SetPriceRequest(BigDecimal amount) {
        this.amount = amount;
    }
    
    public SetPriceRequest(BigDecimal amount, String reason) {
        this.amount = amount;
        this.reason = reason;
    }
    
    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
