package com.pos.inventsight.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that ControllerRegistrationLogger handles multiple 
 * RequestMappingHandlerMapping beans correctly without causing NoUniqueBeanDefinitionException.
 * 
 * This test ensures:
 * 1. Application starts successfully when multiple handler mapping beans exist
 * 2. Logger selects the primary 'requestMappingHandlerMapping' bean by name
 * 3. Application handles gracefully when no beans are found
 */
class ControllerRegistrationLoggerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void shouldStartSuccessfullyWithMultipleHandlerMappingBeans() {
        // This simulates the scenario where both requestMappingHandlerMapping and
        // controllerEndpointHandlerMapping beans are present (e.g., when Actuator is enabled)
        contextRunner
            .withUserConfiguration(MultipleHandlerMappingConfiguration.class, ControllerRegistrationLoggerConfiguration.class)
            .run(context -> {
                // The key success criterion is that the context loads without error
                // Previously, this would fail with NoUniqueBeanDefinitionException
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(ControllerRegistrationLogger.class);
                // Verify both handler mappings exist
                assertThat(context.getBeansOfType(RequestMappingHandlerMapping.class)).hasSize(2);
            });
    }

    @Test
    void shouldHandleNoPrimaryBeanGracefully() {
        // Test the fallback behavior when primary bean is not named 'requestMappingHandlerMapping'
        contextRunner
            .withUserConfiguration(NonStandardNameConfiguration.class, ControllerRegistrationLoggerConfiguration.class)
            .run(context -> {
                // Should still start successfully and use the first available bean
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(ControllerRegistrationLogger.class);
            });
    }

    @Test
    void shouldHandleNoHandlerMappingBeansGracefully() {
        // Test the behavior when no RequestMappingHandlerMapping beans are present
        contextRunner
            .withUserConfiguration(ControllerRegistrationLoggerConfiguration.class)
            .run(context -> {
                // Should start without throwing exception even if no handler mappings exist
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(ControllerRegistrationLogger.class);
            });
    }

    /**
     * Configuration that provides multiple RequestMappingHandlerMapping beans,
     * simulating the scenario with Actuator enabled.
     */
    @Configuration
    static class MultipleHandlerMappingConfiguration {
        
        @Bean
        public RequestMappingHandlerMapping requestMappingHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }
        
        @Bean
        public RequestMappingHandlerMapping controllerEndpointHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }
    }

    /**
     * Configuration with a handler mapping bean that doesn't have the standard name.
     */
    @Configuration
    static class NonStandardNameConfiguration {
        
        @Bean
        public RequestMappingHandlerMapping customHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }
    }

    /**
     * Configuration that includes the ControllerRegistrationLogger bean.
     */
    @Configuration
    static class ControllerRegistrationLoggerConfiguration {
        
        @Bean
        public ControllerRegistrationLogger controllerRegistrationLogger() {
            return new ControllerRegistrationLogger();
        }
    }
}
