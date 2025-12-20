package com.pos.inventsight.dto;

import java.util.UUID;

/**
 * DTO for cashier statistics
 */
public class CashierStatsDTO {
    
    private UUID cashierId;
    private String cashierName;
    private Long receiptCount;
    
    // Constructors
    public CashierStatsDTO() {
    }
    
    public CashierStatsDTO(UUID cashierId, String cashierName, Long receiptCount) {
        this.cashierId = cashierId;
        this.cashierName = cashierName;
        this.receiptCount = receiptCount;
    }
    
    // Getters and Setters
    public UUID getCashierId() {
        return cashierId;
    }
    
    public void setCashierId(UUID cashierId) {
        this.cashierId = cashierId;
    }
    
    public String getCashierName() {
        return cashierName;
    }
    
    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }
    
    public Long getReceiptCount() {
        return receiptCount;
    }
    
    public void setReceiptCount(Long receiptCount) {
        this.receiptCount = receiptCount;
    }
}
