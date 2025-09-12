package com.pos.inventsight.tenant;

/**
 * TenantContext holds the current tenant (schema) per request using ThreadLocal.
 * This class provides thread-safe access to the current tenant identifier.
 */
public class TenantContext {
    
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    /**
     * Default schema/tenant for backwards compatibility
     */
    public static final String DEFAULT_TENANT = "public";
    
    /**
     * Set the current tenant for the thread
     * @param tenantId the tenant identifier (schema name)
     */
    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    /**
     * Get the current tenant for the thread
     * @return the current tenant identifier, or DEFAULT_TENANT if none set
     */
    public static String getCurrentTenant() {
        String tenant = currentTenant.get();
        return (tenant != null && !tenant.trim().isEmpty()) ? tenant : DEFAULT_TENANT;
    }
    
    /**
     * Clear the current tenant for the thread
     */
    public static void clear() {
        currentTenant.remove();
    }
    
    /**
     * Check if a tenant is currently set
     * @return true if a tenant is set, false otherwise
     */
    public static boolean isSet() {
        String tenant = currentTenant.get();
        return tenant != null && !tenant.trim().isEmpty();
    }
}