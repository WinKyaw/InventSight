package com.pos.inventsight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO for approving and sending a transfer request (GM+ only)
 */
public class SendTransferRequestDTO {
    
    @NotNull(message = "Approved quantity is required")
    @Min(value = 0, message = "Approved quantity must be at least 0")
    private Integer approvedQuantity;
    
    private String carrierName;
    
    private String carrierPhone;
    
    private String carrierVehicle;
    
    private LocalDateTime estimatedDeliveryAt;
    
    private String notes;
    
    // Constructors
    public SendTransferRequestDTO() {
    }
    
    // Getters and Setters
    public Integer getApprovedQuantity() {
        return approvedQuantity;
    }
    
    public void setApprovedQuantity(Integer approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }
    
    public String getCarrierName() {
        return carrierName;
    }
    
    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }
    
    public String getCarrierPhone() {
        return carrierPhone;
    }
    
    public void setCarrierPhone(String carrierPhone) {
        this.carrierPhone = carrierPhone;
    }
    
    public String getCarrierVehicle() {
        return carrierVehicle;
    }
    
    public void setCarrierVehicle(String carrierVehicle) {
        this.carrierVehicle = carrierVehicle;
    }
    
    public LocalDateTime getEstimatedDeliveryAt() {
        return estimatedDeliveryAt;
    }
    
    public void setEstimatedDeliveryAt(LocalDateTime estimatedDeliveryAt) {
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
