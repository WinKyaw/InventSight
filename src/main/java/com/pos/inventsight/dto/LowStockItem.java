package com.pos.inventsight.dto;

import java.util.UUID;

public class LowStockItem {
    
    private UUID id;
    private String name;
    private Integer currentStock;
    private Integer minStock;
    private String category;
    private String stockLevel; // CRITICAL, LOW, NORMAL, OUT_OF_STOCK
    
    // Constructors
    public LowStockItem() {}
    
    public LowStockItem(UUID id, String name, Integer currentStock, Integer minStock, String category, String stockLevel) {
        this.id = id;
        this.name = name;
        this.currentStock = currentStock;
        this.minStock = minStock;
        this.category = category;
        this.stockLevel = stockLevel;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }
    
    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getStockLevel() { return stockLevel; }
    public void setStockLevel(String stockLevel) { this.stockLevel = stockLevel; }
}
