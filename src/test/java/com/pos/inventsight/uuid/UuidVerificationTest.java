package com.pos.inventsight.uuid;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.multiTenancy=NONE",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@Transactional
public class UuidVerificationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired  
    private StoreRepository storeRepository;

    @Test
    public void testStoreUuidPrimaryKey() {
        // Create a new store
        Store store = new Store();
        store.setStoreName("Test Store");
        store.setAddress("123 Test St");
        store.setCity("Test City");
        store.setState("TS");
        store.setCountry("Test Country");
        
        // Save the store
        Store savedStore = storeRepository.save(store);
        
        // Verify UUID is generated and is valid
        assertNotNull(savedStore.getId());
        assertTrue(savedStore.getId() instanceof UUID);
        
        // Verify we can find it by UUID
        Store foundStore = storeRepository.findById(savedStore.getId()).orElse(null);
        assertNotNull(foundStore);
        assertEquals("Test Store", foundStore.getStoreName());
    }

    @Test
    public void testProductUuidPrimaryKey() {
        // First create a store
        Store store = new Store("Test Store", "123 Test St", "Test City", "TS", "Test Country");
        Store savedStore = storeRepository.save(store);
        
        // Create a new product
        Product product = new Product();
        product.setName("Test Product");
        product.setSku("TEST-001");
        product.setOriginalPrice(BigDecimal.valueOf(10.00));
        product.setOwnerSetSellPrice(BigDecimal.valueOf(12.00));
        product.setRetailPrice(BigDecimal.valueOf(15.00));
        product.setQuantity(100);
        product.setStore(savedStore);
        
        // Save the product
        Product savedProduct = productRepository.save(product);
        
        // Verify UUID is generated and is valid
        assertNotNull(savedProduct.getId());
        assertTrue(savedProduct.getId() instanceof UUID);
        
        // Verify we can find it by UUID
        Product foundProduct = productRepository.findById(savedProduct.getId()).orElse(null);
        assertNotNull(foundProduct);
        assertEquals("Test Product", foundProduct.getName());
        assertEquals("TEST-001", foundProduct.getSku());
        
        // Verify the store relationship works
        assertNotNull(foundProduct.getStore());
        assertEquals(savedStore.getId(), foundProduct.getStore().getId());
    }

    @Test
    public void testProductStoreRelationship() {
        // Create store
        Store store = new Store("Relationship Test Store", "456 Test Ave", "Test City", "TS", "Test Country");
        Store savedStore = storeRepository.save(store);
        
        // Create product associated with store
        Product product = new Product("Relationship Test Product", "REL-001", 
                BigDecimal.valueOf(20.00), BigDecimal.valueOf(25.00), 
                BigDecimal.valueOf(30.00), 50, savedStore);
        Product savedProduct = productRepository.save(product);
        
        // Verify both have UUIDs
        assertNotNull(savedStore.getId());
        assertNotNull(savedProduct.getId());
        assertTrue(savedStore.getId() instanceof UUID);
        assertTrue(savedProduct.getId() instanceof UUID);
        
        // Verify the relationship
        Product foundProduct = productRepository.findById(savedProduct.getId()).orElse(null);
        assertNotNull(foundProduct);
        assertNotNull(foundProduct.getStore());
        assertEquals(savedStore.getId(), foundProduct.getStore().getId());
    }
}