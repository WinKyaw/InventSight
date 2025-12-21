package com.pos.inventsight.security;

/**
 * Centralized Role-Based Access Control (RBAC) constants
 * 
 * Usage:
 * @PreAuthorize(RoleConstants.ALL_ROLES)
 * @PreAuthorize(RoleConstants.GM_PLUS)
 * @PreAuthorize(RoleConstants.MANAGEMENT)
 */
public final class RoleConstants {
    
    // Prevent instantiation
    private RoleConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // ========== Individual Roles (Case-Insensitive) ==========
    
    public static final String OWNER = "OWNER";
    public static final String FOUNDER = "FOUNDER";
    public static final String CEO = "CEO";
    public static final String GENERAL_MANAGER = "GENERAL_MANAGER";
    public static final String STORE_MANAGER = "STORE_MANAGER";
    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String ADMIN = "ADMIN";
    
    // ========== Role Groups ==========
    
    /**
     * All roles - everyone can access
     * Used for: viewing data, reading information
     */
    public static final String ALL_ROLES = 
        "hasAnyAuthority(" +
        "'OWNER', 'owner', " +
        "'FOUNDER', 'founder', " +
        "'CEO', 'ceo', " +
        "'GENERAL_MANAGER', 'general_manager', " +
        "'STORE_MANAGER', 'store_manager', " +
        "'EMPLOYEE', 'employee', " +
        "'ADMIN', 'admin'" +
        ")";
    
    /**
     * Top management only (OWNER, FOUNDER, CEO)
     * Used for: critical business decisions, financial data
     */
    public static final String TOP_MANAGEMENT = 
        "hasAnyAuthority(" +
        "'OWNER', 'owner', " +
        "'FOUNDER', 'founder', " +
        "'CEO', 'ceo'" +
        ")";
    
    /**
     * General Manager and above (OWNER, FOUNDER, CEO, GENERAL_MANAGER)
     * Used for: warehouse management, inventory control, reporting
     */
    public static final String GM_PLUS = 
        "hasAnyAuthority(" +
        "'OWNER', 'owner', " +
        "'FOUNDER', 'founder', " +
        "'CEO', 'ceo', " +
        "'GENERAL_MANAGER', 'general_manager', " +
        "'ADMIN', 'admin'" +
        ")";
    
    /**
     * Store Manager and above (GM_PLUS + STORE_MANAGER)
     * Used for: store operations, employee management
     */
    public static final String SM_PLUS = 
        "hasAnyAuthority(" +
        "'OWNER', 'owner', " +
        "'FOUNDER', 'founder', " +
        "'CEO', 'ceo', " +
        "'GENERAL_MANAGER', 'general_manager', " +
        "'STORE_MANAGER', 'store_manager', " +
        "'ADMIN', 'admin'" +
        ")";
    
    /**
     * All management levels (GM_PLUS + STORE_MANAGER)
     * Used for: operational decisions
     */
    public static final String MANAGEMENT = SM_PLUS;
    
    /**
     * All staff including employees (everyone)
     * Used for: day-to-day operations, data entry
     */
    public static final String ALL_STAFF = ALL_ROLES;
    
    /**
     * Admin only
     * Used for: system configuration, technical operations
     */
    public static final String ADMIN_ONLY = 
        "hasAnyAuthority('ADMIN', 'admin')";
    
    // ========== Special Permissions ==========
    
    /**
     * Can manage warehouses (create, update, delete)
     * GM+ only
     */
    public static final String CAN_MANAGE_WAREHOUSES = GM_PLUS;
    
    /**
     * Can view warehouse inventory
     * All authenticated users
     */
    public static final String CAN_VIEW_INVENTORY = ALL_ROLES;
    
    /**
     * Can add/modify inventory
     * All staff can add inventory
     */
    public static final String CAN_MODIFY_INVENTORY = ALL_STAFF;
    
    /**
     * Can withdraw inventory
     * All staff can withdraw
     */
    public static final String CAN_WITHDRAW_INVENTORY = ALL_STAFF;
    
    /**
     * Can view financial reports
     * Management only
     */
    public static final String CAN_VIEW_REPORTS = MANAGEMENT;
    
    /**
     * Can manage employees (hire, fire, assign roles)
     * GM+ only
     */
    public static final String CAN_MANAGE_EMPLOYEES = GM_PLUS;
    
    /**
     * Can assign warehouses to employees
     * SM+ can assign
     */
    public static final String CAN_ASSIGN_WAREHOUSES = SM_PLUS;
}
