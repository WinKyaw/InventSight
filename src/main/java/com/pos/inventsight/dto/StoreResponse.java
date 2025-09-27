package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Store;
import java.time.LocalDateTime;
import java.util.UUID;

public class StoreResponse {
    private UUID id;
    private String storeName;
    private String description;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
    private String website;
    private String taxId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Default constructor
    public StoreResponse() {}
    
    // Constructor from Store entity
    public StoreResponse(Store store) {
        this.id = store.getId();
        this.storeName = store.getStoreName();
        this.description = store.getDescription();
        this.address = store.getAddress();
        this.city = store.getCity();
        this.state = store.getState();
        this.postalCode = store.getPostalCode();
        this.country = store.getCountry();
        this.phone = store.getPhone();
        this.email = store.getEmail();
        this.website = store.getWebsite();
        this.taxId = store.getTaxId();
        this.isActive = store.getIsActive();
        this.createdAt = store.getCreatedAt();
        this.updatedAt = store.getUpdatedAt();
        this.createdBy = store.getCreatedBy();
        this.updatedBy = store.getUpdatedBy();
    }
    
    // Getters and Setters
    public UUID getId() { 
        return id; 
    }
    public void setId(UUID id) { 
        this.id = id; 
    }
    
    public String getStoreName() { 
        return storeName; 
    }
    public void setStoreName(String storeName) { 
        this.storeName = storeName; 
    }
    
    public String getDescription() { 
        return description; 
    }
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    public String getAddress() { 
        return address; 
    }
    public void setAddress(String address) { 
        this.address = address; 
    }
    
    public String getCity() { 
        return city; 
    }
    public void setCity(String city) { 
        this.city = city; 
    }
    
    public String getState() { 
        return state; 
    }
    public void setState(String state) { 
        this.state = state; 
    }
    
    public String getPostalCode() { 
        return postalCode; 
    }
    public void setPostalCode(String postalCode) { 
        this.postalCode = postalCode; 
    }
    
    public String getCountry() { 
        return country; 
    }
    public void setCountry(String country) { 
        this.country = country; 
    }
    
    public String getPhone() { 
        return phone; 
    }
    public void setPhone(String phone) { 
        this.phone = phone; 
    }
    
    public String getEmail() { 
        return email; 
    }
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    public String getWebsite() { 
        return website; 
    }
    public void setWebsite(String website) { 
        this.website = website; 
    }
    
    public String getTaxId() { 
        return taxId; 
    }
    public void setTaxId(String taxId) { 
        this.taxId = taxId; 
    }
    
    public Boolean getIsActive() { 
        return isActive; 
    }
    public void setIsActive(Boolean isActive) { 
        this.isActive = isActive; 
    }
    
    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }
    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }
    
    public LocalDateTime getUpdatedAt() { 
        return updatedAt; 
    }
    public void setUpdatedAt(LocalDateTime updatedAt) { 
        this.updatedAt = updatedAt; 
    }
    
    public String getCreatedBy() { 
        return createdBy; 
    }
    public void setCreatedBy(String createdBy) { 
        this.createdBy = createdBy; 
    }
    
    public String getUpdatedBy() { 
        return updatedBy; 
    }
    public void setUpdatedBy(String updatedBy) { 
        this.updatedBy = updatedBy; 
    }
}