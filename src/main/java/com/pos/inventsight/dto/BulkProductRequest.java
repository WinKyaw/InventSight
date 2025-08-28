package com.pos.inventsight.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BulkProductRequest {
    @NotNull(message = "Products list cannot be null")
    @Size(min = 1, max = 100, message = "Products list must contain between 1 and 100 items")
    @Valid
    private List<ProductRequest> products;
    
    private boolean skipDuplicates = true;
    private boolean validateStock = true;
    
    public BulkProductRequest() {}

    public BulkProductRequest(List<ProductRequest> products) {
        this.products = products;
    }

    // Getters and Setters
    public List<ProductRequest> getProducts() { return products; }
    public void setProducts(List<ProductRequest> products) { this.products = products; }

    public boolean isSkipDuplicates() { return skipDuplicates; }
    public void setSkipDuplicates(boolean skipDuplicates) { this.skipDuplicates = skipDuplicates; }

    public boolean isValidateStock() { return validateStock; }
    public void setValidateStock(boolean validateStock) { this.validateStock = validateStock; }
}