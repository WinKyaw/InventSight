package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class StoreTest {
    
    private Store store;
    
    @BeforeEach
    void setUp() {
        store = new Store();
        store.setStoreName("Test Store");
        store.setAddress("123 Test Street");
        store.setCity("Test City");
        store.setState("Test State");
        store.setCountry("Test Country");
        store.setPostalCode("12345");
        store.setPhone("555-1234");
        store.setEmail("test@store.com");
    }
    
    @Test
    void testStoreCreation() {
        assertNotNull(store);
        assertEquals("Test Store", store.getStoreName());
        assertEquals("123 Test Street", store.getAddress());
        assertEquals("Test City", store.getCity());
        assertEquals("Test State", store.getState());
        assertEquals("Test Country", store.getCountry());
        assertTrue(store.getIsActive());
    }
    
    @Test
    void testGetFullAddress() {
        String fullAddress = store.getFullAddress();
        assertNotNull(fullAddress);
        assertTrue(fullAddress.contains("123 Test Street"));
        assertTrue(fullAddress.contains("Test City"));
        assertTrue(fullAddress.contains("Test State"));
        assertTrue(fullAddress.contains("12345"));
        assertTrue(fullAddress.contains("Test Country"));
    }
    
    @Test
    void testGetFullAddressWithNullValues() {
        Store emptyStore = new Store();
        emptyStore.setStoreName("Empty Store");
        String fullAddress = emptyStore.getFullAddress();
        assertNotNull(fullAddress);
        assertEquals("", fullAddress);
    }
    
    @Test
    void testConstructorWithParameters() {
        Store paramStore = new Store("Param Store", "456 Param St", "Param City", "Param State", "Param Country");
        assertEquals("Param Store", paramStore.getStoreName());
        assertEquals("456 Param St", paramStore.getAddress());
        assertEquals("Param City", paramStore.getCity());
        assertEquals("Param State", paramStore.getState());
        assertEquals("Param Country", paramStore.getCountry());
    }
}