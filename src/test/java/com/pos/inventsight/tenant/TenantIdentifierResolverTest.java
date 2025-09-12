package com.pos.inventsight.tenant;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TenantIdentifierResolver
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.multiTenancy=SCHEMA"
})
class TenantIdentifierResolverTest {

    private final TenantIdentifierResolver resolver = new TenantIdentifierResolver();

    @Test
    void testResolveCurrentTenantIdentifier_WithTenantSet() {
        // Given a tenant is set in context
        TenantContext.setCurrentTenant("test_schema");
        
        try {
            // When resolving tenant identifier
            String result = resolver.resolveCurrentTenantIdentifier();
            
            // Then should return the set tenant
            assertEquals("test_schema", result);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void testResolveCurrentTenantIdentifier_WithoutTenantSet() {
        // Given no tenant is set (cleared context)
        TenantContext.clear();
        
        // When resolving tenant identifier
        String result = resolver.resolveCurrentTenantIdentifier();
        
        // Then should return default tenant
        assertEquals(TenantContext.DEFAULT_TENANT, result);
    }

    @Test
    void testValidateExistingCurrentSessions() {
        // For schema-based tenancy, this should always return true
        boolean result = resolver.validateExistingCurrentSessions();
        assertTrue(result);
    }
}