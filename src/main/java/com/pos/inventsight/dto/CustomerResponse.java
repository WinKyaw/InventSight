package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Customer;

import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String notes;
    private UUID companyId;
    private String companyName;
    private UUID storeId;
    private String storeName;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    
    // Default constructor
    public CustomerResponse() {}
    
    // Constructor from Customer entity
    public CustomerResponse(Customer customer) {
        this.id = customer.getId();
        this.name = customer.getName();
        this.email = customer.getEmail();
        this.phone = customer.getPhoneNumber();
        this.address = customer.getAddress();
        this.city = customer.getCity();
        this.state = customer.getState();
        this.postalCode = customer.getPostalCode();
        this.country = customer.getCountry();
        this.notes = customer.getNotes();
        
        // Cache company to avoid multiple method calls
        com.pos.inventsight.model.sql.Company company = customer.getCompany();
        if (company != null) {
            this.companyId = company.getId();
            this.companyName = company.getName();
        }
        
        if (customer.getStore() != null) {
            this.storeId = customer.getStore().getId();
            this.storeName = customer.getStore().getStoreName();
        }
        if (customer.getCreatedByUser() != null) {
            this.createdBy = customer.getCreatedByUser().getUsername();
        }
        this.createdAt = customer.getCreatedAt();
        this.updatedAt = customer.getUpdatedAt();
        this.isActive = customer.getIsActive();
    }
    
    // Getters and Setters
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
    
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
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
    
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public UUID getCompanyId() {
        return companyId;
    }
    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public UUID getStoreId() {
        return storeId;
    }
    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }
    
    public String getStoreName() {
        return storeName;
    }
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
