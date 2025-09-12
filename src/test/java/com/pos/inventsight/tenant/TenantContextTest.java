package com.pos.inventsight.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TenantContext functionality
 */
class TenantContextTest {

    @BeforeEach
    void setUp() {
        // Clear any existing context before each test
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        TenantContext.clear();
    }

    @Test
    void testDefaultTenant() {
        // When no tenant is set, should return default
        String currentTenant = TenantContext.getCurrentTenant();
        assertEquals(TenantContext.DEFAULT_TENANT, currentTenant);
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testSetAndGetTenant() {
        // Test setting and getting a tenant
        String testTenant = "test_tenant";
        TenantContext.setCurrentTenant(testTenant);
        
        assertEquals(testTenant, TenantContext.getCurrentTenant());
        assertTrue(TenantContext.isSet());
    }

    @Test
    void testClearTenant() {
        // Test clearing tenant context
        String testTenant = "test_tenant";
        TenantContext.setCurrentTenant(testTenant);
        assertTrue(TenantContext.isSet());
        
        TenantContext.clear();
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        // Test that tenant context is isolated per thread
        String mainThreadTenant = "main_tenant";
        TenantContext.setCurrentTenant(mainThreadTenant);
        
        final String[] otherThreadTenant = new String[1];
        final boolean[] otherThreadIsSet = new boolean[1];
        
        Thread otherThread = new Thread(() -> {
            // This thread should see the default tenant, not the main thread's tenant
            otherThreadTenant[0] = TenantContext.getCurrentTenant();
            otherThreadIsSet[0] = TenantContext.isSet();
            
            // Set a different tenant in this thread
            TenantContext.setCurrentTenant("other_thread_tenant");
        });
        
        otherThread.start();
        otherThread.join();
        
        // Other thread should have seen default tenant initially
        assertEquals(TenantContext.DEFAULT_TENANT, otherThreadTenant[0]);
        assertFalse(otherThreadIsSet[0]);
        
        // Main thread should still have its tenant
        assertEquals(mainThreadTenant, TenantContext.getCurrentTenant());
        assertTrue(TenantContext.isSet());
    }

    @Test
    void testNullTenantHandling() {
        // Test that null tenant is handled gracefully
        TenantContext.setCurrentTenant(null);
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testEmptyTenantHandling() {
        // Test that empty tenant is handled gracefully
        TenantContext.setCurrentTenant("");
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }
}