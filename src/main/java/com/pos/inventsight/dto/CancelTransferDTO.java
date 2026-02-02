package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for cancelling a transfer request
 */
public class CancelTransferDTO {
    
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
    
    // Constructors
    public CancelTransferDTO() {
    }
    
    public CancelTransferDTO(String reason) {
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
