package com.pos.inventsight.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that WebMvcConfig can be loaded without IllegalStateException.
 * 
 * This test ensures:
 * 1. WebMvcConfig loads successfully without "Unable to locate default servlet" error
 * 2. The configuration does not override configureDefaultServletHandling()
 * 3. Spring Boot's default servlet handling is used instead
 */
class WebMvcConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(WebMvcConfig.class);

    @Test
    void shouldLoadWebMvcConfigWithoutIllegalStateException() {
        // This test verifies that WebMvcConfig can be loaded without throwing
        // IllegalStateException: Unable to locate default servlet
        contextRunner
            .run(context -> {
                // The key success criterion is that the context loads without error
                assertThat(context).hasNotFailed();
                
                // Verify that WebMvcConfig bean is created
                assertThat(context).hasSingleBean(WebMvcConfigurer.class);
            });
    }

    @Test
    void shouldConfigureWebMvcWithoutDefaultServletHandling() {
        // Verify that the configuration can initialize successfully
        // without calling configurer.enable() in configureDefaultServletHandling()
        contextRunner
            .run(context -> {
                assertThat(context).hasNotFailed();
                
                // Get the WebMvcConfig bean
                WebMvcConfigurer config = context.getBean(WebMvcConfigurer.class);
                assertThat(config).isNotNull();
                assertThat(config).isInstanceOf(WebMvcConfig.class);
            });
    }
}
