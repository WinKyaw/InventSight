package com.pos.inventsight.controller;

import com.pos.inventsight.service.StoreInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Test class to verify StoreInventoryController is properly annotated and can be initialized
 */
public class StoreInventoryControllerRegistrationTest {

    private StoreInventoryController controller;
    private StoreInventoryService storeInventoryService;

    @BeforeEach
    void setUp() {
        controller = new StoreInventoryController();
        storeInventoryService = mock(StoreInventoryService.class);
        
        // Inject mock using reflection
        ReflectionTestUtils.setField(controller, "storeInventoryService", storeInventoryService);
    }

    /**
     * Test that StoreInventoryController has @RestController annotation
     */
    @Test
    void testControllerHasRestControllerAnnotation() {
        assertTrue(controller.getClass().isAnnotationPresent(RestController.class),
                "StoreInventoryController should be annotated with @RestController");
    }

    /**
     * Test that StoreInventoryController has @RequestMapping annotation with correct path
     */
    @Test
    void testControllerHasRequestMappingAnnotation() {
        assertTrue(controller.getClass().isAnnotationPresent(RequestMapping.class),
                "StoreInventoryController should be annotated with @RequestMapping");
        
        RequestMapping requestMapping = controller.getClass().getAnnotation(RequestMapping.class);
        String[] paths = requestMapping.value();
        
        assertEquals(1, paths.length, "RequestMapping should have exactly one path");
        assertEquals("/api/store-inventory", paths[0],
                "RequestMapping path should be '/api/store-inventory'");
    }

    /**
     * Test that controller can be instantiated
     */
    @Test
    void testControllerCanBeInstantiated() {
        assertNotNull(controller, "StoreInventoryController should be instantiable");
    }

    /**
     * Test that controller has the required service dependency
     */
    @Test
    void testControllerHasStoreInventoryService() {
        Object service = ReflectionTestUtils.getField(controller, "storeInventoryService");
        assertNotNull(service, "StoreInventoryController should have storeInventoryService field");
    }

    /**
     * Test that the init() method exists and can be called without errors
     */
    @Test
    void testInitMethodExists() {
        assertDoesNotThrow(() -> {
            controller.init();
        }, "Controller init() method should execute without throwing exceptions");
    }
}
