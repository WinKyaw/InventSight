package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for User authorities to verify correct format for Spring Security @PreAuthorize
 * This ensures User.getAuthorities() returns role names WITHOUT "ROLE_" prefix
 * to match @PreAuthorize annotations like hasAnyAuthority('OWNER', 'FOUNDER', etc.)
 */
public class UserAuthoritiesTest {

    @Test
    public void testOwnerAuthorityWithoutRolePrefix() {
        // Given
        User user = new User();
        user.setRole(UserRole.OWNER);
        
        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        // Then
        assertNotNull(authorities, "Authorities should not be null");
        assertEquals(1, authorities.size(), "Should have exactly one authority");
        
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("OWNER", authority.getAuthority(), 
            "Authority should be 'OWNER' without ROLE_ prefix to match @PreAuthorize");
    }
    
    @Test
    public void testFounderAuthorityWithoutRolePrefix() {
        // Given
        User user = new User();
        user.setRole(UserRole.FOUNDER);
        
        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        // Then
        assertNotNull(authorities, "Authorities should not be null");
        assertEquals(1, authorities.size(), "Should have exactly one authority");
        
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("FOUNDER", authority.getAuthority(), 
            "Authority should be 'FOUNDER' without ROLE_ prefix");
    }
    
    @Test
    public void testManagerAuthorityWithoutRolePrefix() {
        // Given
        User user = new User();
        user.setRole(UserRole.MANAGER);
        
        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        // Then
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("MANAGER", authority.getAuthority(), 
            "Authority should be 'MANAGER' without ROLE_ prefix");
    }
    
    @Test
    public void testEmployeeAuthorityWithoutRolePrefix() {
        // Given
        User user = new User();
        user.setRole(UserRole.EMPLOYEE);
        
        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        // Then
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("EMPLOYEE", authority.getAuthority(), 
            "Authority should be 'EMPLOYEE' without ROLE_ prefix");
    }
    
    @Test
    public void testAdminAuthorityWithoutRolePrefix() {
        // Given
        User user = new User();
        user.setRole(UserRole.ADMIN);
        
        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        // Then
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("ADMIN", authority.getAuthority(), 
            "Authority should be 'ADMIN' without ROLE_ prefix");
    }
    
    @Test
    public void testCashierAuthorityWithoutRolePrefix() {
        // Given
        User user = new User();
        user.setRole(UserRole.CASHIER);
        
        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        // Then
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("CASHIER", authority.getAuthority(), 
            "Authority should be 'CASHIER' without ROLE_ prefix");
    }
    
    @Test
    public void testUserRoleDefaultAuthority() {
        // Given
        User user = new User();
        // role defaults to USER
        
        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        // Then
        assertNotNull(authorities, "Authorities should not be null");
        assertEquals(1, authorities.size(), "Should have exactly one authority");
        
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("USER", authority.getAuthority(), 
            "Default authority should be 'USER' without ROLE_ prefix");
    }
    
    /**
     * This test verifies that authorities match the format expected by
     * @PreAuthorize("hasAnyAuthority('OWNER', 'FOUNDER', etc.)")
     * as used in various controllers
     */
    @Test
    public void testAuthorityMatchesPreAuthorizeFormat() {
        // Given - User with OWNER role
        User user = new User();
        user.setRole(UserRole.OWNER);
        
        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        GrantedAuthority authority = authorities.iterator().next();
        
        // Then - Authority should match the format used in @PreAuthorize
        // @PreAuthorize("hasAnyAuthority('OWNER', 'FOUNDER', 'ADMIN', etc.)")
        String[] expectedAuthorities = {"OWNER", "FOUNDER", "CO_OWNER", "MANAGER", "ADMIN"};
        
        boolean matchesExpectedFormat = false;
        for (String expected : expectedAuthorities) {
            if (expected.equals(authority.getAuthority())) {
                matchesExpectedFormat = true;
                break;
            }
        }
        
        assertTrue(matchesExpectedFormat, 
            "Authority should match @PreAuthorize format without ROLE_ prefix");
        
        // Verify it does NOT have ROLE_ prefix
        assertFalse(authority.getAuthority().startsWith("ROLE_"), 
            "Authority should NOT have ROLE_ prefix");
    }
}
