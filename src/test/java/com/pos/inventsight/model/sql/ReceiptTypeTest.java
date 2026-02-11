package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReceiptType enum
 */
public class ReceiptTypeTest {
    
    /**
     * Test that HOLD receipt type exists and can be parsed
     */
    @Test
    public void testHoldReceiptTypeExists() {
        // Verify that HOLD enum constant exists
        ReceiptType hold = ReceiptType.HOLD;
        
        // Verify the enum value is not null
        assertNotNull(hold, "HOLD receipt type should exist");
        
        // Verify it can be converted from string (as the API endpoint does during JSON deserialization)
        ReceiptType fromString = ReceiptType.valueOf("HOLD");
        assertEquals(ReceiptType.HOLD, fromString, 
            "HOLD should be parseable from string");
    }
    
    /**
     * Test that all expected receipt types exist
     */
    @Test
    public void testAllReceiptTypesExist() {
        // Verify all expected receipt types exist
        assertNotNull(ReceiptType.IN_STORE);
        assertNotNull(ReceiptType.DELIVERY);
        assertNotNull(ReceiptType.PICKUP);
        assertNotNull(ReceiptType.HOLD);
        
        // Verify there are exactly 4 receipt types
        assertEquals(4, ReceiptType.values().length, 
            "There should be exactly 4 receipt types");
    }
    
    /**
     * Test that each receipt type can be converted to and from string
     */
    @Test
    public void testReceiptTypeStringConversion() {
        for (ReceiptType type : ReceiptType.values()) {
            // Verify each type can be converted to string and back
            String name = type.name();
            ReceiptType parsed = ReceiptType.valueOf(name);
            assertEquals(type, parsed, 
                "Receipt type " + name + " should be parseable from its string representation");
        }
    }
}
