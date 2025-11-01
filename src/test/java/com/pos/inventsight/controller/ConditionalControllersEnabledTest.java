package com.pos.inventsight.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that AuthController and RegistrationController
 * are loaded when inventsight.security.local-login.enabled=true.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "inventsight.security.local-login.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test",
    "spring.data.redis.host=localhost"
})
class ConditionalControllersEnabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testAuthControllerLoadedWhenLocalLoginEnabled() {
        // Given local-login is enabled (via test properties)
        // When checking for AuthController bean
        // Then it should be present in the context
        AuthController authController = applicationContext.getBean(AuthController.class);
        assertNotNull(authController, "AuthController should be loaded when inventsight.security.local-login.enabled=true");
    }

    @Test
    void testRegistrationControllerLoadedWhenLocalLoginEnabled() {
        // Given local-login is enabled (via test properties)
        // When checking for RegistrationController bean
        // Then it should be present in the context
        RegistrationController registrationController = applicationContext.getBean(RegistrationController.class);
        assertNotNull(registrationController, "RegistrationController should be loaded when inventsight.security.local-login.enabled=true");
    }
}
