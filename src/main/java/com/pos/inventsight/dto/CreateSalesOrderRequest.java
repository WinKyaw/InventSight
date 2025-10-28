package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new sales order
 */
public class CreateSalesOrderRequest {
    
    @NotBlank(message = "Currency code is required")
    @Size(max = 3, min = 3, message = "Currency code must be exactly 3 characters")
    private String currencyCode;
    
    @Size(max = 200, message = "Customer name must not exceed 200 characters")
    private String customerName;
    
    @Size(max = 20, message = "Customer phone must not exceed 20 characters")
    private String customerPhone;
    
    @Size(max = 100, message = "Customer email must not exceed 100 characters")
    private String customerEmail;
    
    // Constructors
    public CreateSalesOrderRequest() {
    }
    
    public CreateSalesOrderRequest(String currencyCode, String customerName, String customerPhone) {
        this.currencyCode = currencyCode;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
    }
    
    // Getters and Setters
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
}
