package com.pos.inventsight.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CircularDependencyTest {

    @Test
    void passwordEncoderCanBeCreatedWithoutCircularDependency() {
        // Create a minimal Spring context with just our config classes
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(PasswordEncoderConfig.class);
            context.refresh();
            
            // This will succeed if PasswordEncoder can be created independently
            PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
            assertNotNull(passwordEncoder);
            
            // Test that it actually works
            String encodedPassword = passwordEncoder.encode("test");
            assertNotNull(encodedPassword);
        }
    }
}