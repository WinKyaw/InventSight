package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Company;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for company information
 */
public class CompanyResponse {
    
    private UUID id;
    private String name;
    private String description;
    private String email;
    private String phone;
    private String website;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String taxId;
    private String businessRegistrationNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private int storeCount;
    private int warehouseCount;
    private int userCount;
    
    // Constructors
    public CompanyResponse() {}
    
    public CompanyResponse(Company company) {
        this.id = company.getId();
        this.name = company.getName();
        this.description = company.getDescription();
        this.email = company.getEmail();
        this.phone = company.getPhone();
        this.website = company.getWebsite();
        this.address = company.getAddress();
        this.city = company.getCity();
        this.state = company.getState();
        this.postalCode = company.getPostalCode();
        this.country = company.getCountry();
        this.taxId = company.getTaxId();
        this.businessRegistrationNumber = company.getBusinessRegistrationNumber();
        this.isActive = company.getIsActive();
        this.createdAt = company.getCreatedAt();
        this.updatedAt = company.getUpdatedAt();
        this.createdBy = company.getCreatedBy();
        this.updatedBy = company.getUpdatedBy();
        this.storeCount = company.getStores().size();
        this.warehouseCount = company.getWarehouses().size();
        this.userCount = company.getCompanyStoreUsers().size();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    
    public String getBusinessRegistrationNumber() { return businessRegistrationNumber; }
    public void setBusinessRegistrationNumber(String businessRegistrationNumber) { 
        this.businessRegistrationNumber = businessRegistrationNumber; 
    }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public int getStoreCount() { return storeCount; }
    public void setStoreCount(int storeCount) { this.storeCount = storeCount; }
    
    public int getWarehouseCount() { return warehouseCount; }
    public void setWarehouseCount(int warehouseCount) { this.warehouseCount = warehouseCount; }
    
    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }
    
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null) sb.append(address);
        if (city != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (state != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        if (postalCode != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(postalCode);
        }
        if (country != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }
}