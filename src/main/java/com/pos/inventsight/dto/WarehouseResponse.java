package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for warehouse information
 */
public class WarehouseResponse {
    
    private UUID id;
    private String name;
    private String description;
    private String location;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
    private String managerName;
    private Warehouse.WarehouseType warehouseType;
    private BigDecimal capacityCubicMeters;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private String fullAddress;

    // Constructors
    public WarehouseResponse() {}

    public WarehouseResponse(Warehouse warehouse) {
        this.id = warehouse.getId();
        this.name = warehouse.getName();
        this.description = warehouse.getDescription();
        this.location = warehouse.getLocation();
        this.address = warehouse.getAddress();
        this.city = warehouse.getCity();
        this.state = warehouse.getState();
        this.postalCode = warehouse.getPostalCode();
        this.country = warehouse.getCountry();
        this.phone = warehouse.getPhone();
        this.email = warehouse.getEmail();
        this.managerName = warehouse.getManagerName();
        this.warehouseType = warehouse.getWarehouseType();
        this.capacityCubicMeters = warehouse.getCapacityCubicMeters();
        this.isActive = warehouse.getIsActive();
        this.createdAt = warehouse.getCreatedAt();
        this.updatedAt = warehouse.getUpdatedAt();
        this.createdBy = warehouse.getCreatedBy();
        this.updatedBy = warehouse.getUpdatedBy();
        this.fullAddress = warehouse.getFullAddress();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

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

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public Warehouse.WarehouseType getWarehouseType() { return warehouseType; }
    public void setWarehouseType(Warehouse.WarehouseType warehouseType) { this.warehouseType = warehouseType; }

    public BigDecimal getCapacityCubicMeters() { return capacityCubicMeters; }
    public void setCapacityCubicMeters(BigDecimal capacityCubicMeters) { this.capacityCubicMeters = capacityCubicMeters; }

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

    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
}