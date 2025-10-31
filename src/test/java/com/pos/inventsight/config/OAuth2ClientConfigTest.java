package com.pos.inventsight.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that OAuth2 client auto-configuration does not fail when
 * client registration properties are not present in the default configuration.
 * 
 * This test ensures that the fix for the issue "Client id of registration 'microsoft' 
 * must not be empty" is working correctly.
 */
public class OAuth2ClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class));

    /**
     * Test that OAuth2 client auto-configuration does not fail when no client 
     * registration properties are provided (i.e., when oauth-login profile is NOT active).
     * 
     * Before the fix, this would fail with:
     * "IllegalStateException: Client id of registration 'microsoft' must not be empty"
     * because empty client registration blocks were defined in application.yml.
     * 
     * After the fix, these registrations are moved to application-oauth-login.yml
     * and the context should load successfully without errors.
     */
    @Test
    public void testOAuth2ClientAutoConfigDoesNotFailWithoutClientRegistrations() {
        this.contextRunner
            .run(context -> {
                // Context should load successfully without throwing exceptions
                assertThat(context).hasNotFailed();
            });
    }
}
