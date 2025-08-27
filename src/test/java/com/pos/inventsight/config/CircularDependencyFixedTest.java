package com.pos.inventsight.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.pos.inventsight.repository.nosql.ActivityLogRepository;
import com.pos.inventsight.repository.nosql.InventoryAnalyticsRepository;
import com.pos.inventsight.repository.sql.ProductRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify the circular dependency has been resolved
 * This test starts the full Spring Boot application context
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration"
    }
)
@ActiveProfiles("test")
class CircularDependencyFixedTest {

    // Mock the repository beans that would normally be auto-configured
    @MockBean
    private ActivityLogRepository activityLogRepository;
    
    @MockBean
    private InventoryAnalyticsRepository analyticsRepository;
    
    @MockBean
    private ProductRepository productRepository;

    @Test
    void testCircularDependencyResolved() {
        // If we get to this point, it means the application context loaded successfully
        // without any circular dependency errors. The @Lazy annotation should have
        // resolved the circular dependency between InventoryAnalyticsService and SaleService
        assertTrue(true, "Application context started successfully - circular dependency resolved");
    }
}