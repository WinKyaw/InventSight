package com.pos.inventsight.filter;

import com.pos.inventsight.model.sql.IdempotencyKey;
import com.pos.inventsight.service.IdempotencyService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Filter that enforces idempotency keys on write operations (POST/PUT/PATCH/DELETE).
 * Ensures duplicate requests with the same Idempotency-Key return identical responses
 * without duplicate processing.
 * 
 * Order: After CompanyTenantFilter and Auth, so tenant context and authentication are available.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IdempotencyKeyFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyKeyFilter.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    
    private final IdempotencyService idempotencyService;
    
    @Value("${inventsight.sync.idempotency.enabled:true}")
    private boolean idempotencyEnabled;
    
    public IdempotencyKeyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!idempotencyEnabled) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String method = httpRequest.getMethod();
        String requestUri = httpRequest.getRequestURI();
        
        // Only apply to write operations
        if (!isWriteOperation(method)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Skip for public endpoints
        if (isPublicEndpoint(requestUri)) {
            chain.doFilter(request, response);
            return;
        }
        
        String idempotencyKey = httpRequest.getHeader(IDEMPOTENCY_KEY_HEADER);
        
        // If no idempotency key provided, continue without idempotency check
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.debug("No idempotency key provided for {} {}", method, requestUri);
            chain.doFilter(request, response);
            return;
        }
        
        // Get tenant ID from current context
        UUID tenantId = idempotencyService.getCurrentTenantId();
        if (tenantId == null) {
            logger.debug("No tenant context available for idempotency check");
            chain.doFilter(request, response);
            return;
        }
        
        // Wrap request and response to cache content
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);
        
        try {
            // Check if this idempotency key already exists
            Optional<IdempotencyKey> existingKey = idempotencyService.findIdempotencyKey(idempotencyKey, tenantId);
            
            if (existingKey.isPresent()) {
                // Compute hash of current request
                String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
                String currentHash = idempotencyService.computeRequestHash(method, requestUri, requestBody);
                
                IdempotencyKey existing = existingKey.get();
                
                // Check if request hash matches
                if (!currentHash.equals(existing.getRequestHash())) {
                    // Same key, different request - conflict
                    logger.warn("Idempotency key conflict: {} for tenant: {} - request hash mismatch", 
                               idempotencyKey, tenantId);
                    httpResponse.setStatus(HttpServletResponse.SC_CONFLICT);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\": \"Idempotency key already used with different request\"}");
                    return;
                }
                
                // Same key, same request - replay cached response
                logger.info("Replaying cached response for idempotency key: {} tenant: {}", idempotencyKey, tenantId);
                httpResponse.setStatus(existing.getResponseStatus());
                httpResponse.setContentType("application/json");
                if (existing.getResponseBody() != null) {
                    httpResponse.getWriter().write(existing.getResponseBody());
                }
                return;
            }
            
            // New idempotency key - process request normally
            chain.doFilter(wrappedRequest, wrappedResponse);
            
            // After processing, cache the response
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
            String requestHash = idempotencyService.computeRequestHash(method, requestUri, requestBody);
            
            // Store idempotency key with response
            idempotencyService.storeIdempotencyKey(
                idempotencyKey,
                tenantId,
                tenantId, // Use tenantId as companyId
                requestUri,
                requestHash,
                wrappedResponse.getStatus(),
                responseBody
            );
            
            // Copy cached response to actual response
            wrappedResponse.copyBodyToResponse();
            
        } catch (Exception e) {
            logger.error("Error processing idempotency key: {}", e.getMessage(), e);
            // On error, try to continue without idempotency
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    private boolean isWriteOperation(String method) {
        return "POST".equalsIgnoreCase(method) ||
               "PUT".equalsIgnoreCase(method) ||
               "PATCH".equalsIgnoreCase(method) ||
               "DELETE".equalsIgnoreCase(method);
    }
    
    private boolean isPublicEndpoint(String requestUri) {
        return requestUri.startsWith("/auth/") ||
               requestUri.startsWith("/api/register") ||
               requestUri.startsWith("/api/auth/register") ||
               requestUri.startsWith("/api/auth/signup") ||
               requestUri.startsWith("/register") ||
               requestUri.startsWith("/health") ||
               requestUri.startsWith("/actuator");
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("IdempotencyKeyFilter initialized - enabled: {}", idempotencyEnabled);
    }
    
    @Override
    public void destroy() {
        logger.info("IdempotencyKeyFilter destroyed");
    }
}
