package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.model.sql.SaleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "spring.sql.init.mode=never"
})
public class SaleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SaleRepository saleRepository;

    @Test
    public void testFindTodaySales_QueryCompilation() {
        // This test mainly verifies that the query compiles without syntax errors
        // When we call the repository method, if there were JPQL syntax errors,
        // it would fail at this point
        
        // Given - Just call the method to test query compilation
        List<Sale> todaysSales = saleRepository.findTodaySales();
        
        // Then - If we get here, the query compiled successfully
        assertThat(todaysSales).isNotNull();
    }
}