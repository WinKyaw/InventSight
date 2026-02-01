package com.pos.inventsight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for approving a transfer request
 */
public class TransferApprovalRequest {
    
    @NotNull(message = "Approved quantity is required")
    @Min(value = 1, message = "Approved quantity must be at least 1")
    private Integer approvedQuantity;
    
    private String notes;
    
    // Constructors
    public TransferApprovalRequest() {
    }
    
    public TransferApprovalRequest(Integer approvedQuantity, String notes) {
        this.approvedQuantity = approvedQuantity;
        this.notes = notes;
    }
    
    // Getters and Setters
    public Integer getApprovedQuantity() {
        return approvedQuantity;
    }
    
    public void setApprovedQuantity(Integer approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
