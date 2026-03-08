package com.pos.inventsight.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for security.RoleConstants authority expressions used in @PreAuthorize annotations.
 */
class RoleConstantsTest {

    @Test
    void gmPlus_ContainsManager() {
        assertTrue(RoleConstants.GM_PLUS.contains("'MANAGER'"),
            "GM_PLUS must include 'MANAGER' so UserRole.MANAGER (mapped from GENERAL_MANAGER) is accepted");
        assertTrue(RoleConstants.GM_PLUS.contains("'manager'"),
            "GM_PLUS must include lowercase 'manager' for case-insensitive matching");
    }

    @Test
    void gmPlus_ContainsCoOwner() {
        assertTrue(RoleConstants.GM_PLUS.contains("'CO_OWNER'"),
            "GM_PLUS must include 'CO_OWNER' since UserRole.CO_OWNER is a GM+ level role");
        assertTrue(RoleConstants.GM_PLUS.contains("'co_owner'"),
            "GM_PLUS must include lowercase 'co_owner' for case-insensitive matching");
    }

    @Test
    void gmPlus_ContainsCoreRoles() {
        assertTrue(RoleConstants.GM_PLUS.contains("'OWNER'"));
        assertTrue(RoleConstants.GM_PLUS.contains("'FOUNDER'"));
        assertTrue(RoleConstants.GM_PLUS.contains("'CEO'"));
        assertTrue(RoleConstants.GM_PLUS.contains("'ADMIN'"));
    }

    @Test
    void smPlus_ContainsManager() {
        assertTrue(RoleConstants.SM_PLUS.contains("'MANAGER'"),
            "SM_PLUS must include 'MANAGER' so GENERAL_MANAGER-mapped authorities pass");
        assertTrue(RoleConstants.SM_PLUS.contains("'manager'"));
    }

    @Test
    void smPlus_ContainsStoreManager() {
        assertTrue(RoleConstants.SM_PLUS.contains("'STORE_MANAGER'"),
            "SM_PLUS must include 'STORE_MANAGER' for CompanyRole.STORE_MANAGER-mapped authority");
        assertTrue(RoleConstants.SM_PLUS.contains("'store_manager'"));
    }

    @Test
    void smPlus_ContainsCoOwner() {
        assertTrue(RoleConstants.SM_PLUS.contains("'CO_OWNER'"),
            "SM_PLUS must include 'CO_OWNER' since CO_OWNER is above SM level");
        assertTrue(RoleConstants.SM_PLUS.contains("'co_owner'"));
    }

    @Test
    void allRoles_ContainsManager() {
        assertTrue(RoleConstants.ALL_ROLES.contains("'MANAGER'"),
            "ALL_ROLES must include 'MANAGER' so authenticated MANAGER users can access basic endpoints");
    }

    @Test
    void allRoles_ContainsCoOwner() {
        assertTrue(RoleConstants.ALL_ROLES.contains("'CO_OWNER'"),
            "ALL_ROLES must include 'CO_OWNER'");
    }

    @Test
    void gmPlus_IsSubsetOfSmPlus() {
        // GM_PLUS should not grant access to endpoints meant only for SM+ but not GM+ —
        // verifying SM_PLUS is a superset of GM_PLUS by checking all GM+ roles appear in SM_PLUS.
        for (String role : new String[]{"'OWNER'", "'FOUNDER'", "'CO_OWNER'", "'CEO'",
                                        "'GENERAL_MANAGER'", "'MANAGER'", "'ADMIN'"}) {
            assertTrue(RoleConstants.SM_PLUS.contains(role),
                "SM_PLUS should contain all roles from GM_PLUS, missing: " + role);
        }
    }
}
