package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for rejecting a transfer request
 */
public class TransferRejectionRequest {
    
    @NotBlank(message = "Rejection reason is required")
    private String reason;
    
    // Constructors
    public TransferRejectionRequest() {
    }
    
    public TransferRejectionRequest(String reason) {
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
