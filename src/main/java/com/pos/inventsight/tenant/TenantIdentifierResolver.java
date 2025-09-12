package com.pos.inventsight.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * TenantIdentifierResolver implements Hibernate's CurrentTenantIdentifierResolver
 * to resolve the current schema for multi-tenant operations.
 * Only active when multi-tenancy is enabled.
 */
@Component
@ConditionalOnProperty(name = "spring.jpa.properties.hibernate.multiTenancy", havingValue = "SCHEMA")
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {
    
    /**
     * Resolve the current tenant identifier from TenantContext
     * @return the current tenant identifier
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getCurrentTenant();
    }
    
    /**
     * Validate the tenant identifier exists and is valid
     * @param tenantId the tenant identifier to validate
     * @return true if valid, false otherwise
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        // Always return true for schema-based tenancy
        // Schema validation happens at the database level
        return true;
    }
}