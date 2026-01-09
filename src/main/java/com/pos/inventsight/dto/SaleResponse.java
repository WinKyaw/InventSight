package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.PaymentMethod;
import com.pos.inventsight.model.sql.ReceiptType;
import com.pos.inventsight.model.sql.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SaleResponse {
    
    private Long id;
    private String receiptNumber;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private SaleStatus status;
    
    // Company info
    private UUID companyId;
    private String companyName;
    
    // Store info
    private UUID storeId;
    private String storeName;
    
    // User info
    private UUID processedById;
    private String processedByUsername;
    private String processedByFullName;
    
    private UUID fulfilledByUserId;
    private String fulfilledByUsername;
    private LocalDateTime fulfilledAt;
    
    // Customer info
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private BigDecimal customerDiscount; // From customer.discountPercentage
    
    private PaymentMethod paymentMethod;
    private ReceiptType receiptType;
    
    // Delivery (if applicable)
    private UUID deliveryPersonId;
    private String deliveryPersonName;
    private LocalDateTime deliveryAssignedAt;
    private LocalDateTime deliveredAt;
    private String deliveryNotes;
    
    private String notes;
    
    private List<SaleItemDTO> items;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Nested DTO for sale items
    public static class SaleItemDTO {
        private Long id;
        private UUID productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public String getProductSku() { return productSku; }
        public void setProductSku(String productSku) { this.productSku = productSku; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public SaleStatus getStatus() { return status; }
    public void setStatus(SaleStatus status) { this.status = status; }
    
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    
    public UUID getProcessedById() { return processedById; }
    public void setProcessedById(UUID processedById) { this.processedById = processedById; }
    
    public String getProcessedByUsername() { return processedByUsername; }
    public void setProcessedByUsername(String processedByUsername) { this.processedByUsername = processedByUsername; }
    
    public String getProcessedByFullName() { return processedByFullName; }
    public void setProcessedByFullName(String processedByFullName) { this.processedByFullName = processedByFullName; }
    
    public UUID getFulfilledByUserId() { return fulfilledByUserId; }
    public void setFulfilledByUserId(UUID fulfilledByUserId) { this.fulfilledByUserId = fulfilledByUserId; }
    
    public String getFulfilledByUsername() { return fulfilledByUsername; }
    public void setFulfilledByUsername(String fulfilledByUsername) { this.fulfilledByUsername = fulfilledByUsername; }
    
    public LocalDateTime getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(LocalDateTime fulfilledAt) { this.fulfilledAt = fulfilledAt; }
    
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public BigDecimal getCustomerDiscount() { return customerDiscount; }
    public void setCustomerDiscount(BigDecimal customerDiscount) { this.customerDiscount = customerDiscount; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public ReceiptType getReceiptType() { return receiptType; }
    public void setReceiptType(ReceiptType receiptType) { this.receiptType = receiptType; }
    
    public UUID getDeliveryPersonId() { return deliveryPersonId; }
    public void setDeliveryPersonId(UUID deliveryPersonId) { this.deliveryPersonId = deliveryPersonId; }
    
    public String getDeliveryPersonName() { return deliveryPersonName; }
    public void setDeliveryPersonName(String deliveryPersonName) { this.deliveryPersonName = deliveryPersonName; }
    
    public LocalDateTime getDeliveryAssignedAt() { return deliveryAssignedAt; }
    public void setDeliveryAssignedAt(LocalDateTime deliveryAssignedAt) { this.deliveryAssignedAt = deliveryAssignedAt; }
    
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    
    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public List<SaleItemDTO> getItems() { return items; }
    public void setItems(List<SaleItemDTO> items) { this.items = items; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
