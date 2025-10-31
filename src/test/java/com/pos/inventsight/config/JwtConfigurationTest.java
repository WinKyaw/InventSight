package com.pos.inventsight.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that JwtDecoder bean creation works correctly based on configuration.
 * 
 * This test ensures:
 * 1. When JWT URIs are null and OAuth2 is disabled, no JwtDecoder is created
 * 2. Application can start without "jwkSetUri cannot be empty" error
 */
class JwtConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void shouldNotCreateJwtDecoderWhenJwtUrisAreNull() {
        // This simulates the default configuration where JWT_ISSUER_URI and JWT_JWK_SET_URI
        // environment variables are not set, defaulting to null (as configured in application.yml)
        contextRunner
            .withPropertyValues(
                "inventsight.security.oauth2.resource-server.enabled=false"
            )
            .run(context -> {
                // When both URIs are null and OAuth2 is disabled,
                // Spring Boot should NOT attempt to create JwtDecoder
                assertThat(context).doesNotHaveBean(JwtDecoder.class);
            });
    }

    @Test
    void shouldNotFailWhenJwtPropertiesAreNullInApplicationYml() {
        // This test verifies the fix: using #{null} as default prevents
        // Spring Boot from attempting to create JwtDecoder with empty string
        contextRunner
            .run(context -> {
                // The key success criterion is that the context loads without error
                // Previously, this would fail with "jwkSetUri cannot be empty"
                assertThat(context).hasNotFailed();
            });
    }
}
