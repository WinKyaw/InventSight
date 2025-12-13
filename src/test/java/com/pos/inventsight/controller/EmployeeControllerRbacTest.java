package com.pos.inventsight.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify RBAC (Role-Based Access Control) fixes on EmployeeController
 * This test verifies that the correct @PreAuthorize annotations are present on the endpoints
 */
public class EmployeeControllerRbacTest {

    /**
     * Test that GET /employees has @PreAuthorize annotation with correct roles
     */
    @Test
    public void testGetAllEmployees_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = EmployeeController.class.getMethod("getAllEmployees");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "GET /employees should have @PreAuthorize annotation");
        assertEquals("hasAnyRole('MANAGER', 'OWNER', 'ADMIN')", preAuthorize.value(),
            "GET /employees should require MANAGER, OWNER, or ADMIN role");
    }

    /**
     * Test that GET /employees/{id} has @PreAuthorize annotation with correct roles
     */
    @Test
    public void testGetEmployeeById_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = EmployeeController.class.getMethod("getEmployeeById", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "GET /employees/{id} should have @PreAuthorize annotation");
        assertEquals("hasAnyRole('MANAGER', 'OWNER', 'ADMIN')", preAuthorize.value(),
            "GET /employees/{id} should require MANAGER, OWNER, or ADMIN role");
    }

    /**
     * Test that GET /employees/statistics has @PreAuthorize annotation with correct roles
     */
    @Test
    public void testGetEmployeeStatistics_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = EmployeeController.class.getMethod("getEmployeeStatistics");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "GET /employees/statistics should have @PreAuthorize annotation");
        assertEquals("hasAnyRole('MANAGER', 'OWNER', 'ADMIN')", preAuthorize.value(),
            "GET /employees/statistics should require MANAGER, OWNER, or ADMIN role");
    }

    /**
     * Test that PUT /employees/{id} has @PreAuthorize annotation with correct roles
     */
    @Test
    public void testUpdateEmployee_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = EmployeeController.class.getMethod("updateEmployee", UUID.class, 
            com.pos.inventsight.dto.EmployeeRequest.class, org.springframework.security.core.Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "PUT /employees/{id} should have @PreAuthorize annotation");
        assertEquals("hasAnyRole('MANAGER', 'OWNER', 'ADMIN')", preAuthorize.value(),
            "PUT /employees/{id} should require MANAGER, OWNER, or ADMIN role");
    }

    /**
     * Test that PUT /employees/{id}/role has @PreAuthorize annotation with correct roles
     * This is the main fix - changed from hasRole('ADMIN') to hasAnyRole('MANAGER', 'OWNER', 'ADMIN')
     */
    @Test
    public void testUpdateEmployeeRole_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = EmployeeController.class.getMethod("updateEmployeeRole", UUID.class, 
            java.util.Map.class, org.springframework.security.core.Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "PUT /employees/{id}/role should have @PreAuthorize annotation");
        assertEquals("hasAnyRole('MANAGER', 'OWNER', 'ADMIN')", preAuthorize.value(),
            "PUT /employees/{id}/role should now allow MANAGER, OWNER, and ADMIN roles (not just ADMIN)");
    }
}
