package com.pos.inventsight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for confirming receipt of a transfer
 */
public class ReceiveTransferDTO {
    
    @NotNull(message = "Received quantity is required")
    @Min(value = 0, message = "Received quantity must be at least 0")
    private Integer receivedQuantity;
    
    private String receiverName;
    
    private String receiptNotes;
    
    private Boolean damageReported;
    
    // Constructors
    public ReceiveTransferDTO() {
    }
    
    // Getters and Setters
    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }
    
    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
    
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    
    public String getReceiptNotes() {
        return receiptNotes;
    }
    
    public void setReceiptNotes(String receiptNotes) {
        this.receiptNotes = receiptNotes;
    }
    
    public Boolean getDamageReported() {
        return damageReported;
    }
    
    public void setDamageReported(Boolean damageReported) {
        this.damageReported = damageReported;
    }
}
