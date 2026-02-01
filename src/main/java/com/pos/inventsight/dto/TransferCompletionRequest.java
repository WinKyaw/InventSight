package com.pos.inventsight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for completing a transfer request
 */
public class TransferCompletionRequest {
    
    @NotNull(message = "Received quantity is required")
    @Min(value = 0, message = "Received quantity must be at least 0")
    private Integer receivedQuantity;
    
    @Min(value = 0, message = "Damaged quantity must be at least 0")
    private Integer damagedQuantity;
    
    private String conditionOnArrival;
    
    private String receiverName;
    
    private String receiptNotes;
    
    // Constructors
    public TransferCompletionRequest() {
    }
    
    public TransferCompletionRequest(Integer receivedQuantity, Integer damagedQuantity, 
                                     String conditionOnArrival, String receiverName, String receiptNotes) {
        this.receivedQuantity = receivedQuantity;
        this.damagedQuantity = damagedQuantity;
        this.conditionOnArrival = conditionOnArrival;
        this.receiverName = receiverName;
        this.receiptNotes = receiptNotes;
    }
    
    // Getters and Setters
    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }
    
    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }
    
    public Integer getDamagedQuantity() {
        return damagedQuantity;
    }
    
    public void setDamagedQuantity(Integer damagedQuantity) {
        this.damagedQuantity = damagedQuantity;
    }
    
    public String getConditionOnArrival() {
        return conditionOnArrival;
    }
    
    public void setConditionOnArrival(String conditionOnArrival) {
        this.conditionOnArrival = conditionOnArrival;
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
}
