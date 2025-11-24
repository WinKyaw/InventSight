package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entity representing the relationship between an employee, employer (user who created the employee),
 * store, and company. This tracks the creation and management of employee records.
 */
@Entity
@Table(name = "employee_relationships")
public class EmployeeRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private User employer; // The user who created this employee
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "relationship_status")
    @Enumerated(EnumType.STRING)
    private RelationshipStatus relationshipStatus = RelationshipStatus.ACTIVE;
    
    // Constructors
    public EmployeeRelationship() {}
    
    public EmployeeRelationship(Employee employee, User employer, Store store, Company company) {
        this.employee = employee;
        this.employer = employer;
        this.store = store;
        this.company = company;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.relationshipStatus = RelationshipStatus.ACTIVE;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Employee getEmployee() {
        return employee;
    }
    
    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
    
    public User getEmployer() {
        return employer;
    }
    
    public void setEmployer(User employer) {
        this.employer = employer;
    }
    
    public Store getStore() {
        return store;
    }
    
    public void setStore(Store store) {
        this.store = store;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public RelationshipStatus getRelationshipStatus() {
        return relationshipStatus;
    }
    
    public void setRelationshipStatus(RelationshipStatus relationshipStatus) {
        this.relationshipStatus = relationshipStatus;
    }
    
    // Enum for relationship status
    public enum RelationshipStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        TERMINATED
    }
}
