package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SalesOrder entity representing customer orders created by employees
 */
@Entity
@Table(name = "sales_orders")
public class SalesOrder {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;
    
    @NotNull(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @NotNull(message = "Order status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status = OrderStatus.DRAFT;
    
    @Column(name = "requires_manager_approval")
    private Boolean requiresManagerApproval = false;
    
    @NotBlank(message = "Currency code is required")
    @Size(max = 3, message = "Currency code must be 3 characters")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    
    @Size(max = 200, message = "Customer name must not exceed 200 characters")
    @Column(name = "customer_name", length = 200)
    private String customerName;
    
    @Size(max = 20, message = "Customer phone must not exceed 20 characters")
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;
    
    @Size(max = 100, message = "Customer email must not exceed 100 characters")
    @Column(name = "customer_email", length = 100)
    private String customerEmail;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Size(max = 100, message = "Updated by must not exceed 100 characters")
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SalesOrderItem> items = new ArrayList<>();
    
    // Constructors
    public SalesOrder() {
    }
    
    public SalesOrder(UUID tenantId, String currencyCode, String createdBy) {
        this.tenantId = tenantId;
        this.currencyCode = currencyCode;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Boolean getRequiresManagerApproval() {
        return requiresManagerApproval;
    }
    
    public void setRequiresManagerApproval(Boolean requiresManagerApproval) {
        this.requiresManagerApproval = requiresManagerApproval;
    }
    
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public List<SalesOrderItem> getItems() {
        return items;
    }
    
    public void setItems(List<SalesOrderItem> items) {
        this.items = items;
    }
    
    // Helper methods
    public void addItem(SalesOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    public void removeItem(SalesOrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
    
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
