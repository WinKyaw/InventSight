package com.pos.inventsight.repository.sql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify EventRepository query validation is fixed
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "spring.sql.init.mode=never"
})
public class EventRepositoryValidationTest {

    @Test
    void contextLoads() {
        // This test just verifies that the Spring context loads without JPQL validation errors
        // The fact that this test runs means the EventRepository query is valid
        assertTrue(true, "EventRepository query validation passed");
    }
}