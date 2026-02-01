package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * DTO for marking a transfer as shipped (IN_TRANSIT)
 */
public class TransferShipmentRequest {
    
    @NotBlank(message = "Carrier name is required")
    private String carrierName;
    
    private String carrierPhone;
    
    private String carrierVehicle;
    
    private LocalDateTime estimatedDeliveryAt;
    
    // Constructors
    public TransferShipmentRequest() {
    }
    
    public TransferShipmentRequest(String carrierName, String carrierPhone, 
                                   String carrierVehicle, LocalDateTime estimatedDeliveryAt) {
        this.carrierName = carrierName;
        this.carrierPhone = carrierPhone;
        this.carrierVehicle = carrierVehicle;
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }
    
    // Getters and Setters
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
}
