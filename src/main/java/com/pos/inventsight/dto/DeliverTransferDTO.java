package com.pos.inventsight.dto;

/**
 * DTO for marking a transfer as delivered
 */
public class DeliverTransferDTO {
    
    private String proofOfDeliveryUrl;
    
    private String conditionOnArrival;
    
    private String notes;
    
    // Constructors
    public DeliverTransferDTO() {
    }
    
    public DeliverTransferDTO(String proofOfDeliveryUrl, String conditionOnArrival, String notes) {
        this.proofOfDeliveryUrl = proofOfDeliveryUrl;
        this.conditionOnArrival = conditionOnArrival;
        this.notes = notes;
    }
    
    // Getters and Setters
    public String getProofOfDeliveryUrl() {
        return proofOfDeliveryUrl;
    }
    
    public void setProofOfDeliveryUrl(String proofOfDeliveryUrl) {
        this.proofOfDeliveryUrl = proofOfDeliveryUrl;
    }
    
    public String getConditionOnArrival() {
        return conditionOnArrival;
    }
    
    public void setConditionOnArrival(String conditionOnArrival) {
        this.conditionOnArrival = conditionOnArrival;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
