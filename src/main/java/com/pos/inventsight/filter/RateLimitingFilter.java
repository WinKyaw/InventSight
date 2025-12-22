package com.pos.inventsight.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Bucket4j (token bucket algorithm).
 * Applies per-IP and per-tenant rate limits.
 * 
 * Configuration:
 * - Global limit: 100 requests per minute per IP
 * - Excludes: /api/auth/**, /actuator/health, /api/public/**
 * - Tracks by IP address and tenant
 * 
 * Order: Highest precedence to apply rate limiting before any other processing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Value("${inventsight.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;
    
    @Value("${inventsight.rate-limiting.per-tenant.requests-per-minute:1000}")
    private int tenantRpm;
    
    @Value("${inventsight.rate-limiting.per-ip.requests-per-minute:100}")
    private int ipRpm;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!rateLimitingEnabled) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestUri = httpRequest.getRequestURI();
        
        // ✅ FIXED: Exclude auth and health endpoints from rate limiting
        if (shouldSkipRateLimiting(requestUri)) {
            logger.debug("⚡ Skipping rate limit for: {}", requestUri);
            chain.doFilter(request, response);
            return;
        }
        
        String clientIp = getClientIp(httpRequest);
        String tenantId = httpRequest.getHeader("X-Tenant-ID");
        
        // Check per-IP limit (using regular IP limit for non-auth endpoints)
        String ipKey = "ip:" + clientIp;
        Bucket ipBucket = resolveBucket(ipKey, ipRpm);
        
        if (!ipBucket.tryConsume(1)) {
            logger.warn("⚠️ Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestUri);
            sendRateLimitError(httpResponse, ipRpm, "IP");
            return;
        }
        
        // Check per-tenant limit (if tenant ID is present)
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            String tenantKey = "tenant:" + tenantId;
            Bucket tenantBucket = resolveBucket(tenantKey, tenantRpm);
            
            if (!tenantBucket.tryConsume(1)) {
                logger.warn("⚠️ Rate limit exceeded for tenant: {} on endpoint: {}", tenantId, requestUri);
                sendRateLimitError(httpResponse, tenantRpm, "tenant");
                return;
            }
        }
        
        // All rate limit checks passed
        chain.doFilter(request, response);
    }
    
    /**
     * ✅ NEW: Determine if rate limiting should be skipped for this endpoint
     * 
     * @param requestUri Request URI to check
     * @return true if rate limiting should be skipped, false otherwise
     */
    private boolean shouldSkipRateLimiting(String requestUri) {
        // Exclude authentication endpoints
        if (requestUri.startsWith("/api/auth/")) {
            return true;
        }
        
        // Exclude health check endpoints
        if (requestUri.startsWith("/actuator/health")) {
            return true;
        }
        
        // Exclude public endpoints
        if (requestUri.startsWith("/api/public/")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Resolve or create a bucket for the given key and limit
     * @param key Bucket key (e.g., "ip:192.168.1.1" or "tenant:uuid")
     * @param requestsPerMinute Requests per minute limit
     * @return Bucket instance
     */
    private Bucket resolveBucket(String key, int requestsPerMinute) {
        return buckets.computeIfAbsent(key, k -> {
            // Create bucket with token bucket algorithm
            // Capacity: requestsPerMinute tokens
            // Refill: requestsPerMinute tokens per minute
            Bandwidth limit = Bandwidth.classic(requestsPerMinute, 
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }
    
    /**
     * Send rate limit error response
     * @param response HTTP response
     * @param limit Rate limit that was exceeded
     * @param limitType Type of limit (IP or tenant)
     */
    private void sendRateLimitError(HttpServletResponse response, int limit, String limitType) 
            throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60"); // Retry after 1 minute
        response.getWriter().write(String.format(
            "{\"error\": \"Rate limit exceeded\", \"limit\": %d, \"limit_type\": \"%s\", \"retry_after\": 60}",
            limit, limitType
        ));
    }
    

    
    /**
     * Extract client IP address, considering proxy headers
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return ip.split(",")[0].trim();
        }
        
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        return request.getRemoteAddr();
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RateLimitingFilter initialized - enabled: {}, IP limit: {}/min, tenant limit: {}/min",
                   rateLimitingEnabled, ipRpm, tenantRpm);
        logger.info("Auth endpoints (/api/auth/**) excluded from rate limiting");
    }
    
    @Override
    public void destroy() {
        buckets.clear();
        logger.info("RateLimitingFilter destroyed");
    }
}
