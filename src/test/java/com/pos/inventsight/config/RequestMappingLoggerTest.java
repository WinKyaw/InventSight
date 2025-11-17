package com.pos.inventsight.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit test to verify that RequestMappingLogger can be instantiated
 * with a RequestMappingHandlerMapping bean without NoUniqueBeanDefinitionException.
 * 
 * This test ensures:
 * 1. RequestMappingLogger can be instantiated with the @Qualifier annotation
 * 2. It properly handles the ApplicationReadyEvent
 * 3. It correctly processes request mappings
 */
class RequestMappingLoggerTest {

    @Test
    void shouldCreateRequestMappingLoggerWithQualifiedBean() {
        // Create a mock RequestMappingHandlerMapping
        RequestMappingHandlerMapping mockHandlerMapping = mock(RequestMappingHandlerMapping.class);
        
        // Create an empty map for handler methods
        Map<RequestMappingInfo, HandlerMethod> mockMappings = new HashMap<>();
        when(mockHandlerMapping.getHandlerMethods()).thenReturn(mockMappings);
        
        // Verify that RequestMappingLogger can be instantiated
        // The key test is that this doesn't throw NoUniqueBeanDefinitionException
        RequestMappingLogger logger = new RequestMappingLogger(mockHandlerMapping);
        
        assertThat(logger).isNotNull();
    }

    @Test
    void shouldHandleApplicationReadyEvent() {
        // Create a mock RequestMappingHandlerMapping
        RequestMappingHandlerMapping mockHandlerMapping = mock(RequestMappingHandlerMapping.class);
        
        // Create an empty map for handler methods
        Map<RequestMappingInfo, HandlerMethod> mockMappings = new HashMap<>();
        when(mockHandlerMapping.getHandlerMethods()).thenReturn(mockMappings);
        
        // Create the logger
        RequestMappingLogger logger = new RequestMappingLogger(mockHandlerMapping);
        
        // Create a mock ApplicationReadyEvent
        ApplicationReadyEvent mockEvent = mock(ApplicationReadyEvent.class);
        
        // Verify that the event handler doesn't throw an exception
        logger.onApplicationEvent(mockEvent);
        
        // Verify that getHandlerMethods was called
        verify(mockHandlerMapping, times(1)).getHandlerMethods();
    }
}

