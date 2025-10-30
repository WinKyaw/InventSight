package com.pos.inventsight.model.sql;

/**
 * Company role enum for company-centric multi-tenant architecture
 */
public enum CompanyRole {
    FOUNDER("Company Founder"),
    CEO("Chief Executive Officer"),
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
     * Owner-level roles: FOUNDER, CEO
     */
    public boolean isOwnerLevel() {
        return this == FOUNDER || this == CEO;
    }
    
    /**
     * Check if this role has manager-level privileges 
     * Manager-level roles: FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER
     */
    public boolean isManagerLevel() {
        return this == FOUNDER || this == CEO || this == GENERAL_MANAGER || this == STORE_MANAGER;
    }
    
    /**
     * Check if this role can manage stores
     * CEO has same privileges as FOUNDER
     */
    public boolean canManageStores() {
        return this == FOUNDER || this == CEO || this == GENERAL_MANAGER;
    }
    
    /**
     * Check if this role can manage users
     * CEO has same privileges as FOUNDER
     */
    public boolean canManageUsers() {
        return this == FOUNDER || this == CEO || this == GENERAL_MANAGER;
    }
    
    /**
     * Check if this role can manage warehouses
     * CEO has same privileges as FOUNDER
     */
    public boolean canManageWarehouses() {
        return this == FOUNDER || this == CEO || this == GENERAL_MANAGER;
    }
}