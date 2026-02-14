package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReceiptResponse {
    
    private UUID id;
    private String receiptNumber;
    private ReceiptType receiptType;
    private ReceiptStatus status;
    
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal paidAmount;
    private LocalDateTime paymentDate;
    
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    private DeliveryType deliveryType;
    private String deliveryNotes;
    private LocalDateTime deliveredAt;
    
    private LocalDateTime fulfilledAt;
    private String fulfilledByName;
    
    private LocalDateTime createdAt;
    private String createdByName;
    
    private String notes;
    private List<ReceiptItemResponse> items;
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    
    public ReceiptType getReceiptType() { return receiptType; }
    public void setReceiptType(ReceiptType receiptType) { this.receiptType = receiptType; }
    
    public ReceiptStatus getStatus() { return status; }
    public void setStatus(ReceiptStatus status) { this.status = status; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public DeliveryType getDeliveryType() { return deliveryType; }
    public void setDeliveryType(DeliveryType deliveryType) { this.deliveryType = deliveryType; }
    
    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }
    
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    
    public LocalDateTime getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(LocalDateTime fulfilledAt) { this.fulfilledAt = fulfilledAt; }
    
    public String getFulfilledByName() { return fulfilledByName; }
    public void setFulfilledByName(String fulfilledByName) { this.fulfilledByName = fulfilledByName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public List<ReceiptItemResponse> getItems() { return items; }
    public void setItems(List<ReceiptItemResponse> items) { this.items = items; }
    
    public static class ReceiptItemResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
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
}
