package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.IdempotencyKey;
import com.pos.inventsight.repository.sql.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for IdempotencyService
 */
public class IdempotencyServiceTest {
    
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    
    @InjectMocks
    private IdempotencyService idempotencyService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testComputeRequestHash() {
        String hash1 = idempotencyService.computeRequestHash("POST", "/api/test", "{\"foo\":\"bar\"}");
        String hash2 = idempotencyService.computeRequestHash("POST", "/api/test", "{\"foo\":\"bar\"}");
        String hash3 = idempotencyService.computeRequestHash("POST", "/api/test", "{\"foo\":\"baz\"}");
        
        // Same request should produce same hash
        assertEquals(hash1, hash2);
        
        // Different request should produce different hash
        assertNotEquals(hash1, hash3);
        
        // Hash should be 64 characters (SHA-256 in hex)
        assertEquals(64, hash1.length());
    }
    
    @Test
    void testStoreIdempotencyKey() {
        UUID tenantId = UUID.randomUUID();
        String key = "test-key-123";
        String endpoint = "/api/test";
        String requestHash = "abcd1234";
        int responseStatus = 200;
        String responseBody = "{\"success\":true}";
        
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        IdempotencyKey result = idempotencyService.storeIdempotencyKey(
            key, tenantId, tenantId, endpoint, requestHash, responseStatus, responseBody
        );
        
        assertNotNull(result);
        assertEquals(key, result.getIdempotencyKey());
        assertEquals(tenantId, result.getTenantId());
        assertEquals(endpoint, result.getEndpoint());
        assertEquals(requestHash, result.getRequestHash());
        assertEquals(responseStatus, result.getResponseStatus());
        assertEquals(responseBody, result.getResponseBody());
        
        verify(idempotencyKeyRepository, times(1)).save(any(IdempotencyKey.class));
    }
    
    @Test
    void testFindIdempotencyKey() {
        UUID tenantId = UUID.randomUUID();
        String key = "test-key-123";
        
        IdempotencyKey mockKey = new IdempotencyKey();
        mockKey.setIdempotencyKey(key);
        mockKey.setTenantId(tenantId);
        
        when(idempotencyKeyRepository.findByIdempotencyKeyAndTenantId(key, tenantId))
            .thenReturn(Optional.of(mockKey));
        
        Optional<IdempotencyKey> result = idempotencyService.findIdempotencyKey(key, tenantId);
        
        assertTrue(result.isPresent());
        assertEquals(key, result.get().getIdempotencyKey());
        assertEquals(tenantId, result.get().getTenantId());
        
        verify(idempotencyKeyRepository, times(1)).findByIdempotencyKeyAndTenantId(key, tenantId);
    }
    
    @Test
    void testCleanupExpiredKeys() {
        when(idempotencyKeyRepository.deleteExpiredKeys(any(LocalDateTime.class)))
            .thenReturn(5);
        
        idempotencyService.cleanupExpiredKeys();
        
        verify(idempotencyKeyRepository, times(1)).deleteExpiredKeys(any(LocalDateTime.class));
    }
}
