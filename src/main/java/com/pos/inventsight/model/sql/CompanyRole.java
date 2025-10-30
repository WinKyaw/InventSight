package com.pos.inventsight.model.sql;

/**
 * Company role enum for company-centric multi-tenant architecture
 */
public enum CompanyRole {
    CEO("Chief Executive Officer"),
    FOUNDER("Company Founder"),
    GENERAL_MANAGER("General Manager"),
    STORE_MANAGER("Store Manager"), 
    EMPLOYEE("Employee");
    
    private final String displayName;
    
    CompanyRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this role has owner-level privileges
     */
    public boolean isOwnerLevel() {
        return this == CEO || this == FOUNDER;
    }
    
    /**
     * Check if this role has manager-level privileges 
     */
    public boolean isManagerLevel() {
        return this == CEO || this == FOUNDER || this == GENERAL_MANAGER || this == STORE_MANAGER;
    }
    
    /**
     * Check if this role can manage stores
     */
    public boolean canManageStores() {
        return this == CEO || this == FOUNDER || this == GENERAL_MANAGER;
    }
    
    /**
     * Check if this role can manage users
     */
    public boolean canManageUsers() {
        return this == CEO || this == FOUNDER || this == GENERAL_MANAGER;
    }
    
    /**
     * Check if this role can manage warehouses
     */
    public boolean canManageWarehouses() {
        return this == CEO || this == FOUNDER || this == GENERAL_MANAGER;
    }
    
    /**
     * Check if this role can manage product pricing
     * (CEO, FOUNDER, and GENERAL_MANAGER only)
     */
    public boolean canManagePricing() {
        return this == CEO || this == FOUNDER || this == GENERAL_MANAGER;
    }
}