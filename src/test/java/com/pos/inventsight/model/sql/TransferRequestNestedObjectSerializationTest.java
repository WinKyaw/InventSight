package com.pos.inventsight.model.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that nested objects (Warehouse, Store, User) are properly serialized
 * with their full information, not just IDs
 */
public class TransferRequestNestedObjectSerializationTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Test
    public void testFromWarehouseSerializationIncludesAllFields() throws Exception {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("၁၈ လမ်း");
        warehouse.setLocation("၁၈ လမ်း");
        warehouse.setWarehouseType(Warehouse.WarehouseType.GENERAL);
        warehouse.setIsActive(true);
        
        TransferRequest request = new TransferRequest();
        request.setId(UUID.randomUUID());
        request.setFromWarehouse(warehouse);
        request.setRequestedQuantity(10);
        
        // Act
        String json = objectMapper.writeValueAsString(request);
        
        // Assert
        assertTrue(json.contains("\"name\":\"၁၈ လမ်း\""), 
            "Warehouse name should be serialized");
        assertTrue(json.contains("\"location\":\"၁၈ လမ်း\""), 
            "Warehouse location should be serialized");
        assertTrue(json.contains("\"warehouseType\":\"GENERAL\""), 
            "Warehouse type should be serialized");
        assertTrue(json.contains("\"isActive\":true"), 
            "Warehouse isActive should be serialized");
        
        // Verify password-related fields are excluded
        assertFalse(json.contains("\"password\""), 
            "Password should not be serialized");
    }
    
    @Test
    public void testToStoreSerializationIncludesAllFields() throws Exception {
        // Arrange
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setStoreName("CoN Store");
        store.setAddress("123 Main St");
        store.setCity("Yangon");
        store.setIsActive(true);
        
        TransferRequest request = new TransferRequest();
        request.setId(UUID.randomUUID());
        request.setToStore(store);
        request.setRequestedQuantity(10);
        
        // Act
        String json = objectMapper.writeValueAsString(request);
        
        // Assert
        assertTrue(json.contains("\"storeName\":\"CoN Store\""), 
            "Store name should be serialized");
        assertTrue(json.contains("\"address\":\"123 Main St\""), 
            "Store address should be serialized");
        assertTrue(json.contains("\"city\":\"Yangon\""), 
            "Store city should be serialized");
        assertTrue(json.contains("\"isActive\":true"), 
            "Store isActive should be serialized");
    }
    
    @Test
    public void testRequestedByUserSerializationIncludesAllFields() throws Exception {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("Jennie.Win_CoN9Z");
        user.setFirstName("Jennie");
        user.setLastName("Win");
        user.setEmail("jennie@example.com");
        user.setPassword("encrypted_password"); // This should be excluded
        
        TransferRequest request = new TransferRequest();
        request.setId(UUID.randomUUID());
        request.setRequestedBy(user);
        request.setRequestedQuantity(10);
        
        // Act
        String json = objectMapper.writeValueAsString(request);
        
        // Assert
        assertTrue(json.contains("\"username\":\"Jennie.Win_CoN9Z\""), 
            "User username should be serialized");
        assertTrue(json.contains("\"firstName\":\"Jennie\""), 
            "User firstName should be serialized");
        assertTrue(json.contains("\"lastName\":\"Win\""), 
            "User lastName should be serialized");
        assertTrue(json.contains("\"email\":\"jennie@example.com\""), 
            "User email should be serialized");
        
        // Verify password is excluded
        assertFalse(json.contains("\"password\""), 
            "User password should NOT be serialized");
        assertFalse(json.contains("encrypted_password"), 
            "Password value should NOT appear in JSON");
    }
    
    @Test
    public void testFromStoreSerializationIncludesAllFields() throws Exception {
        // Arrange
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setStoreName("Source Store");
        store.setAddress("456 Another St");
        store.setCity("Mandalay");
        store.setIsActive(true);
        
        TransferRequest request = new TransferRequest();
        request.setId(UUID.randomUUID());
        request.setFromStore(store);
        request.setRequestedQuantity(10);
        
        // Act
        String json = objectMapper.writeValueAsString(request);
        
        // Assert
        assertTrue(json.contains("\"storeName\":\"Source Store\""), 
            "From Store name should be serialized");
        assertTrue(json.contains("\"address\":\"456 Another St\""), 
            "From Store address should be serialized");
        assertTrue(json.contains("\"city\":\"Mandalay\""), 
            "From Store city should be serialized");
    }
    
    @Test
    public void testToWarehouseSerializationIncludesAllFields() throws Exception {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Destination Warehouse");
        warehouse.setLocation("Industrial Zone");
        warehouse.setWarehouseType(Warehouse.WarehouseType.DISTRIBUTION);
        warehouse.setIsActive(true);
        
        TransferRequest request = new TransferRequest();
        request.setId(UUID.randomUUID());
        request.setToWarehouse(warehouse);
        request.setRequestedQuantity(10);
        
        // Act
        String json = objectMapper.writeValueAsString(request);
        
        // Assert
        assertTrue(json.contains("\"name\":\"Destination Warehouse\""), 
            "To Warehouse name should be serialized");
        assertTrue(json.contains("\"location\":\"Industrial Zone\""), 
            "To Warehouse location should be serialized");
        assertTrue(json.contains("\"warehouseType\":\"DISTRIBUTION\""), 
            "To Warehouse type should be serialized");
    }
    
    @Test
    public void testCompleteTransferRequestWithAllNestedObjects() throws Exception {
        // Arrange - Create all nested objects
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");
        
        Warehouse fromWarehouse = new Warehouse();
        fromWarehouse.setId(UUID.randomUUID());
        fromWarehouse.setName("Main Warehouse");
        fromWarehouse.setLocation("Central");
        fromWarehouse.setWarehouseType(Warehouse.WarehouseType.GENERAL);
        
        Store toStore = new Store();
        toStore.setId(UUID.randomUUID());
        toStore.setStoreName("Retail Store");
        toStore.setAddress("789 Retail Ave");
        
        User requestedBy = new User();
        requestedBy.setId(UUID.randomUUID());
        requestedBy.setUsername("john.doe");
        requestedBy.setFirstName("John");
        requestedBy.setLastName("Doe");
        requestedBy.setEmail("john@example.com");
        requestedBy.setPassword("secret");
        
        User approvedBy = new User();
        approvedBy.setId(UUID.randomUUID());
        approvedBy.setUsername("jane.manager");
        approvedBy.setFirstName("Jane");
        approvedBy.setLastName("Manager");
        approvedBy.setEmail("jane@example.com");
        
        TransferRequest request = new TransferRequest();
        request.setId(UUID.randomUUID());
        request.setCompany(company);
        request.setFromWarehouse(fromWarehouse);
        request.setToStore(toStore);
        request.setRequestedBy(requestedBy);
        request.setApprovedBy(approvedBy);
        request.setRequestedQuantity(50);
        request.setProductName("Test Product");
        
        // Act
        String json = objectMapper.writeValueAsString(request);
        
        // Assert - Verify all nested objects have their properties
        // Company
        assertTrue(json.contains("\"name\":\"Test Company\""), 
            "Company name should be present");
        
        // From Warehouse
        assertTrue(json.contains("\"name\":\"Main Warehouse\""), 
            "From Warehouse name should be present");
        assertTrue(json.contains("\"location\":\"Central\""), 
            "From Warehouse location should be present");
        
        // To Store
        assertTrue(json.contains("\"storeName\":\"Retail Store\""), 
            "To Store name should be present");
        assertTrue(json.contains("\"address\":\"789 Retail Ave\""), 
            "To Store address should be present");
        
        // Requested By User
        assertTrue(json.contains("\"username\":\"john.doe\""), 
            "Requested by username should be present");
        assertTrue(json.contains("\"firstName\":\"John\""), 
            "Requested by firstName should be present");
        assertTrue(json.contains("\"lastName\":\"Doe\""), 
            "Requested by lastName should be present");
        
        // Approved By User
        assertTrue(json.contains("\"username\":\"jane.manager\""), 
            "Approved by username should be present");
        assertTrue(json.contains("\"firstName\":\"Jane\""), 
            "Approved by firstName should be present");
        
        // Verify passwords are excluded
        assertFalse(json.contains("\"password\""), 
            "Password field should not be in JSON");
        assertFalse(json.contains("secret"), 
            "Password value should not be in JSON");
        
        // Verify hibernateLazyInitializer is excluded (this is the important one)
        assertFalse(json.contains("hibernateLazyInitializer"), 
            "hibernateLazyInitializer should be excluded");
    }
}
