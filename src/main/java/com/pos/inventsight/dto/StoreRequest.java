package com.pos.inventsight.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StoreRequest {
    
    @NotBlank(message = "Store name is required")
    @Size(max = 200, message = "Store name cannot exceed 200 characters")
    private String storeName;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    @Size(max = 200, message = "Address cannot exceed 200 characters")
    private String address;
    
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;
    
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;
    
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;
    
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;
    
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;
    
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;
    
    @Size(max = 200, message = "Website cannot exceed 200 characters")
    private String website;
    
    @Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    private String taxId;
    
    // Default constructor
    public StoreRequest() {}
    
    // Constructor for basic store creation
    public StoreRequest(String storeName, String description) {
        this.storeName = storeName;
        this.description = description;
    }
    
    // Getters and Setters
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
}