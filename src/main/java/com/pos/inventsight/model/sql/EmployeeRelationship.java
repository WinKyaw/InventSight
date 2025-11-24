package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing the relationship between an employee, employer (user who created the employee),
 * store, and company. This tracks the creation and management of employee records.
 * Simplified to use IDs only to avoid circular JSON serialization issues.
 */
@Entity
@Table(name = "employee_relationships")
public class EmployeeRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Just store IDs, not full entity relationships
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    
    @Column(name = "employer_id", nullable = false)
    private UUID employerId;
    
    @Column(name = "store_id", nullable = false)
    private UUID storeId;
    
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    // Constructors
    public EmployeeRelationship() {
        this.createdAt = LocalDateTime.now();
    }
    
    public EmployeeRelationship(Long employeeId, UUID employerId, UUID storeId, UUID companyId, String createdBy) {
        this.employeeId = employeeId;
        this.employerId = employerId;
        this.storeId = storeId;
        this.companyId = companyId;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    
    public UUID getEmployerId() { return employerId; }
    public void setEmployerId(UUID employerId) { this.employerId = employerId; }
    
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
