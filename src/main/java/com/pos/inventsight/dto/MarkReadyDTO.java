package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for marking a transfer as ready for pickup
 */
public class MarkReadyDTO {
    
    @NotBlank(message = "Packed by name is required")
    private String packedBy;
    
    private String notes;
    
    // Constructors
    public MarkReadyDTO() {
    }
    
    public MarkReadyDTO(String packedBy, String notes) {
        this.packedBy = packedBy;
        this.notes = notes;
    }
    
    // Getters and Setters
    public String getPackedBy() {
        return packedBy;
    }
    
    public void setPackedBy(String packedBy) {
        this.packedBy = packedBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
