package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid;
    
    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String username;
    
    @NotBlank
    @Size(max = 100)
    @Email
    @Column(unique = true)
    private String email;
    
    @NotBlank
    @Size(max = 120)
    private String password;
    
    @NotBlank
    @Size(max = 100)
    private String firstName;
    
    @NotBlank
    @Size(max = 100)
    private String lastName;
    
    @Size(max = 20)
    private String phone;
    
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_level")
    private SubscriptionLevel subscriptionLevel = SubscriptionLevel.FREE;
    
    private Boolean isActive = true;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "created_by")
    private String createdBy;
    
    // Tenant ID - using user's UUID as tenant identifier for schema-based isolation
    // Note: Maps to native PostgreSQL UUID type, not varchar. JPA handles conversion automatically.
    @Column(name = "tenant_id")
    private UUID tenantId;
    
    // Default tenant/company ID for automatic tenant binding at login
    // Set during registration or invite acceptance for seamless multi-tenant access
    @Column(name = "default_tenant_id")
    private UUID defaultTenantId;
    
    // Constructors
    public User() {
        this.uuid = UUID.randomUUID();
        this.tenantId = this.uuid; // Use UUID as tenant ID
    }
    
    public User(String username, String email, String password, String firstName, String lastName) {
        this(); // Call default constructor to initialize UUID and tenantId
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return isActive;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { 
        this.uuid = uuid; 
        // Update tenant ID when UUID changes
        this.tenantId = uuid;
    }
    
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    
    public UUID getDefaultTenantId() { return defaultTenantId; }
    public void setDefaultTenantId(UUID defaultTenantId) { this.defaultTenantId = defaultTenantId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public SubscriptionLevel getSubscriptionLevel() { return subscriptionLevel; }
    public void setSubscriptionLevel(SubscriptionLevel subscriptionLevel) { this.subscriptionLevel = subscriptionLevel; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    // Utility methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Ensures UUID is set for existing users that might not have one
     * Note: This handles the transition from String-based UUIDs to native UUID objects
     */
    @PrePersist
    @PreUpdate
    public void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
        if (this.tenantId == null) {
            this.tenantId = this.uuid;
        }
    }
}