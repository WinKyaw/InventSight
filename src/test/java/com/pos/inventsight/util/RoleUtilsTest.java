package com.pos.inventsight.util;

import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleUtils Unit Tests")
class RoleUtilsTest {

    // -----------------------------------------------------------------------
    // mapCompanyRoleToUserRole
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("FOUNDER CompanyRole maps to 'FOUNDER'")
    void founderCompanyRoleMapsToFounder() {
        assertEquals("FOUNDER", RoleUtils.mapCompanyRoleToUserRole(CompanyRole.FOUNDER));
    }

    @Test
    @DisplayName("CEO CompanyRole maps to 'OWNER'")
    void ceoCompanyRoleMapsToOwner() {
        assertEquals("OWNER", RoleUtils.mapCompanyRoleToUserRole(CompanyRole.CEO));
    }

    @Test
    @DisplayName("GENERAL_MANAGER CompanyRole maps to 'MANAGER'")
    void generalManagerCompanyRoleMapsToManager() {
        assertEquals("MANAGER", RoleUtils.mapCompanyRoleToUserRole(CompanyRole.GENERAL_MANAGER));
    }

    @Test
    @DisplayName("STORE_MANAGER CompanyRole maps to 'MANAGER'")
    void storeManagerCompanyRoleMapsToManager() {
        assertEquals("MANAGER", RoleUtils.mapCompanyRoleToUserRole(CompanyRole.STORE_MANAGER));
    }

    @Test
    @DisplayName("EMPLOYEE CompanyRole maps to 'EMPLOYEE'")
    void employeeCompanyRoleMapsToEmployee() {
        assertEquals("EMPLOYEE", RoleUtils.mapCompanyRoleToUserRole(CompanyRole.EMPLOYEE));
    }

    // -----------------------------------------------------------------------
    // isGMPlusRole
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("OWNER UserRole is GM+")
    void ownerIsGMPlus() {
        assertTrue(RoleUtils.isGMPlusRole(UserRole.OWNER));
    }

    @Test
    @DisplayName("FOUNDER UserRole is GM+")
    void founderIsGMPlus() {
        assertTrue(RoleUtils.isGMPlusRole(UserRole.FOUNDER));
    }

    @Test
    @DisplayName("CO_OWNER UserRole is GM+")
    void coOwnerIsGMPlus() {
        assertTrue(RoleUtils.isGMPlusRole(UserRole.CO_OWNER));
    }

    @Test
    @DisplayName("MANAGER UserRole is GM+")
    void managerIsGMPlus() {
        assertTrue(RoleUtils.isGMPlusRole(UserRole.MANAGER));
    }

    @Test
    @DisplayName("ADMIN UserRole is GM+")
    void adminIsGMPlus() {
        assertTrue(RoleUtils.isGMPlusRole(UserRole.ADMIN));
    }

    @Test
    @DisplayName("EMPLOYEE UserRole is not GM+")
    void employeeIsNotGMPlus() {
        assertFalse(RoleUtils.isGMPlusRole(UserRole.EMPLOYEE));
    }

    @Test
    @DisplayName("CUSTOMER UserRole is not GM+")
    void customerIsNotGMPlus() {
        assertFalse(RoleUtils.isGMPlusRole(UserRole.CUSTOMER));
    }

    @Test
    @DisplayName("CASHIER UserRole is not GM+")
    void cashierIsNotGMPlus() {
        assertFalse(RoleUtils.isGMPlusRole(UserRole.CASHIER));
    }

    @Test
    @DisplayName("null UserRole is not GM+")
    void nullIsNotGMPlus() {
        assertFalse(RoleUtils.isGMPlusRole(null));
    }
}
