package com.pos.inventsight.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify the warehouse available products endpoint exists
 * This test verifies that the GET /warehouses/{warehouseId}/available-products endpoint
 * is correctly configured in the WarehouseController.
 */
public class WarehouseAvailableProductsEndpointTest {

    /**
     * Test that GET /warehouses/{warehouseId}/available-products endpoint exists
     */
    @Test
    public void testGetWarehouseAvailableProducts_EndpointExists() throws NoSuchMethodException {
        Method method = WarehouseController.class.getMethod("getWarehouseAvailableProducts",
                UUID.class, Authentication.class);
        
        assertNotNull(method, "getWarehouseAvailableProducts method should exist");
        
        // Verify the method has @GetMapping annotation
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "GET endpoint should have @GetMapping annotation");
        
        // Verify the mapping path
        String[] paths = getMapping.value();
        assertEquals(1, paths.length, "Should have exactly one path mapping");
        assertEquals("/{warehouseId}/available-products", paths[0], 
            "Path should be /{warehouseId}/available-products");
    }

    /**
     * Test that the method has correct return type
     */
    @Test
    public void testGetWarehouseAvailableProducts_HasCorrectReturnType() throws NoSuchMethodException {
        Method method = WarehouseController.class.getMethod("getWarehouseAvailableProducts",
                UUID.class, Authentication.class);
        
        // Verify return type is ResponseEntity
        assertEquals("org.springframework.http.ResponseEntity", 
                     method.getReturnType().getName(),
                     "Method should return ResponseEntity");
    }

    /**
     * Test that the method accepts correct parameters
     */
    @Test
    public void testGetWarehouseAvailableProducts_HasCorrectParameters() throws NoSuchMethodException {
        Method method = WarehouseController.class.getMethod("getWarehouseAvailableProducts",
                UUID.class, Authentication.class);
        
        Class<?>[] paramTypes = method.getParameterTypes();
        assertEquals(2, paramTypes.length, "Method should accept 2 parameters");
        assertEquals(UUID.class, paramTypes[0], "First parameter should be UUID");
        assertEquals(Authentication.class, paramTypes[1], "Second parameter should be Authentication");
    }

    /**
     * Test that the endpoint requires authentication
     */
    @Test
    public void testGetWarehouseAvailableProducts_RequiresAuthentication() throws NoSuchMethodException {
        Method method = WarehouseController.class.getMethod("getWarehouseAvailableProducts",
                UUID.class, Authentication.class);
        
        org.springframework.security.access.prepost.PreAuthorize preAuthorize = 
            method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        
        assertNotNull(preAuthorize, "Endpoint should have @PreAuthorize annotation");
        assertEquals("isAuthenticated()", preAuthorize.value(), 
            "Endpoint should require authentication");
    }
}
