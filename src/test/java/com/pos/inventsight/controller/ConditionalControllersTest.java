package com.pos.inventsight.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that AuthController and RegistrationController
 * are conditionally loaded based on the inventsight.security.local-login.enabled property.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "inventsight.security.local-login.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test",
    "spring.data.redis.host=localhost"
})
class ConditionalControllersTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testAuthControllerNotLoadedWhenLocalLoginDisabled() {
        // Given local-login is disabled (via test properties)
        // When checking for AuthController bean
        // Then it should not be present in the context
        assertThrows(NoSuchBeanDefinitionException.class, () -> {
            applicationContext.getBean(AuthController.class);
        }, "AuthController should not be loaded when inventsight.security.local-login.enabled=false");
    }

    @Test
    void testRegistrationControllerNotLoadedWhenLocalLoginDisabled() {
        // Given local-login is disabled (via test properties)
        // When checking for RegistrationController bean
        // Then it should not be present in the context
        assertThrows(NoSuchBeanDefinitionException.class, () -> {
            applicationContext.getBean(RegistrationController.class);
        }, "RegistrationController should not be loaded when inventsight.security.local-login.enabled=false");
    }
}
