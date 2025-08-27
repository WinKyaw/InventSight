package com.pos.inventsight.repository.sql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to verify that the SaleRepository can be created without errors.
 * This test focuses on ensuring that the JPQL query syntax is valid and the 
 * repository configuration is correct.
 */
@SpringBootTest
@TestPropertySource(properties = {
    // Use H2 in-memory database
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver", 
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    
    // Disable schema initialization
    "spring.sql.init.mode=never",
    
    // Use H2 dialect
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    
    // Disable MongoDB and Redis for this test
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
public class SaleRepositoryIntegrationTest {

    /**
     * This test verifies that the Spring application context can be loaded successfully
     * with the SaleRepository. If there were any issues with:
     * 1. Package name mismatch in @EnableJpaRepositories
     * 2. JPQL query syntax errors in SaleRepository.findTodaySales()
     * 
     * This test would fail during context initialization.
     */
    @Test
    public void contextLoads() {
        // If we reach this point, it means:
        // 1. The repository package scanning worked correctly
        // 2. All JPQL queries in SaleRepository compiled successfully
        // 3. The Spring context loaded without errors
        
        // This effectively tests that our fixes resolved the original issues:
        // - Fixed package name from "com.inventsight.repository.sql" to "com.pos.inventsight.repository.sql"
        // - Fixed JPQL syntax from "DATE(s.createdAt) = CURRENT_DATE" to use YEAR/MONTH/DAY functions
    }
}