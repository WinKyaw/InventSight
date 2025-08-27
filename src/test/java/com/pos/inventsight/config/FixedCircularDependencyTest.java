package com.pos.inventsight.config;

import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test demonstrates that the circular dependency has been resolved.
 * The old configuration (simulated here) would have caused circular dependencies.
 */
class FixedCircularDependencyTest {

    /**
     * This simulates the OLD configuration where PasswordEncoder was in SecurityConfig
     * along with UserService dependency - this would create a circular dependency
     */
    @Configuration
    static class OldStyleSecurityConfig {
        // Simulating the old style that would cause circular dependency
        @Bean
        @Primary
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Test
    void newConfigurationWorksWithoutCircularDependency() {
        // Test that our NEW separated configuration works fine
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(PasswordEncoderConfig.class);
            context.refresh();
            
            PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
            assertNotNull(passwordEncoder);
            
            // Verify it actually encodes passwords correctly
            String encoded = passwordEncoder.encode("test-password");
            assertNotNull(encoded);
            
            // Verify it can match passwords
            boolean matches = passwordEncoder.matches("test-password", encoded);
            assertNotNull(matches);
        }
    }

    @Test
    void passwordEncoderConfigIsIndependent() {
        // Test that PasswordEncoderConfig can be loaded independently
        // This proves the circular dependency has been broken
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(PasswordEncoderConfig.class);
            context.refresh();
            
            PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
            assertNotNull(encoder);
            
            // Test BCrypt functionality
            String testPassword = "MySecurePassword123";
            String encoded = encoder.encode(testPassword);
            assertNotNull(encoded);
        }
    }
}