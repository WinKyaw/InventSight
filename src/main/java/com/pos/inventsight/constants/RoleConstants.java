package com.pos.inventsight.constants;

import com.pos.inventsight.model.sql.UserRole;
import java.util.Arrays;
import java.util.List;

/**
 * Constants for role-based access control.
 * Centralizes role checking logic to avoid duplication and ensure consistency.
 */
public class RoleConstants {
    
    /**
     * General Manager Plus roles - have access to team management, warehouse creation, etc.
     * These are leadership/management level roles.
     * 
     * GM+ Roles:
     * - OWNER: Company owner
     * - FOUNDER: Company founder (equivalent to OWNER)
     * - CO_OWNER: Co-owner with owner-level permissions
     * - MANAGER: General manager
     * - ADMIN: System administrator
     * 
     * Below GM:
     * - EMPLOYEE: Regular employee
     * - CASHIER: Cashier role
     * - CUSTOMER: Customer role
     * - MERCHANT: Merchant role
     * - PARTNER: Partner role
     * - USER: Legacy user role
     */
    public static final List<UserRole> GM_PLUS_ROLES = Arrays.asList(
        UserRole.OWNER,
        UserRole.FOUNDER,    // Founder is equivalent to Owner
        UserRole.CO_OWNER,
        UserRole.MANAGER,
        UserRole.ADMIN
    );
    
    /**
     * Check if a role is GM+ level (has team management and warehouse creation permissions).
     * 
     * @param role the user's role to check
     * @return true if role is GM+ level, false otherwise
     */
    public static boolean isGMPlus(UserRole role) {
        return role != null && GM_PLUS_ROLES.contains(role);
    }
    
    /**
     * Get Spring Security role strings for @PreAuthorize annotations.
     * This method returns the role expression that can be used in @PreAuthorize.
     * 
     * @return String expression for @PreAuthorize annotation
     */
    public static String getGMPlusRolesForPreAuthorize() {
        return "hasAnyRole('OWNER', 'FOUNDER', 'CO_OWNER', 'MANAGER', 'ADMIN')";
    }
}
