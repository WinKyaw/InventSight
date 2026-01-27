package com.pos.inventsight.dto;

import java.util.UUID;

/**
 * DTO for transfer location details
 */
public class TransferLocationDTO {
    
    private String type; // "WAREHOUSE" or "STORE"
    private UUID id;
    private String name;
    
    public TransferLocationDTO() {
    }
    
    public TransferLocationDTO(String type, UUID id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
