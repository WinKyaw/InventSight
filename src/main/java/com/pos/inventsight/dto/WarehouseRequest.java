package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Warehouse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating and updating warehouses
 */
public class WarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 200, message = "Warehouse name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 500, message = "Location must not exceed 500 characters")
    private String location;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 100, message = "Manager name must not exceed 100 characters")
    private String managerName;

    private Warehouse.WarehouseType warehouseType;

    @DecimalMin(value = "0.0", message = "Capacity must be non-negative")
    private BigDecimal capacityCubicMeters;

    private Boolean isActive = true;

    // Getters and Setters
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
}