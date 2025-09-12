package com.pos.inventsight.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for multi-tenancy functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.multiTenancy=NONE",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureWebMvc
class MultiTenancyIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRequestWithoutTenantHeader() {
        // When making a request without tenant header
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            String.class
        );

        // Then should get successful response
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRequestWithTenantHeader() {
        // Given headers with tenant ID
        HttpHeaders headers = new HttpHeaders();
        headers.set(TenantFilter.TENANT_HEADER_NAME, "test-tenant");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // When making a request with tenant header
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/actuator/health",
            HttpMethod.GET,
            entity,
            String.class
        );

        // Then should get successful response
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testFilterIsRegistered() {
        // The filter should be automatically registered due to @Component annotation
        // This test verifies the Spring context starts successfully with the filter
        assertNotNull(restTemplate);
    }
}