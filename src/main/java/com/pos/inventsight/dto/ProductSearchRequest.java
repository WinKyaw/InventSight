package com.pos.inventsight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductSearchRequest {
    private String query;
    private String category;
    private String supplier;
    private String location;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minQuantity;
    private Integer maxQuantity;
    private Boolean lowStock;
    private Boolean outOfStock;
    private Boolean needsReorder;
    private Boolean active;
    private Boolean nearExpiry;
    private Boolean expired;
    private LocalDate expiryBefore;
    private LocalDate expiryAfter;
    private String sortBy = "name";
    private String sortDirection = "asc";
    private Integer page = 0;
    private Integer size = 20;

    public ProductSearchRequest() {}

    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }

    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }

    public Boolean getLowStock() { return lowStock; }
    public void setLowStock(Boolean lowStock) { this.lowStock = lowStock; }

    public Boolean getOutOfStock() { return outOfStock; }
    public void setOutOfStock(Boolean outOfStock) { this.outOfStock = outOfStock; }

    public Boolean getNeedsReorder() { return needsReorder; }
    public void setNeedsReorder(Boolean needsReorder) { this.needsReorder = needsReorder; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getNearExpiry() { return nearExpiry; }
    public void setNearExpiry(Boolean nearExpiry) { this.nearExpiry = nearExpiry; }

    public Boolean getExpired() { return expired; }
    public void setExpired(Boolean expired) { this.expired = expired; }

    public LocalDate getExpiryBefore() { return expiryBefore; }
    public void setExpiryBefore(LocalDate expiryBefore) { this.expiryBefore = expiryBefore; }

    public LocalDate getExpiryAfter() { return expiryAfter; }
    public void setExpiryAfter(LocalDate expiryAfter) { this.expiryAfter = expiryAfter; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}