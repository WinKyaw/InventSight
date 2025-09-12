package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discount_audit_log")
public class DiscountAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "attempted_price", precision = 10, scale = 2)
    private BigDecimal attemptedPrice;
    
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private DiscountResult result;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(name = "session_id")
    private String sessionId;
    
    // Constructors
    public DiscountAuditLog() {}
    
    public DiscountAuditLog(User user, UserRole role, Store store, Product product, 
                           BigDecimal attemptedPrice, BigDecimal originalPrice, 
                           DiscountResult result) {
        this.user = user;
        this.role = role;
        this.store = store;
        this.product = product;
        this.attemptedPrice = attemptedPrice;
        this.originalPrice = originalPrice;
        this.result = result;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public BigDecimal getAttemptedPrice() { return attemptedPrice; }
    public void setAttemptedPrice(BigDecimal attemptedPrice) { this.attemptedPrice = attemptedPrice; }
    
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    
    public DiscountResult getResult() { return result; }
    public void setResult(DiscountResult result) { this.result = result; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    // Business methods
    public BigDecimal getDiscountAmount() {
        if (originalPrice == null || attemptedPrice == null) return BigDecimal.ZERO;
        return originalPrice.subtract(attemptedPrice);
    }
    
    public BigDecimal getDiscountPercentage() {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getDiscountAmount().divide(originalPrice, 4, BigDecimal.ROUND_HALF_UP)
               .multiply(new BigDecimal(100));
    }
    
    public boolean isApproved() {
        return result == DiscountResult.APPROVED;
    }
}