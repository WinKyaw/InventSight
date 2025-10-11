package com.pos.inventsight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.model.sql.AuditEvent;
import com.pos.inventsight.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    
    @Mock
    private AuditEventRepository auditEventRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private AuditService auditService;
    
    @BeforeEach
    void setUp() {
        // Setup common mocks - lenient for tests that don't use it
        lenient().when(auditEventRepository.findLatestEvent(any(Pageable.class)))
                .thenReturn(Optional.empty());
    }
    
    @Test
    void testLogAuditEvent_Success() throws Exception {
        // Given
        String actor = "test@example.com";
        Long actorId = 1L;
        String action = "USER_LOGIN";
        String entityType = "User";
        String entityId = "1";
        String details = "Login successful";
        
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"message\":\"Login successful\"}");
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // When
        AuditEvent result = auditService.log(actor, actorId, action, entityType, entityId, details);
        
        // Then
        assertNotNull(result);
        assertEquals(actor, result.getActor());
        assertEquals(actorId, result.getActorId());
        assertEquals(action, result.getAction());
        assertEquals(entityType, result.getEntityType());
        assertEquals(entityId, result.getEntityId());
        
        verify(auditEventRepository, times(1)).save(any(AuditEvent.class));
    }
    
    @Test
    void testLogAuditEvent_WithHashChaining() throws Exception {
        // Given
        AuditEvent previousEvent = AuditEvent.builder()
                .actor("previous@example.com")
                .actorId(1L)
                .action("PREVIOUS_ACTION")
                .build();
        previousEvent.setHash("previous-hash-123");
        
        // Override the default stubbing for this test
        reset(auditEventRepository);
        reset(objectMapper);
        when(auditEventRepository.findLatestEvent(any(Pageable.class)))
                .thenReturn(Optional.of(previousEvent));
        
        ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        when(auditEventRepository.save(eventCaptor.capture())).thenAnswer(i -> i.getArguments()[0]);
        
        // When
        auditService.log("test@example.com", 1L, "TEST_ACTION", "Test", "1", null);
        
        // Then
        AuditEvent savedEvent = eventCaptor.getValue();
        assertNotNull(savedEvent.getHash());
        assertEquals("previous-hash-123", savedEvent.getPrevHash());
    }
    
    @Test
    void testLogAuditEvent_WithNullDetails() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // When
        AuditEvent result = auditService.log("actor", 1L, "action", "entity", "id", null);
        
        // Then
        assertNotNull(result);
        assertNull(result.getDetailsJson());
    }
    
    @Test
    void testFindByTenant() {
        // Given
        UUID tenantId = UUID.randomUUID();
        Pageable pageable = Pageable.ofSize(10);
        
        // When
        auditService.findByTenant(tenantId, pageable);
        
        // Then
        verify(auditEventRepository, times(1)).findByTenantIdOrderByEventAtDesc(tenantId, pageable);
    }
    
    @Test
    void testFindByCompany() {
        // Given
        UUID companyId = UUID.randomUUID();
        Pageable pageable = Pageable.ofSize(10);
        
        // When
        auditService.findByCompany(companyId, pageable);
        
        // Then
        verify(auditEventRepository, times(1)).findByCompanyIdOrderByEventAtDesc(companyId, pageable);
    }
    
    @Test
    void testAuditEventBuilder() {
        // Test the builder pattern
        UUID tenantId = UUID.randomUUID();
        
        AuditEvent event = AuditEvent.builder()
                .actor("test@example.com")
                .actorId(1L)
                .action("TEST_ACTION")
                .entityType("TestEntity")
                .entityId("123")
                .tenantId(tenantId)
                .ipAddress("192.168.1.1")
                .userAgent("Test-Agent/1.0")
                .detailsJson("{\"test\":\"data\"}")
                .build();
        
        assertNotNull(event);
        assertEquals("test@example.com", event.getActor());
        assertEquals(1L, event.getActorId());
        assertEquals("TEST_ACTION", event.getAction());
        assertEquals("TestEntity", event.getEntityType());
        assertEquals("123", event.getEntityId());
        assertEquals(tenantId, event.getTenantId());
        assertEquals("192.168.1.1", event.getIpAddress());
        assertEquals("Test-Agent/1.0", event.getUserAgent());
        assertEquals("{\"test\":\"data\"}", event.getDetailsJson());
    }
}
