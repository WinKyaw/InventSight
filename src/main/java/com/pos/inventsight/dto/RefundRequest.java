package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;

public class RefundRequest {
    @NotBlank(message = "Refund reason is required")
    private String reason;
    
    private String notes;
    
    // Getters and Setters
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}