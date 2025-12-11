package com.pos.inventsight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RecentOrdersDTO {
    
    private List<OrderInfo> orders;
    
    // Constructors
    public RecentOrdersDTO() {}
    
    public RecentOrdersDTO(List<OrderInfo> orders) {
        this.orders = orders;
    }
    
    // Getters and Setters
    public List<OrderInfo> getOrders() { return orders; }
    public void setOrders(List<OrderInfo> orders) { this.orders = orders; }
    
    // Inner class for order info
    public static class OrderInfo {
        private UUID id;
        private String orderNumber;
        private BigDecimal totalAmount;
        private String status;
        private String customerName;
        private Integer itemCount;
        private LocalDateTime createdAt;
        
        public OrderInfo() {}
        
        public OrderInfo(UUID id, String orderNumber, BigDecimal totalAmount, String status, 
                        String customerName, Integer itemCount, LocalDateTime createdAt) {
            this.id = id;
            this.orderNumber = orderNumber;
            this.totalAmount = totalAmount;
            this.status = status;
            this.customerName = customerName;
            this.itemCount = itemCount;
            this.createdAt = createdAt;
        }
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        
        public Integer getItemCount() { return itemCount; }
        public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
