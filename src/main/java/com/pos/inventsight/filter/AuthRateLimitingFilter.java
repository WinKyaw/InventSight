package com.pos.inventsight.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Separate rate limiting filter specifically for authentication endpoints.
 * 
 * Configuration:
 * - Login: 10 attempts per 5 minutes per IP (prevents brute force)
 * - Register: 5 attempts per 10 minutes per IP (prevents spam)
 * - Only applies to /api/auth/login and /api/auth/register
 * 
 * Order: Executes at priority 1 (before main RateLimitingFilter at HIGHEST_PRECEDENCE)
 */
@Component
@Order(1)
public class AuthRateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthRateLimitingFilter.class);
    
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    
    @Value("${inventsight.rate-limiting.auth.enabled:true}")
    private boolean authRateLimitingEnabled;
    
    @Value("${inventsight.rate-limiting.auth.login.max-attempts:10}")
    private int maxLoginAttempts;
    
    @Value("${inventsight.rate-limiting.auth.login.window-minutes:5}")
    private int loginWindowMinutes;
    
    @Value("${inventsight.rate-limiting.auth.register.max-attempts:5}")
    private int maxRegisterAttempts;
    
    @Value("${inventsight.rate-limiting.auth.register.window-minutes:10}")
    private int registerWindowMinutes;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!authRateLimitingEnabled) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestUri = httpRequest.getRequestURI();
        String clientIp = getClientIp(httpRequest);
        
        // Check login endpoint
        if (requestUri.endsWith("/auth/login") || requestUri.endsWith("/api/auth/login")) {
            if (!checkRateLimit(clientIp, loginBuckets, maxLoginAttempts, loginWindowMinutes)) {
                logger.warn("⚠️ Auth rate limit exceeded for IP: {} on login", clientIp);
                sendRateLimitError(httpResponse, "login", maxLoginAttempts, loginWindowMinutes);
                return;
            }
        }
        
        // Check register endpoint
        if (requestUri.endsWith("/auth/register") || 
            requestUri.endsWith("/api/auth/register") || 
            requestUri.endsWith("/auth/signup") ||
            requestUri.endsWith("/api/auth/signup")) {
            if (!checkRateLimit(clientIp, registerBuckets, maxRegisterAttempts, registerWindowMinutes)) {
                logger.warn("⚠️ Auth rate limit exceeded for IP: {} on register", clientIp);
                sendRateLimitError(httpResponse, "register", maxRegisterAttempts, registerWindowMinutes);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * Check rate limit for the given IP and bucket map
     * 
     * @param clientIp Client IP address
     * @param buckets Bucket map (login or register)
     * @param maxAttempts Maximum attempts allowed
     * @param windowMinutes Time window in minutes
     * @return true if within rate limit, false otherwise
     */
    private boolean checkRateLimit(String clientIp, Map<String, Bucket> buckets, 
                                     int maxAttempts, int windowMinutes) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> {
            Bandwidth limit = Bandwidth.classic(maxAttempts, 
                Refill.intervally(maxAttempts, Duration.ofMinutes(windowMinutes)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
        
        return bucket.tryConsume(1);
    }
    
    /**
     * Send rate limit error response
     * 
     * @param response HTTP response
     * @param endpoint Endpoint type (login or register)
     * @param limit Rate limit that was exceeded
     * @param windowMinutes Time window in minutes
     */
    private void sendRateLimitError(HttpServletResponse response, String endpoint, 
                                     int limit, int windowMinutes) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(windowMinutes * 60));
        
        String jsonResponse = String.format(
            "{\"error\":\"Too many %s attempts\",\"limit\":%d,\"retry_after\":%d}",
            endpoint, limit, windowMinutes * 60
        );
        
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * Extract client IP address, considering proxy headers
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
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
        logger.info("AuthRateLimitingFilter initialized - enabled: {}", authRateLimitingEnabled);
        logger.info("Login limit: {} attempts per {} minutes", maxLoginAttempts, loginWindowMinutes);
        logger.info("Register limit: {} attempts per {} minutes", maxRegisterAttempts, registerWindowMinutes);
    }
    
    @Override
    public void destroy() {
        loginBuckets.clear();
        registerBuckets.clear();
        logger.info("AuthRateLimitingFilter destroyed");
    }
}
