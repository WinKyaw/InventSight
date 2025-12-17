package com.pos.inventsight.util;

import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUserRole;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Utility class for role-related operations
 */
public class RoleUtils {
    
    /**
     * Get highest priority role from list
     * Priority: FOUNDER > CEO > GENERAL_MANAGER > STORE_MANAGER > EMPLOYEE
     */
    public static CompanyStoreUserRole getHighestPriorityRole(List<CompanyStoreUserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        
        return roles.stream()
            .min((r1, r2) -> {
                int priority1 = getCompanyRolePriority(r1.getRole());
                int priority2 = getCompanyRolePriority(r2.getRole());
                return Integer.compare(priority1, priority2);
            })
            .orElse(roles.get(0));
    }
    
    /**
     * Get company role priority (lower number = higher priority)
     */
    public static int getCompanyRolePriority(CompanyRole role) {
        return switch (role) {
            case FOUNDER -> 1;
            case CEO -> 2;
            case GENERAL_MANAGER -> 3;
            case STORE_MANAGER -> 4;
            case EMPLOYEE -> 5;
            default -> 99;
        };
    }
    
    /**
     * Map CompanyRole to UserRole for compatibility
     * This ensures the frontend gets roles it understands
     */
    public static String mapCompanyRoleToUserRole(CompanyRole companyRole) {
        return switch (companyRole) {
            case FOUNDER, CEO -> "OWNER";
            case GENERAL_MANAGER -> "MANAGER";
            case STORE_MANAGER -> "MANAGER";
            case EMPLOYEE -> "EMPLOYEE";
            default -> "EMPLOYEE";
        };
    }
    
    /**
     * Check if a role has expired
     */
    public static boolean isExpired(CompanyStoreUserRole role) {
        if (role == null || role.getExpiresAt() == null) {
            return false; // Permanent roles never expire
        }
        return LocalDateTime.now().isAfter(role.getExpiresAt());
    }
}
