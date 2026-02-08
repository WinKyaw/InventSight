package com.pos.inventsight.filter;

import com.pos.inventsight.model.sql.IdempotencyKey;
import com.pos.inventsight.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for IdempotencyKeyFilter to verify request body handling
 */
class IdempotencyKeyFilterTest {

    @Mock
    private IdempotencyService idempotencyService;

    private IdempotencyKeyFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        filter = new IdempotencyKeyFilter(idempotencyService);
        
        // Set idempotencyEnabled to true using reflection since @Value won't work in unit tests
        java.lang.reflect.Field field = IdempotencyKeyFilter.class.getDeclaredField("idempotencyEnabled");
        field.setAccessible(true);
        field.set(filter, true);
    }

    @Test
    void testRequestBodyAvailableForNewIdempotencyKey() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";
        String requestBody = "{\"receivedQuantity\":30,\"receiverName\":\"Test\"}";
        
        when(idempotencyService.getCurrentTenantId()).thenReturn(tenantId);
        when(idempotencyService.findIdempotencyKey(idempotencyKey, tenantId))
            .thenReturn(Optional.empty());
        when(idempotencyService.computeRequestHash(anyString(), anyString(), anyString()))
            .thenReturn("hash123");
        when(idempotencyService.storeIdempotencyKey(anyString(), any(), any(), anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(new IdempotencyKey());
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        request.setRequestURI("/api/transfers/123/receive");
        request.addHeader("Idempotency-Key", idempotencyKey);
        request.setContent(requestBody.getBytes());
        request.setContentType("application/json");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Track whether chain was called and body was readable
        final boolean[] bodyWasReadable = {false};
        
        // Create a custom filter chain that verifies the request body is available
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) 
                    throws IOException, ServletException {
                // Verify request body can be read by controller
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                byte[] bodyBytes = httpRequest.getInputStream().readAllBytes();
                String body = new String(bodyBytes);
                
                // This should succeed - body should be available
                if (body != null && !body.isEmpty() && body.contains("receivedQuantity")) {
                    bodyWasReadable[0] = true;
                }
                
                // Write a response
                response.getOutputStream().write("{\"success\":true}".getBytes());
                ((HttpServletResponse) response).setStatus(200);
            }
        };
        
        // Act
        filter.doFilter(request, response, chain);
        
        // Assert - verify body was readable
        assertTrue(bodyWasReadable[0], "Request body should be readable by the controller");
        
        // Assert - verify idempotency key was stored
        verify(idempotencyService).storeIdempotencyKey(
            eq(idempotencyKey),
            eq(tenantId),
            eq(tenantId),
            eq("/api/transfers/123/receive"),
            anyString(),
            eq(200),
            anyString()
        );
    }

    @Test
    void testReplayCachedResponseForExistingIdempotencyKey() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";
        String cachedResponseBody = "{\"success\":true,\"transferId\":\"123\"}";
        
        IdempotencyKey existingKey = new IdempotencyKey();
        existingKey.setIdempotencyKey(idempotencyKey);
        existingKey.setTenantId(tenantId);
        existingKey.setResponseStatus(200);
        existingKey.setResponseBody(cachedResponseBody);
        existingKey.setRequestHash("hash123");
        
        when(idempotencyService.getCurrentTenantId()).thenReturn(tenantId);
        when(idempotencyService.findIdempotencyKey(idempotencyKey, tenantId))
            .thenReturn(Optional.of(existingKey));
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        request.setRequestURI("/api/transfers/123/receive");
        request.addHeader("Idempotency-Key", idempotencyKey);
        request.setContent("{\"receivedQuantity\":30}".getBytes());
        request.setContentType("application/json");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Act
        filter.doFilter(request, response, chain);
        
        // Assert
        assertEquals(200, response.getStatus());
        
        // Verify chain was NOT called (cached response used)
        assertNull(chain.getRequest(), "Filter chain should not be called for cached response");
        
        // Verify no new key was stored
        verify(idempotencyService, never()).storeIdempotencyKey(
            anyString(), any(), any(), anyString(), anyString(), anyInt(), anyString()
        );
    }

    @Test
    void testNoIdempotencyKeyHeaderPassesThrough() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        request.setRequestURI("/api/transfers/123/receive");
        request.setContent("{\"receivedQuantity\":30}".getBytes());
        request.setContentType("application/json");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Act
        filter.doFilter(request, response, chain);
        
        // Assert
        assertNotNull(chain.getRequest(), "Filter chain should be called");
        verify(idempotencyService, never()).findIdempotencyKey(anyString(), any());
    }

    @Test
    void testGetRequestSkipsIdempotencyCheck() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        when(idempotencyService.getCurrentTenantId()).thenReturn(tenantId);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/transfers/123");
        request.addHeader("Idempotency-Key", "test-key-123");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Act
        filter.doFilter(request, response, chain);
        
        // Assert
        assertNotNull(chain.getRequest(), "Filter chain should be called");
        verify(idempotencyService, never()).findIdempotencyKey(anyString(), any());
    }

    @Test
    void testPublicEndpointSkipsIdempotencyCheck() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/auth/login");
        request.addHeader("Idempotency-Key", "test-key-123");
        request.setContent("{\"username\":\"test\"}".getBytes());
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Act
        filter.doFilter(request, response, chain);
        
        // Assert
        assertNotNull(chain.getRequest(), "Filter chain should be called");
        verify(idempotencyService, never()).findIdempotencyKey(anyString(), any());
    }

    @Test
    void testNoTenantContextSkipsIdempotencyCheck() throws Exception {
        // Arrange
        when(idempotencyService.getCurrentTenantId()).thenReturn(null);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        request.setRequestURI("/api/transfers/123/receive");
        request.addHeader("Idempotency-Key", "test-key-123");
        request.setContent("{\"receivedQuantity\":30}".getBytes());
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Act
        filter.doFilter(request, response, chain);
        
        // Assert
        assertNotNull(chain.getRequest(), "Filter chain should be called");
        verify(idempotencyService, never()).findIdempotencyKey(anyString(), any());
    }
}
