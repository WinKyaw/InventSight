package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.PaymentMethod;
import com.pos.inventsight.model.sql.ReceiptType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class SaleRequest {
    @NotNull(message = "Items are required")
    @NotEmpty(message = "At least one item is required")
    private List<ItemRequest> items;
    
    private UUID customerId; // NEW: Link to customer
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    private ReceiptType receiptType; // NEW: Receipt type
    
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String notes;
    
    // NEW: Delivery info (if receiptType = DELIVERY)
    private UUID deliveryPersonId;
    private String deliveryNotes;
    
    // Nested class for item requests
    public static class ItemRequest {
        @NotNull(message = "Product ID is required")
        private UUID productId;
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
        
        // Getters and Setters
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
    
    // Getters and Setters
    public List<ItemRequest> getItems() { return items; }
    public void setItems(List<ItemRequest> items) { this.items = items; }
    
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public ReceiptType getReceiptType() { return receiptType; }
    public void setReceiptType(ReceiptType receiptType) { this.receiptType = receiptType; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public UUID getDeliveryPersonId() { return deliveryPersonId; }
    public void setDeliveryPersonId(UUID deliveryPersonId) { this.deliveryPersonId = deliveryPersonId; }
    
    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }
}